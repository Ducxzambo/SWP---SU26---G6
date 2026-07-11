package com.petclinic.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 1 dòng bảng Payments — đại diện cho 1 GIAO DỊCH thanh toán độc lập.
 *
 * KHÔNG có field invoiceID: một Payment KHÔNG thuộc về 1 invoice cụ thể nào
 * (N-N với Invoice qua bảng join {@link InvoicePayment}/PaymentInvoices).
 * "amount" ở đây là TỔNG số tiền của giao dịch thanh toán này — số tiền cụ
 * thể được phân bổ (allocate) cho MỘT invoice nào đó nằm ở
 * {@link InvoicePayment#getAllocatedAmount()}, không phải ở đây.
 */
public class Payment {
    private int             paymentID;
    private BigDecimal      amount;
    private String          method;
    private LocalDateTime   paidAt;
    private Integer         processedByStaffID;
    private String          processedByName; // joined display field (Staff.FullName)

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    public Payment() {}

    public int           getPaymentID()               { return paymentID; }
    public void          setPaymentID(int v)           { paymentID = v; }
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
