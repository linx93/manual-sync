package com.tuochexia.manualsync.entity.execl;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.metadata.data.CellData;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author linx
 * @version 1.0.0
 * @date 2023-07-12 11:08:45
 * @describe 向下付款的execl对象
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class PayDownExecl {

    private static final int Cent = 100;

    private CellData<String> license;
    private CellData<Double> total;
    private CellData<Double> downPaymentPayable;
    private CellData<Double> downPaymentPaid;
    private CellData<String> outNo;


}


