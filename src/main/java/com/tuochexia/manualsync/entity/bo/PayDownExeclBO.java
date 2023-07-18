package com.tuochexia.manualsync.entity.bo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author linx
 * @version 1.0.0
 * @date 2023-07-13 15:04:10
 * @describe BO
 */
@Setter
@Getter
@Builder
@ToString
public class PayDownExeclBO {
    @ExcelProperty(value = "车牌号码", index = 0)
    private String license;
    @ExcelProperty(value = "总金额", index = 1)
    private long total;
    @ExcelProperty(value = "应付联盟", index = 2)
    private long downPaymentPayable;
    @ExcelProperty(value = "已付", index = 3)
    private long downPaymentPaid;
    @ExcelProperty(value = "报案号", index = 4)
    private String outNo;
}
