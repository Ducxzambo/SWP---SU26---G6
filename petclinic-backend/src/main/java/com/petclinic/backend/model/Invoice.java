package com.petclinic.backend.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Invoice {
    private int              invoiceID;
    private String           invoiceCode;
    private int               appointmentID;
    private int               customerID;
    private BigDecimal        totalAmount;
    private String            otherFees;
    private String            status;
    private List<InvoiceItem> items    = new ArrayList<>();
    private List<Payment>     payments = new ArrayList<>();

    public Invoice() {}

    public int        getInvoiceID()          { return invoiceID; }
    public void       setInvoiceID(int v)     { invoiceID = v; }
    public String getInvoiceCode()              { return invoiceCode; }
    public void   setInvoiceCode(String v)      { invoiceCode = v; }
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
    public List<Payment> getPayments()                 { return payments; }
    public void          setPayments(List<Payment> v) {
        this.payments = v != null ? v : new ArrayList<>();
    }
}
