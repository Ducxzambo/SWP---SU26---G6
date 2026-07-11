package com.petclinic.model;

import java.math.BigDecimal;

/**
 * 1 dòng bảng join N-N PaymentInvoices — liên kết 1 {@link Payment} với 1
 * {@link Invoice} kèm số tiền của giao dịch thanh toán đó được PHÂN BỔ
 * (allocate) cho invoice này cụ thể.
 *
 * Về lý thuyết 1 Payment có thể được phân bổ cho nhiều Invoice khác nhau,
 * và 1 Invoice có thể được thanh toán bởi nhiều Payment khác nhau — đây
 * chính là chỗ biểu diễn quan hệ N-N đó, tách biệt hẳn khỏi cả Payment lẫn
 * Invoice (không nhúng lồng bên trong class nào).
 *
 * "payment" được nạp kèm (join) để tiện hiển thị (phương thức, thời gian,
 * nhân viên xử lý...) mà không cần query riêng — nhưng "allocatedAmount"
 * mới là số tiền ĐÚNG cho invoice này, KHÔNG phải payment.getAmount()
 * (số đó là tổng của cả giao dịch, có thể lớn hơn nếu 1 payment trải trên
 * nhiều invoice).
 */
public class InvoicePayment {
    private int        paymentInvoiceID;
    private int        paymentID;
    private int        invoiceID;
    private BigDecimal allocatedAmount;
    private Payment    payment;

    public InvoicePayment() {}

    public int        getPaymentInvoiceID()        { return paymentInvoiceID; }
    public void       setPaymentInvoiceID(int v)   { paymentInvoiceID = v; }
    public int        getPaymentID()               { return paymentID; }
    public void       setPaymentID(int v)          { paymentID = v; }
    public int        getInvoiceID()               { return invoiceID; }
    public void       setInvoiceID(int v)          { invoiceID = v; }
    public BigDecimal getAllocatedAmount()         { return allocatedAmount; }
    public void       setAllocatedAmount(BigDecimal v) { allocatedAmount = v; }
    public Payment    getPayment()                 { return payment; }
    public void       setPayment(Payment v)        { payment = v; }
}
