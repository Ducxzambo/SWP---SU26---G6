package com.petclinic.backend.service;

import com.petclinic.backend.dao.AppointmentDAO;
import com.petclinic.backend.dao.CustomerDAO;
import com.petclinic.backend.dao.InvoiceDAO;
import com.petclinic.backend.dao.RefundDAO;
import com.petclinic.backend.model.*;
import com.petclinic.backend.util.VietQrUtil;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * Service layer cho tính năng quản lý hoàn tiền (staff-side).
 * <p>
 * "Xác nhận hoàn tiền" chỉ đánh dấu Processed SAU KHI staff đã tự chuyển
 * khoản thủ công (quét VietQR do buildVietQrUrl() sinh ra) - hệ thống
 * không tự động gửi tiền, xem RefundListServlet/RefundDetailServlet.
 */
public class RefundService {

    /** Appointment phải ở 1 trong các trạng thái này mới được tạo yêu cầu hoàn tiền thủ công. */
    private static final List<String> ELIGIBLE_APPOINTMENT_STATUSES = List.of("Cancelled", "NoShow", "Done");

    private final RefundDAO refundDAO = new RefundDAO();
    private final InvoiceDAO invoiceDAO = new InvoiceDAO();
    private final AppointmentDAO appointmentDAO = new AppointmentDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final EmailService emailService = new EmailService();

    // ── List / detail ────────────────────────────────────────────────────────

    public List<Refund> listRefunds(String statusFilter, String sortBy) throws SQLException {
        return refundDAO.search(statusFilter, sortBy);
    }

    public Refund getById(int refundId) throws SQLException {
        return refundDAO.findById(refundId);
    }

    /** Invoice (kèm Items + Payments) của appointment gắn với refund này. */
    public Invoice getInvoiceForRefund(Refund refund) throws SQLException {
        return invoiceDAO.findByAppointment(refund.getAppointmentID());
    }

    /** Cùng logic trên, dùng khi chưa có Refund (màn tạo yêu cầu mới, đang xem preview). */
    public Invoice getInvoiceForAppointment(int appointmentId) throws SQLException {
        return invoiceDAO.findByAppointment(appointmentId);
    }

    /** URL ảnh VietQR để staff quét chuyển khoản - null nếu thiếu thông tin ngân hàng. */
    public String buildVietQrUrl(Refund refund) {
        String message = "Hoan tien lich hen " + refund.getAppointmentID();
        return VietQrUtil.buildQuickLinkUrl(refund.getBankCode(), refund.getAccountNumber(),
                refund.getAccountName(), refund.getPaidAmount(), message);
    }

    // ── Xử lý: Xác nhận / Từ chối ────────────────────────────────────────────

    /**
     * Xác nhận ĐÃ CHUYỂN KHOẢN xong (bước 2, sau khi staff quét QR ở bước 1
     * và tự thực hiện chuyển khoản bằng app ngân hàng của họ). Cập nhật
     * Refund → Processed, Invoice → Refunded/PartiallyRefunded tuỳ số tiền
     * hoàn có phủ hết tổng đã thu hay không, rồi gửi email cho khách.
     */
    public void confirmProcessed(int refundId, int staffId) throws Exception {
        Refund refund = refundDAO.findById(refundId);
        if (refund == null) throw new IllegalArgumentException("Không tìm thấy yêu cầu hoàn tiền.");
        if (!refund.isRequested()) {
            throw new IllegalStateException("Yêu cầu này đã được xử lý trước đó.");
        }

        boolean updated = refundDAO.markProcessed(refundId, staffId);
        if (!updated) {
            throw new IllegalStateException(
                    "Yêu cầu này vừa được xử lý (có thể ở tab khác) — vui lòng tải lại trang.");
        }

        Invoice invoice = invoiceDAO.findByAppointment(refund.getAppointmentID());
        if (invoice != null) {
            BigDecimal totalPaid = sumPayments(invoice);
            boolean coversAll = refund.getPaidAmount() != null
                    && totalPaid.compareTo(refund.getPaidAmount()) <= 0;
            invoiceDAO.updateStatus(invoice.getInvoiceID(), coversAll ? "Refunded" : "PartiallyRefunded");
        }

        refund.setStatus("Processed");
        sendOutcomeEmail(refund, true);
    }

    /** Từ chối yêu cầu - lý do bắt buộc, khác với Reason (lý do gốc của khách). */
    public void reject(int refundId, int staffId, String rejectReason) throws Exception {
        if (rejectReason == null || rejectReason.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập lý do từ chối.");
        }
        Refund refund = refundDAO.findById(refundId);
        if (refund == null) throw new IllegalArgumentException("Không tìm thấy yêu cầu hoàn tiền.");
        if (!refund.isRequested()) {
            throw new IllegalStateException("Yêu cầu này đã được xử lý trước đó.");
        }

        boolean updated = refundDAO.markRejected(refundId, staffId, rejectReason.trim());
        if (!updated) {
            throw new IllegalStateException(
                    "Yêu cầu này vừa được xử lý (có thể ở tab khác) — vui lòng tải lại trang.");
        }

        refund.setStatus("Rejected");
        refund.setRejectReason(rejectReason.trim());
        sendOutcomeEmail(refund, false);
    }

    private void sendOutcomeEmail(Refund refund, boolean processed) throws SQLException {
        Appointment appt = appointmentDAO.findById(refund.getAppointmentID());
        if (appt == null) return;
        Customer customer = customerDAO.findById(appt.getCustomerID());
        if (customer == null) return;
        if (processed) emailService.onRefundProcessed(customer, refund, appt);
        else emailService.onRefundRejected(customer, refund, appt);
    }

    private BigDecimal sumPayments(Invoice invoice) {
        BigDecimal sum = BigDecimal.ZERO;
        if (invoice.getPayments() == null) return sum;
        for (Payment p : invoice.getPayments()) {
            if (p.getAmount() != null) sum = sum.add(p.getAmount());
        }
        return sum;
    }

    // ── Tạo yêu cầu mới (staff tự khởi tạo, không qua luồng huỷ lịch) ────────────

    /**
     * @param statusFilter null/blank = tất cả 3 trạng thái hợp lệ; nếu có giá trị thì PHẢI
     *                     nằm trong ELIGIBLE_APPOINTMENT_STATUSES - giá trị lạ (vd bị chỉnh
     *                     tay trên query string) bị bỏ qua, không được dùng để lách qua danh
     *                     sách trạng thái cho phép.
     */
    public List<Appointment> getEligibleAppointments(String keyword, String statusFilter) throws SQLException {
        List<String> statuses = (statusFilter != null && ELIGIBLE_APPOINTMENT_STATUSES.contains(statusFilter))
                ? List.of(statusFilter)
                : ELIGIBLE_APPOINTMENT_STATUSES;
        return appointmentDAO.findByStatuses(statuses, keyword);
    }

    public Appointment getEligibleAppointment(int appointmentId) throws SQLException {
        Appointment appt = appointmentDAO.findById(appointmentId);
        if (appt == null || !ELIGIBLE_APPOINTMENT_STATUSES.contains(appt.getStatus())) {
            return null;
        }
        return appt;
    }

    /**
     * Tạo yêu cầu hoàn tiền do STAFF khởi tạo. Luôn tính lại số tiền đã
     * thanh toán thực tế (SUM Payments.Amount) từ DB - KHÔNG tin số fullRefund/
     * customAmount client gửi lên vượt quá con số này.
     */
    public int createManualRequest(int appointmentId, String reason, BigDecimal customAmount,
                                   boolean fullRefund, String bankCode, String accountNumber,
                                   String accountName) throws Exception {
        Appointment appt = getEligibleAppointment(appointmentId);
        if (appt == null) {
            throw new IllegalArgumentException(
                    "Lịch hẹn không hợp lệ hoặc chưa ở trạng thái có thể hoàn tiền.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập lý do hoàn tiền.");
        }
        if (isBlank(bankCode) || isBlank(accountNumber) || isBlank(accountName)) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ thông tin ngân hàng.");
        }

        Invoice invoice = invoiceDAO.findByAppointment(appointmentId);
        if (invoice == null) {
            throw new IllegalArgumentException("Không tìm thấy hoá đơn cho lịch hẹn này.");
        }
        BigDecimal totalPaid = sumPayments(invoice);
        if (totalPaid.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Lịch hẹn này chưa có khoản thanh toán nào để hoàn.");
        }

        BigDecimal refundAmount = fullRefund ? totalPaid : customAmount;
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền hoàn không hợp lệ.");
        }
        if (refundAmount.compareTo(totalPaid) > 0) {
            throw new IllegalArgumentException(
                    "Số tiền hoàn không được vượt quá số tiền đã thanh toán (" + totalPaid + "đ).");
        }

        Refund r = new Refund();
        r.setAppointmentID(appointmentId);
        r.setTotalAmount(invoice.getTotalAmount());
        r.setPaidAmount(refundAmount);
        r.setReason(reason.trim());
        r.setBankCode(bankCode.trim());
        r.setAccountNumber(accountNumber.trim());
        r.setAccountName(accountName.trim());

        return refundDAO.createRequest(r);
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
