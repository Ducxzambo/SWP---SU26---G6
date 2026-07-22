package com.petclinic.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 1 dong bang [Refunds].
 *
 * Bang nay phuc vu 2 giai doan trong vong doi 1 khoan hoan tien:
 *
 *  1) YEU CAU (Status = "Requested") - dong duoc tao NGAY khi khach hang
 *     tick "Yeu cau hoan tien" luc huy 1 appointment dang Confirmed (xem
 *     AppointmentServlet.handleCancel()). O buoc nay CHUA co nhan vien nao
 *     xu ly - processedByID/refundedAt con NULL.
 *
 *  2) DA XU LY (Status = "Processed"/"Rejected") - staff cap nhat sau khi
 *     thuc su chuyen khoan hoan tien (hoac tu choi yeu cau). Man hinh/servlet
 *     staff-side cho buoc nay CHUA duoc xay dung trong pham vi tinh nang
 *     hien tai - class nay chi expose san cac field can thiet de lam sau.
 *
 * KHONG luu PaymentID: appointmentID la du de tra ra Invoice tuong ung
 * (Invoices.AppointmentID) roi tra tiep cac Payment co InvoiceID do (1-N
 * truc tiep qua Payments.InvoiceID) bat cu luc nao can doi chieu lai.
 * totalAmount/paidAmount la SNAPSHOT tai thoi diem tao yeu cau
 * (Invoice.TotalAmount va tong Payments.Amount cua invoice do) - khong doc
 * lai truc tiep tu Invoice moi lan hien thi, vi invoice co the bi thay doi
 * sau do (vd phu phi qua gio o noi tru).
 */
public class Refund {
    private int            refundID;
    private int             appointmentID;
    private BigDecimal      totalAmount;   // snapshot Invoice.TotalAmount luc yeu cau
    private BigDecimal      paidAmount;    // snapshot tong Payments.Amount (InvoiceID tuong ung) luc yeu cau
    private String          reason;
    private String          bankCode;
    private String          accountNumber;
    private String          accountName;
    private String          status;        // Requested | Processed | Rejected
    private LocalDateTime   requestedAt;
    private Integer         processedByID; // null cho toi khi staff xu ly
    private LocalDateTime   refundedAt;    // null cho toi khi staff xu ly

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

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

    public String getFormattedRequestedAt() {
        return requestedAt != null ? requestedAt.format(FMT) : "";
    }

    public boolean isProcessed() { return "Processed".equals(status); }
    public boolean isRequested() { return "Requested".equals(status); }
}

