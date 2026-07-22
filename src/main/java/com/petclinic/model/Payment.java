package com.petclinic.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 1 dòng bảng Payments — đại diện cho 1 GIAO DỊCH thanh toán, thuộc về
 * ĐÚNG 1 Invoice (1-N: 1 Invoice có thể có nhiều Payment, mỗi Payment chỉ
 * thuộc 1 Invoice — xem cột Payments.InvoiceID, FK_Payments_Invoices).
 *
 * "amount" là số tiền của giao dịch thanh toán này, áp dụng trọn vẹn cho
 * invoiceID — không còn khái niệm "phân bổ" (allocate) một phần cho nhiều
 * invoice khác nhau như mô hình N-N cũ (đã bỏ bảng PaymentInvoices).
 */
public class Payment {
    private int             paymentID;
    private int              invoiceID;
    private BigDecimal      amount;
    private String          method;
    private LocalDateTime   paidAt;
    private Integer         processedByStaffID;
    private String          processedByName; // joined display field (Staff.FullName)

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    public Payment() {}

    public int           getPaymentID()               { return paymentID; }
    public void          setPaymentID(int v)           { paymentID = v; }
    public int           getInvoiceID()                { return invoiceID; }
    public void          setInvoiceID(int v)            { invoiceID = v; }
    public BigDecimal    getAmount()                   { return amount; }
    public void          setAmount(BigDecimal v)       { amount = v; }
    public String        getMethod()                   { return method; }
    public void          setMethod(String v)           { method = v; }
    public LocalDateTime getPaidAt()                    { return paidAt; }
    public void          setPaidAt(LocalDateTime v)     { paidAt = v; }
    public Integer       getProcessedByStaffID()         { return processedByStaffID; }
    public void          setProcessedByStaffID(Integer v){ processedByStaffID = v; }
    public String        getProcessedByName()             { return processedByName; }
    public void          setProcessedByName(String v)      { processedByName = v; }

    public String getFormattedPaidAt() {
        return paidAt != null ? paidAt.format(FMT) : "";
    }
}
