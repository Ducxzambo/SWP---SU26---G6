package com.petclinic.backend.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Refund {
    private int            refundID;
    private int             appointmentID;
    private BigDecimal      totalAmount;
    private BigDecimal      paidAmount;
    private String          reason;
    private String          bankCode;
    private String          accountNumber;
    private String          accountName;
    private String          status;        // Requested | Processed | Rejected
    private LocalDateTime   requestedAt;
    private Integer         processedByID;
    private LocalDateTime   refundedAt;
    private String          rejectReason;

    private String          customerName;
    private String          customerPhone;
    private String          petName;
    private LocalDate appointmentDate;
    private String          appointmentStatus;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public Refund() {}

    public int           getRefundID()                { return refundID; }
    public void          setRefundID(int v)            { refundID = v; }
    public int           getAppointmentID()            { return appointmentID; }
    public void          setAppointmentID(int v)       { appointmentID = v; }
    public BigDecimal    getTotalAmount()               { return totalAmount; }
    public void          setTotalAmount(BigDecimal v)   { totalAmount = v; }
    public BigDecimal    getPaidAmount()                 { return paidAmount; }
    public void          setPaidAmount(BigDecimal v)     { paidAmount = v; }
    public String        getReason()                     { return reason; }
    public void          setReason(String v)             { reason = v; }
    public String        getBankCode()                    { return bankCode; }
    public void          setBankCode(String v)             { bankCode = v; }
    public String        getAccountNumber()                 { return accountNumber; }
    public void          setAccountNumber(String v)          { accountNumber = v; }
    public String        getAccountName()                     { return accountName; }
    public void          setAccountName(String v)              { accountName = v; }
    public String        getStatus()                            { return status; }
    public void          setStatus(String v)                    { status = v; }
    public LocalDateTime getRequestedAt()                        { return requestedAt; }
    public void          setRequestedAt(LocalDateTime v)         { requestedAt = v; }
    public Integer       getProcessedByID()                      { return processedByID; }
    public void          setProcessedByID(Integer v)              { processedByID = v; }
    public LocalDateTime getRefundedAt()                          { return refundedAt; }
    public void          setRefundedAt(LocalDateTime v)           { refundedAt = v; }
    public String        getRejectReason()                        { return rejectReason; }
    public void          setRejectReason(String v)                { rejectReason = v; }

    public String        getCustomerName()             { return customerName; }
    public void          setCustomerName(String v)     { customerName = v; }
    public String        getCustomerPhone()            { return customerPhone; }
    public void          setCustomerPhone(String v)    { customerPhone = v; }
    public String        getPetName()                  { return petName; }
    public void          setPetName(String v)          { petName = v; }
    public LocalDate      getAppointmentDate()          { return appointmentDate; }
    public void           setAppointmentDate(LocalDate v) { appointmentDate = v; }
    public String         getAppointmentStatus()         { return appointmentStatus; }
    public void            setAppointmentStatus(String v) { appointmentStatus = v; }

    public String getFormattedRequestedAt() {
        return requestedAt != null ? requestedAt.format(FMT) : "";
    }

    public String getFormattedRefundedAt() {
        return refundedAt != null ? refundedAt.format(FMT) : "";
    }

    public String getFormattedAppointmentDate() {
        return appointmentDate != null ? appointmentDate.format(DATE_FMT) : "";
    }

    public boolean isProcessed() { return "Processed".equals(status); }
    public boolean isRequested() { return "Requested".equals(status); }
    public boolean isRejected()  { return "Rejected".equals(status); }
}