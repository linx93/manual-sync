package com.tuochexia.manualsync.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.alibaba.excel.util.ListUtils;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.tuochexia.manualsync.entity.bo.PayDownExeclBO;
import com.tuochexia.manualsync.entity.execl.PayDownExecl;
import com.tuochexia.manualsync.entity.po.FinanceReconciliationPO;
import com.tuochexia.manualsync.mapper.FinanceReconciliationMapper;
import com.tuochexia.manualsync.service.IPayDown;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @author linx
 * @version 1.0.0
 * @date 2023-07-12 10:48:22
 * @describe 通过execl模板把向下付款的应收、应付同步到系统中
 */
@Service
public class PayDownImpl implements IPayDown {
    private static final Logger log = LoggerFactory.getLogger(PayDownImpl.class);

    @Value("${sync.data-source.pay-down}")
    private String path;

    private final FinanceReconciliationMapper fiMapper;
    private final SqlSessionFactory sqlSessionFactory;

    public PayDownImpl(FinanceReconciliationMapper fiMapper, SqlSessionFactory sqlSessionFactory) {
        this.fiMapper = fiMapper;
        this.sqlSessionFactory = sqlSessionFactory;
    }


    private static final Set<PayDownExeclBO> aLicenseHasManyService = new HashSet<>(32);
    private static final Set<PayDownExeclBO> errorData = new HashSet<>(32);
    private static final Set<PayDownExeclBO> notExist = new HashSet<>(32);
    private static final Set<PayDownExeclBO> rollbackData = new HashSet<>(32);
    private static final Set<PayDownExeclBO> exceptionExeclData = new HashSet<>(32);

    //元到分
    private static final long cent = 100;
    //总条数
    private static long rows = 0;

    @Override
    public long syncPayDown() {
        ExcelReader excelReader = EasyExcel.read(path.trim()).build();
        //sheet1是ERP流水不处理
        //ReadSheet readSheet1 =  EasyExcel.readSheet(0).head(PayDownExecl.class).registerReadListener(new PayDownListener()).build();
        ReadSheet readSheet2 = EasyExcel.readSheet(1).head(PayDownExecl.class).registerReadListener(new PayDownListener()).build();
        ReadSheet readSheet3 = EasyExcel.readSheet(2).head(PayDownExecl.class).registerReadListener(new PayDownListener()).build();
        ReadSheet readSheet4 = EasyExcel.readSheet(3).head(PayDownExecl.class).registerReadListener(new PayDownListener()).build();
        ReadSheet readSheet5 = EasyExcel.readSheet(4).head(PayDownExecl.class).registerReadListener(new PayDownListener()).build();
        ReadSheet readSheet6 = EasyExcel.readSheet(5).head(PayDownExecl.class).registerReadListener(new PayDownListener()).build();
        ReadSheet readSheet7 = EasyExcel.readSheet(6).head(PayDownExecl.class).registerReadListener(new PayDownListener()).build();
        ReadSheet readSheet8 = EasyExcel.readSheet(7).head(PayDownExecl.class).registerReadListener(new PayDownListener()).build();
        // 这里注意 一定要把sheet1 sheet2 .... 一起传进去，不然有个问题就是03版的excel 会读取多次，浪费性能
        excelReader.read(readSheet2, readSheet3, readSheet4, readSheet5, readSheet6, readSheet7, readSheet8);


        log.info("[有车牌号且无报案号存在多个救援服务的情况，需要手动处理==>{}]", aLicenseHasManyService);
        log.info("[有车牌号且有报案号存在多个救援服务的情况，需要手动处理==>{}]", errorData);
        log.info("[车牌号不存在于财务报表中的情况，需要手动处理==>{}]", notExist);
        log.info("[execl中异常的数据，需要手动处理==>{}]", exceptionExeclData);
        log.info("[报错回滚对应的数据需要手动处理，需要手动处理==>{}]", rollbackData);

        // 把这数据写到execl中存起来
        writeExecl();
        return 0;
    }

    private void batchProcess(List<PayDownExecl> dataList) {
        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH, false);
        PayDownExeclBO payDownExeclBO = PayDownExeclBO.builder().build();
        try {
            FinanceReconciliationMapper mapper = sqlSession.getMapper(FinanceReconciliationMapper.class);
            for (PayDownExecl item : dataList) {
                //前提需知: 首先明确一点，车牌号是一定有的，报案号不一定有
                //处理逻辑：
                //1. 用车牌号+非空的报案号匹配数据，如果返回一条数据直接处理，返回多条就是异常数据many，返回0条也算异常数据zero
                //2. 剩下的有车牌号同时报案号为空的情况，用车牌号匹配数据，返回一条数据直接处理，返回多条就是异常数据many，返回0条也算异常数据zero

                payDownExeclBO = buildBO(item);

                FinanceReconciliationPO po = new FinanceReconciliationPO();
                po.setLicense(payDownExeclBO.getLicense());
                po.setOutNo(payDownExeclBO.getOutNo());
                List<FinanceReconciliationPO> pos = mapper.selectByEntity(po);
                if (pos.size() == 1) {
                    //直接处理
                    FinanceReconciliationPO po1 = pos.get(0);
                    process(mapper, payDownExeclBO, po1);
                } else if (pos.size() > 1) {
                    //说明存在一个车牌有两个及以上救援服务的情况
                    if (payDownExeclBO.getOutNo() == null || payDownExeclBO.getOutNo().trim().equals("")) {
                        log.info("车牌号[{}]且报案号为空有多个救援服务的情况，需要手动处理", payDownExeclBO.getLicense());
                        //这里暂时放到list中，严格来说这个应该存库
                        aLicenseHasManyService.add(payDownExeclBO);
                    } else {
                        log.info("车牌号[{}]且报案号[{}]有多个救援服务的情况，需要手动处理[一定是异常数据了]", payDownExeclBO.getLicense(), payDownExeclBO.getOutNo());
                        //这里暂时放到list中，严格来说这个应该存库
                        errorData.add(payDownExeclBO);
                    }
                } else {
                    //车牌号不存在于财务报表中
                    log.info("车牌号[{}]不存在于财务报表中，需要手动处理", payDownExeclBO.getLicense());
                    notExist.add(payDownExeclBO);
                }
            }
            sqlSession.commit();
        } catch (Exception e) {
            //回滚
            rollbackData.add(payDownExeclBO);
            log.error("报错回滚:{}", e.getMessage());
            log.error("报错回滚对应的数据需要手动处理==>{}", payDownExeclBO);
            sqlSession.rollback();
            e.printStackTrace();
        } finally {
            sqlSession.close();
        }
    }

    private class PayDownListener implements ReadListener<PayDownExecl> {

        /**
         * 所有数据解析完成了 都会来调用
         */
        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            // 这里也要保存数据，确保最后遗留的数据也存储到数据库
            save();
        }

        /**
         * 每隔5条存储数据库，实际使用中可以100条，然后清理list ，方便内存回收
         */
        private static final int BATCH_COUNT = 100;

        /**
         * 缓存的数据
         */
        private List<PayDownExecl> cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);

        /**
         * 这个每一条数据解析都会来调用
         *
         * @param data one row value. Is is same as {@link AnalysisContext#readRowHolder()}
         */
        @Override
        public void invoke(PayDownExecl data, AnalysisContext context) {
            rows += 1;
            // log.info("解析到一条数据:{}", JSON.toJSONString(data));
//            Map<Integer, Cell> cellMap = context.readRowHolder().getCellMap();
//            log.info("解析到一条数据cellMap:{}", JSON.toJSONString(cellMap));
//            cellMap.forEach((k,v) -> {
//                //先做类型转换
//                ReadCellData<PayDownExecl> cellData = (ReadCellData) v;
//                //再提取单元格属性
//                log.info("列索引[{}]的,单元格类型[{}],公式[{}]",k,cellData.getType(),JSON.toJSONString(cellData.getFormulaData()));
//            });
            cachedDataList.add(data);
            // 达到阈值BATCH_COUNT了，需要去操作一次数据库，防止数据几万条数据在内存，容易OOM
            if (cachedDataList.size() >= BATCH_COUNT) {
                save();
                // 存储完成清理 list
                cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
            }
        }


        /**
         * 加上存储数据库
         */
        private void save() {
            log.info("{}条数据，开始存储数据库！", cachedDataList.size());
            batchProcess(cachedDataList);
            log.info("{}条数据，存储数据库成功！", cachedDataList.size());
        }
    }

    private void process(FinanceReconciliationMapper mapper, PayDownExeclBO bo, FinanceReconciliationPO po1) {


        //开始处理
        long payable = bo.getDownPaymentPayable();
        long paid = bo.getDownPaymentPaid();

        if (payable > 0 && paid == 0) {
            //已付为0，应付大于0，只需要把应付更新到finance_reconciliation表中的down_payment_payable字段
            po1.setDownPaymentPayable(payable);
            mapper.updateById(po1);
        } else if (payable > 0 && paid > 0) {
            //已付和应付都大于0，需要两种处理
            //1. 把应付更新到finance_reconciliation表中的down_payment_payable和down_payment_paid字段
            po1.setDownPaymentPayable(payable);
            po1.setDownPaymentPaid(paid);
            mapper.updateById(po1);
            //2. 添加向下收款表finance_down_payment_record的记录
            mapper.insertFinanceDownPaymentRecord(po1);
        } else {
            exceptionExeclData.add(bo);
            log.error("异常的execl中数据[车牌号={}][应付={}][已付={}]", bo.getLicense(), bo.getDownPaymentPayable(), bo.getDownPaymentPaid());
        }
    }


    private PayDownExeclBO buildBO(PayDownExecl item) {
        String license = item.getLicense() == null || item.getLicense().getData() == null ? "" : item.getLicense().getData().trim();
        long total = item.getTotal() == null || item.getTotal().getData() == null ? 0 : (long) (item.getTotal().getData() * cent);
        long payable = item.getDownPaymentPayable() == null || item.getDownPaymentPayable().getData() == null ? 0 : (long) (item.getDownPaymentPayable().getData() * cent);
        long paid = item.getDownPaymentPaid() == null || item.getDownPaymentPaid().getData() == null ? 0 : (long) (item.getDownPaymentPaid().getData() * cent);
        String outNo = item.getOutNo() == null || item.getOutNo().getData() == null ? "" : item.getOutNo().getData().trim();
        PayDownExeclBO build = PayDownExeclBO.builder().downPaymentPayable(payable).downPaymentPaid(paid).total(total).outNo(outNo).license(license).build();
        return build;
    }


    private void writeExecl() {
        // 方法2: 如果写到不同的sheet 同一个对象
        String path = getPath(this.path.trim());
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        //            log.info("[有车牌号且无报案号存在多个救援服务的情况，需要手动处理==>{}]", aLicenseHasManyService);
        //            log.info("[有车牌号且有报案号存在多个救援服务的情况，需要手动处理==>{}]", errorData);
        //            log.info("[车牌号不存在于财务报表中的情况，需要手动处理==>{}]", notExist);
        //            log.info("[execl中异常的数据，需要手动处理==>{}]", exceptionExeclData);
        //            log.info("[报错回滚对应的数据需要手动处理，需要手动处理==>{}]", rollbackData);
        // 这里 指定文件
        try (ExcelWriter excelWriter = EasyExcel.write(path + File.separator + "需要手动处理的数据" + dateStr + ".xlsx", PayDownExeclBO.class).build()) {

            // 每次都要创建writeSheet 这里注意必须指定sheetNo 而且sheetName必须不一样

            //有车牌号且无报案号存在多个救援服务的情况，需要手动处理
            WriteSheet writeSheet0 = EasyExcel.writerSheet(0, "有车牌号且无报案号存在多个救援服务").build();
            excelWriter.write(aLicenseHasManyService, writeSheet0);

            //有车牌号且有报案号存在多个救援服务的情况，需要手动处理
            WriteSheet writeSheet1 = EasyExcel.writerSheet(1, "有车牌号且有报案号存在多个救援服务").build();
            excelWriter.write(errorData, writeSheet1);

            //车牌号不存在于财务报表中的情况，需要手动处理
            WriteSheet writeSheet2 = EasyExcel.writerSheet(2, "车牌号不存在于财务报表中的情况").build();
            excelWriter.write(notExist, writeSheet2);

            //execl中异常的数据，需要手动处理
            WriteSheet writeSheet3 = EasyExcel.writerSheet(3, "execl中异常的数据").build();
            excelWriter.write(exceptionExeclData, writeSheet3);

            //报错回滚对应的数据需要手动处理，需要手动处理
            WriteSheet writeSheet4 = EasyExcel.writerSheet(4, "报错回滚对应的数据需要手动处理").build();
            excelWriter.write(rollbackData, writeSheet4);

        } catch (Exception e) {
            log.error("写入execl出错:{}", e.getMessage());
            e.printStackTrace();
        }
    }


    public String getPath(String path) {
        return path.substring(0, path.lastIndexOf(File.separator));
    }
}
