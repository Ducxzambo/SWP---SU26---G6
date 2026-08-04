package com.petclinic.backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Payment {
    private int             paymentID;
    private int              invoiceID;
    private BigDecimal      amount;
    private String          method;
    private LocalDateTime   paidAt;
    private Integer         processedByStaffID;
    private String          processedByName;
    private String          paymentCode;

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
    public String getPaymentCode()        { return paymentCode; }
    public void   setPaymentCode(String v){ paymentCode = v; }

    public String getFormattedPaidAt() {
        return paidAt != null ? paidAt.format(FMT) : "";
    }
}
