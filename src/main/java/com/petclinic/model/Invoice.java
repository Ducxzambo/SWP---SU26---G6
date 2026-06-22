package com.petclinic.model;

public class Invoice {
    private int    invoiceID;
    private int    appointmentID;
    private int    customerID;
    private double totalAmount;
    private String status; // Unpaid | Paid | Refunded | PartiallyRefunded

    public Invoice() {}

    public int    getInvoiceID()             { return invoiceID; }
    public void   setInvoiceID(int v)        { invoiceID = v; }
    public int    getAppointmentID()         { return appointmentID; }
    public void   setAppointmentID(int v)    { appointmentID = v; }
    public int    getCustomerID()            { return customerID; }
    public void   setCustomerID(int v)       { customerID = v; }
    public double getTotalAmount()           { return totalAmount; }
    public void   setTotalAmount(double v)   { totalAmount = v; }
    public String getStatus()               { return status; }
    public void   setStatus(String v)       { status = v; }
}
