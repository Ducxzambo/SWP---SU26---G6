package com.petclinic.service;

import com.petclinic.dao.CustomerDAO;
import com.petclinic.dao.NotificationDAO;
import com.petclinic.model.Appointment;
import com.petclinic.model.Customer;
import com.petclinic.model.Notification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

/**
 * Mỗi method:
 *  1. Tạo bản ghi Notification trong DB
 *  2. Gửi email tương ứng (async, không block)
 *
 * Gọi từ:
 *  - PaymentWebhookServlet     --> BOOKING_CONFIRMED, PAYMENT_SUCCESS, PAYMENT_PENDING
 *  - AppointmentServlet        --> BOOKING_CANCELLED
 *  - AppointmentStatusJob      --> nhắc lịch 48h/18h, EXAM_RESULT, VACCINE_DUE
 *  - Staff portal              --> CARE_TIP, SUPPORT
 */
public class NotificationService {

    private static final Logger LOG = Logger.getLogger(NotificationService.class.getName());

    private final NotificationDAO notifDAO   = new NotificationDAO();
    private final CustomerDAO     customerDAO= new CustomerDAO();
    private final EmailService    emailSvc   = new EmailService();

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_ONLY = DateTimeFormatter.ofPattern("HH:mm");

    // ═══════════════════════════════════════════════════════════════════════════
    //  BOOKING & APPOINTMENT
    // ═══════════════════════════════════════════════════════════════════════════

    /** Gọi sau khi thanh toán xong → appointment Confirmed. */
    public void onBookingConfirmed(Customer customer, Appointment appt,
                                   BigDecimal total, BigDecimal paid, boolean isFull) {
        String apptUrl  = "/appointments/detail?id=" + appt.getAppointmentID();
        String timeLabel = fmtAppt(appt);

        // In-app notification
        createSafe(customer.getCustomerID(),
                Notification.TYPE_BOOKING_CONFIRMED,
                "Lịch khám đã được xác nhận",
                "Dịch vụ: " + appt.getServiceName()
                        + " | " + timeLabel
                        + (appt.getStaffName() != null ? " | BS: " + appt.getStaffName() : ""),
                apptUrl);

        // Email
        emailSvc.onPaymentConfirmed(customer, appt, total, paid, isFull);

        // Schedule reminders
        scheduleReminders(customer, appt);
    }

    /** Gọi khi khách huỷ lịch. */
    public void onBookingCancelled(Customer customer, Appointment appt, String reason) {
        createSafe(customer.getCustomerID(),
                Notification.TYPE_BOOKING_CANCELLED,
                "Lịch hẹn đã bị huỷ",
                "Dịch vụ: " + appt.getServiceName()
                        + " | " + fmtAppt(appt)
                        + (reason != null && !reason.isBlank() ? " | Lý do: " + reason : ""),
                "/appointments");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  PAYMENT
    // ═══════════════════════════════════════════════════════════════════════════

    /** Gọi sau khi thanh toán thành công (webhook). */
    public void onPaymentSuccess(Customer customer, Appointment appt,
                                 BigDecimal amount, BigDecimal paid, boolean isFull) {
        String label = isFull ? "Thanh toán toàn bộ" : "Đặt cọc";
        createSafe(customer.getCustomerID(),
                Notification.TYPE_PAYMENT_SUCCESS,
                "💳 " + label + " thành công",
                fmtAmount(amount) + " – Lịch #" + appt.getAppointmentID()
                        + " | " + appt.getServiceName(),
                "/appointments/detail?id=" + appt.getAppointmentID());

        emailSvc.onPaymentConfirmed(customer, appt, amount, paid, isFull);
    }

    /** Gọi khi tạo appointment Pending (chưa thanh toán). */
    public void onPaymentPending(Customer customer, Appointment appt, BigDecimal total) {
        createSafe(customer.getCustomerID(),
                Notification.TYPE_PAYMENT_PENDING,
                "⏳ Lịch hẹn đang chờ thanh toán",
                "Vui lòng thanh toán để xác nhận lịch khám "
                        + appt.getServiceName() + " | " + fmtAppt(appt)
                        + " | Cần thanh toán: " + fmtAmount(total),
                "/appointments/pay?id=" + appt.getAppointmentID(),
                LocalDateTime.now().plusDays(7)); // hết hạn sau 1 tuần nếu không thanh toán
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  REMINDERS (48h & 18h)
    // ═══════════════════════════════════════════════════════════════════════════

    public void scheduleReminders(Customer customer, Appointment appt) {
        if (appt.getAppointmentDate() == null || appt.getStartTime() == null) return;
        LocalDateTime apptDt = LocalDateTime.of(appt.getAppointmentDate(), appt.getStartTime());

        // 48h reminder
        scheduleAt(apptDt.minusHours(48), () -> {
            onReminder(customer, appt, 48);
        });
        // 18h reminder
        scheduleAt(apptDt.minusHours(18), () -> {
            onReminder(customer, appt, 18);
        });
    }

    /** Gửi nhắc lịch (gọi từ scheduler). */
    public void onReminder(Customer customer, Appointment appt, int hoursAhead) {
        String type = hoursAhead >= 48
                ? Notification.TYPE_REMINDER_48H : Notification.TYPE_REMINDER_18H;

        // Refresh customer từ DB (tránh stale object từ session cũ)
        Customer freshCustomer = customer;
        try { freshCustomer = customerDAO.findById(customer.getCustomerID()); }
        catch (Exception ignored) {}
        if (freshCustomer == null) return;

        // Check appointment vẫn còn Confirmed (không bị huỷ)
        // (Caller - AppointmentStatusJob - đã check trước khi gọi)

        createSafe(freshCustomer.getCustomerID(),
                type,
                "Nhắc lịch: còn " + hoursAhead + " giờ",
                "Dịch vụ: " + appt.getServiceName()
                        + " | " + fmtAppt(appt)
                        + (appt.getStaffName() != null ? " | NV: " + appt.getStaffName() : ""),
                "/appointments/detail?id=" + appt.getAppointmentID());

        emailSvc.scheduleReminders(freshCustomer, appt);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  KẾT QUẢ KHÁM (EXAM_RESULT)
    // ═══════════════════════════════════════════════════════════════════════════

    /** Gọi khi bác sĩ hoàn tất bệnh án (appointment → Done). */
    public void onExamResult(int customerId, Appointment appt, String diagnosis) {
        String body = "Dịch vụ: " + appt.getServiceName()
                + " | " + appt.getPetName()
                + " | " + fmtAppt(appt);
        if (diagnosis != null && !diagnosis.isBlank()) {
            body += " | Chẩn đoán: " + truncate(diagnosis, 80);
        }
        createSafe(customerId,
                Notification.TYPE_EXAM_RESULT,
                "🩺 Kết quả khám đã được cập nhật",
                body,
                "/appointments/detail?id=" + appt.getAppointmentID());

        // Email kết quả khám
//        try {
//            Customer customer = customerDAO.findById(customerId);
//            if (customer != null) emailSvc.sendExamResultEmail(customer, appt, diagnosis);
//        } catch (Exception e) {
//            LOG.warning("sendExamResultEmail failed: " + e.getMessage());
//        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  VACCINE NHẮC HẸN (VACCINE_DUE)
    // ═══════════════════════════════════════════════════════════════════════════

    /** Gọi từ scheduler khi đến ngày tiêm nhắc. */
    public void onVaccineDue(int customerId, String petName, String vaccineName,
                             String dueDate) {
        createSafe(customerId,
                Notification.TYPE_VACCINE_DUE,
                "💉 Đến lịch tiêm vaccine: " + vaccineName,
                petName + " cần tiêm " + vaccineName + " vào " + dueDate
                        + ". Hãy đặt lịch sớm!",
                "/booking/new");

//        try {
//            Customer customer = customerDAO.findById(customerId);
//            if (customer != null)
//                emailSvc.sendVaccineDueEmail(customer, petName, vaccineName, dueDate);
//        } catch (Exception e) {
//            LOG.warning("sendVaccineDueEmail failed: " + e.getMessage());
//        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  CHĂM SÓC & HỖ TRỢ (CARE_TIP / SUPPORT) — Gửi từ Staff
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Staff gửi mẹo chăm sóc đến 1 customer hoặc broadcast (customerId = null).
     * Nếu customerId = null → lấy danh sách customer active từ DB và gửi cho tất cả.
     */
    public void sendCareTip(Integer customerId, Integer staffId,
                            String title, String body) {
        if (customerId != null) {
            // Gửi 1 customer cụ thể
            Notification n = buildNotif(customerId, staffId,
                    Notification.TYPE_CARE_TIP, title, body, null, null);
            saveSafe(n);
        } else {
            // Broadcast — lấy danh sách từ DB, giới hạn 500 để tránh quá tải
            try {
                java.util.List<Customer> customers = customerDAO.findAllActive(500);
                for (Customer c : customers) {
                    Notification n = buildNotif(c.getCustomerID(), staffId,
                            Notification.TYPE_CARE_TIP, title, body, null, null);
                    saveSafe(n);
                }
                LOG.info("[NotificationService] CareTip broadcast to " + customers.size() + " customers");
            } catch (Exception e) {
                LOG.warning("Broadcast careTip failed: " + e.getMessage());
            }
        }
    }

    /** Staff gửi hỗ trợ / trả lời hỏi đáp đến 1 customer. */
    public void sendSupport(int customerId, Integer staffId,
                            String title, String body, String actionUrl) {
        Notification n = buildNotif(customerId, staffId,
                Notification.TYPE_SUPPORT, title, body, actionUrl, null);
        saveSafe(n);
        // Optionally email
//        try {
//            Customer c = customerDAO.findById(customerId);
//            if (c != null) emailSvc.sendSupportMessageEmail(c, title, body);
//        } catch (Exception e) {
//            LOG.warning("sendSupportEmail failed: " + e.getMessage());
//        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Internal helpers
    // ═══════════════════════════════════════════════════════════════════════════

    private void createSafe(int customerId, String type, String title, String body,
                            String actionUrl) {
        createSafe(customerId, type, title, body, actionUrl, null);
    }

    private void createSafe(int customerId, String type, String title, String body,
                            String actionUrl, LocalDateTime expiresAt) {
        Notification n = buildNotif(customerId, null, type, title, body, actionUrl, expiresAt);
        saveSafe(n);
    }

    private Notification buildNotif(Integer customerId, Integer staffId, String type,
                                    String title, String body, String actionUrl,
                                    LocalDateTime expiresAt) {
        Notification n = new Notification();
        n.setCustomerID(customerId);
        n.setStaffID(staffId);
        n.setType(type);
        n.setTitle(title);
        n.setBody(body);
        n.setActionUrl(actionUrl);
        n.setExpiresAt(expiresAt);
        return n;
    }

    private void saveSafe(Notification n) {
        try { notifDAO.create(n); }
        catch (Exception e) { LOG.warning("Notification create failed: " + e.getMessage()); }
    }

    private void scheduleAt(LocalDateTime target, Runnable task) {
        long delayMs = java.time.Duration.between(LocalDateTime.now(), target).toMillis();
        if (delayMs <= 0) return;
        java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "notif-scheduler");
            t.setDaemon(true);
            return t;
        }).schedule(() -> {
            try { task.run(); }
            catch (Exception e) { LOG.warning("Scheduled notif failed: " + e.getMessage()); }
        }, delayMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    private String fmtAppt(Appointment a) {
        if (a.getAppointmentDate() == null) return "";
        return a.getAppointmentDate().format(DATE_ONLY)
                + (a.getStartTime() != null ? " lúc " + a.getStartTime().format(TIME_ONLY) : "");
    }

    private String fmtAmount(BigDecimal v) {
        if (v == null) return "0₫";
        return String.format("%,.0f₫", v.doubleValue());
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
