package com.petclinic.backend.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 1 dong bang [Refunds].
 *
 * Bang nay phuc vu 2 giai doan trong vong doi 1 khoan hoan tien:
 *
 *  1) YEU CAU (Status = "Requested") - dong duoc tao khi:
 *     a) khach hang tick "Yeu cau hoan tien" luc huy 1 appointment dang
 *        Confirmed (xem AppointmentServlet.handleCancel()), HOAC
 *     b) staff tu tao yeu cau cho 1 appointment da Cancelled/NoShow/Done tu
 *        truoc (xem RefundCreateServlet) - vd khach lien he rieng ngoai
 *        luong huy lich thong thuong.
 *     O buoc nay CHUA co nhan vien nao xu ly - processedByID/refundedAt con
 *     NULL.
 *
 *  2) DA XU LY (Status = "Processed"/"Rejected") - staff cap nhat sau khi
 *     thuc su chuyen khoan hoan tien (hoac tu choi yeu cau). Xem
 *     RefundProcessServlet / RefundRejectServlet.
 *
 * KHONG luu PaymentID: appointmentID la du de tra ra Invoice tuong ung
 * (Invoices.AppointmentID) roi tra tiep cac Payment co InvoiceID do (1-N
 * truc tiep qua Payments.InvoiceID) bat cu luc nao can doi chieu lai.
 * totalAmount la SNAPSHOT Invoice.TotalAmount luc tao yeu cau. paidAmount la
 * SO TIEN SE DUOC HOAN - mac dinh bang tong Payments.Amount da thu (hoan
 * 100%), nhung co the la 1 so nho hon neu staff tu tao yeu cau hoan mot
 * phan (xem RefundService.createManualRequest). Ca 2 deu KHONG doc lai truc
 * tiep tu Invoice/Payments moi lan hien thi, vi invoice co the bi thay doi
 * sau do (vd phu phi qua gio o noi tru).
 */
public class Refund {
    private int            refundID;
    private int             appointmentID;
    private BigDecimal      totalAmount;   // snapshot Invoice.TotalAmount luc yeu cau
    private BigDecimal      paidAmount;    // so tien SE DUOC HOAN (xem javadoc class)
    private String          reason;        // ly do cua KHACH khi yeu cau
    private String          bankCode;
    private String          accountNumber;
    private String          accountName;
    private String          status;        // Requested | Processed | Rejected
    private LocalDateTime   requestedAt;
    private Integer         processedByID; // null cho toi khi staff xu ly
    private LocalDateTime   refundedAt;    // null cho toi khi staff XAC NHAN DA CHUYEN KHOAN
    private String          rejectReason;  // ly do cua STAFF khi tu choi - null neu chua tu choi

    // Cac truong HIEN THI tong hop, duoc RefundDAO gan tu JOIN Appointments/
    // Customers/Pets - khong luu trong bang Refunds, chi phuc vu man hinh
    // danh sach/chi tiet (cung quy uoc voi Appointment.petName/serviceName).
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