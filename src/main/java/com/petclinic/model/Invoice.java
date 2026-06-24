package com.petclinic.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class Invoice {
    private int           invoiceID;
    private int           appointmentID;
    private int           customerID;
    private BigDecimal    totalAmount;
    private String         otherFees; // nvarchar(200) — nhãn mô tả phụ thu (ví dụ "OT Fee"),
    // KHÔNG phải số tiền. Số tiền đã được cộng thẳng vào TotalAmount.
    private String        status; // Unpaid | Paid | Refunded | PartiallyRefunded
    private List<InvoiceItem> items;
    private List<Payment>     payments;

    public Invoice() {}

    public int           getInvoiceID()       { return invoiceID; }
    public void          setInvoiceID(int v)  { invoiceID = v; }
    public int           getAppointmentID()   { return appointmentID; }
    public void          setAppointmentID(int v){ appointmentID = v; }
    public int           getCustomerID()      { return customerID; }
    public void          setCustomerID(int v) { customerID = v; }
    public BigDecimal    getTotalAmount()     { return totalAmount; }
    public void          setTotalAmount(BigDecimal v){ totalAmount = v; }
    public String         getOtherFees()       { return otherFees; }
    public void           setOtherFees(String v){ otherFees = v; }
    public String        getStatus()          { return status; }
    public void          setStatus(String v)  { status = v; }
    public List<InvoiceItem> getItems()       { return items; }
    public void          setItems(List<InvoiceItem> v){ items = v; }
    public List<Payment> getPayments()        { return payments; }
    public void          setPayments(List<Payment> v){ payments = v; }

    // ── Inner: InvoiceItem ────────────────────────────────────────────────
    public static class InvoiceItem {
        private int        invoiceItemID;
        private int        invoiceID;
        private String     itemType;    // Service | Medicine | Vaccine | Other
        private String     description;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;

        public InvoiceItem() {}

        public int        getInvoiceItemID()      { return invoiceItemID; }
        public void       setInvoiceItemID(int v) { invoiceItemID = v; }
        public int        getInvoiceID()          { return invoiceID; }
        public void       setInvoiceID(int v)     { invoiceID = v; }
        public String     getItemType()           { return itemType; }
        public void       setItemType(String v)   { itemType = v; }
        public String     getDescription()        { return description; }
        public void       setDescription(String v){ description = v; }
        public BigDecimal getQuantity()           { return quantity; }
        public void       setQuantity(BigDecimal v){ quantity = v; }
        public BigDecimal getUnitPrice()          { return unitPrice; }
        public void       setUnitPrice(BigDecimal v){ unitPrice = v; }
        public BigDecimal getLineTotal()          { return lineTotal; }
        public void       setLineTotal(BigDecimal v){ lineTotal = v; }
    }

    // ── Inner: Payment ────────────────────────────────────────────────────
    public static class Payment {
        private int           paymentID;
        private int           invoiceID;
        private BigDecimal    amount;
        private String        method;
        private LocalDateTime paidAt;
        private String        processedByName;

        public Payment() {}

        public int           getPaymentID()          { return paymentID; }
        public void          setPaymentID(int v)     { paymentID = v; }
        public int           getInvoiceID()          { return invoiceID; }
        public void          setInvoiceID(int v)     { invoiceID = v; }
        public BigDecimal    getAmount()             { return amount; }
        public void          setAmount(BigDecimal v) { amount = v; }
        public String        getMethod()             { return method; }
        public void          setMethod(String v)     { method = v; }
        public LocalDateTime getPaidAt()             { return paidAt; }
        public void          setPaidAt(LocalDateTime v){ paidAt = v; }

        public String getFormattedPaidAt() {
            if (paidAt == null) return "";
            return paidAt.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
        }

        public String        getProcessedByName()    { return processedByName; }
        public void          setProcessedByName(String v){ processedByName = v; }
    }
}