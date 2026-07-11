package com.petclinic.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 1 dòng bảng Invoices — hoá đơn ứng với 1 Appointment (1-1, ràng buộc bởi
 * UNIQUE constraint trên AppointmentID).
 *
 * Invoice KHÔNG còn chứa các inner class InvoiceItem/Payment lồng bên trong
 * — đã tách thành các model top-level độc lập:
 *   - {@link InvoiceItem}    — N-1 thật (nhiều item thuộc đúng 1 invoice)
 *   - {@link InvoicePayment} — bảng join N-N thật với {@link Payment}
 *     (1 invoice có thể được trả bởi nhiều payment, 1 payment về lý thuyết
 *     có thể phân bổ cho nhiều invoice) — xem PaymentInvoices trong schema.
 * Việc lồng Payment bên trong Invoice trước đây ngầm giả định N-1
 * (nhiều payment → đúng 1 invoice), SAI với thiết kế N-N thực tế của schema.
 */
public class Invoice {
    private int              invoiceID;
    private int               appointmentID;
    private int               customerID;
    private BigDecimal        totalAmount;
    private String            otherFees;
    private String            status;
    private List<InvoiceItem>    items    = new ArrayList<>();
    private List<InvoicePayment> payments = new ArrayList<>();

    public Invoice() {}

    public int        getInvoiceID()          { return invoiceID; }
    public void       setInvoiceID(int v)     { invoiceID = v; }
    public int        getAppointmentID()      { return appointmentID; }
    public void       setAppointmentID(int v) { appointmentID = v; }
    public int        getCustomerID()         { return customerID; }
    public void       setCustomerID(int v)    { customerID = v; }
    public BigDecimal getTotalAmount()        { return totalAmount; }
    public void       setTotalAmount(BigDecimal v) { totalAmount = v; }
    public String      getOtherFees()          { return otherFees; }
    public void        setOtherFees(String v)  { otherFees = v; }
    public String      getStatus()             { return status; }
    public void        setStatus(String v)     { status = v; }

    public List<InvoiceItem> getItems()                 { return items; }
    public void              setItems(List<InvoiceItem> v) {
        this.items = v != null ? v : new ArrayList<>();
    }
    public List<InvoicePayment> getPayments()                    { return payments; }
    public void                 setPayments(List<InvoicePayment> v) {
        this.payments = v != null ? v : new ArrayList<>();
    }

    /** Tổng đã thu cho invoice này = SUM(InvoicePayment.allocatedAmount). Tiện dùng ở JSP. */
    public BigDecimal getAmountPaid() {
        BigDecimal sum = BigDecimal.ZERO;
        for (InvoicePayment p : payments) {
            if (p.getAllocatedAmount() != null) sum = sum.add(p.getAllocatedAmount());
        }
        return sum;
    }
}
