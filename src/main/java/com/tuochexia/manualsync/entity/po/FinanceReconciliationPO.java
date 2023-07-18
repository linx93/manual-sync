package com.tuochexia.manualsync.entity.po;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author linx
 * @version 1.0.0
 * @date 2023-07-12 11:31:25
 * @describe 财务报表
 */
@Data
public class FinanceReconciliationPO {
    private Long id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Long tenantId;
    private String area;
    private String outNo;
    private String callerCompanyId;
    private String callerCompanyName;
    private Long cashCollection;
    private String city;
    private Long serveCreatedAt;
    private String district;
    private Long driverId;
    private String driverName;
    private String license;
    private Long online;
    private Long outDispatch;
    private Long paid;
    private String province;
    private Long receivable;
    private Long received;
    private Long rescueDate;
    private String serveCompanyId;
    private String serveCompanyName;
    private String sid;
    private Long surveyorId;
    private String surveyorName;
    private Long serveUpdatedAt;
    private Long wxReceivable;
    private Long wxReceived;
    private Long zpPaid;
    private String zpPaidRate;
    private Long zpReceivable;
    private Long zpReceived;
    private Long downPaymentPayable;
    private Long downPaymentPaid;
    private Long needInvoice;
    private Long feedbackStatus;
    private Long financeStatus;
    private Long overdueDays;
}

