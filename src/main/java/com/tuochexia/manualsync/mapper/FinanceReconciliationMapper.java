package com.tuochexia.manualsync.mapper;

import com.tuochexia.manualsync.entity.execl.PayDownExecl;
import com.tuochexia.manualsync.entity.po.FinanceReconciliationPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author linx
 * @version 1.0.0
 * @date 2023-07-12 11:32:47
 * @describe 财务报表mapper
 */
@Mapper
public interface FinanceReconciliationMapper {
    void syncPayDownData(PayDownExecl data);

    void updateById(@Param("po") FinanceReconciliationPO po);

    FinanceReconciliationPO selectOneById(Long id);

    List<FinanceReconciliationPO> selectByEntity(@Param("po") FinanceReconciliationPO po);

    void insertFinanceDownPaymentRecord(@Param("po")FinanceReconciliationPO po);
}
