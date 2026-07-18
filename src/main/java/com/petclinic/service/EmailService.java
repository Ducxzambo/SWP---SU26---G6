package com.petclinic.service;

import com.petclinic.dao.AppointmentDAO;
import com.petclinic.model.Appointment;
import com.petclinic.model.Customer;
import com.petclinic.util.DBConnection;
import com.petclinic.util.OtpUtil;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Email service: invoice delivery + reminder scheduler.
 *
 * Reminder rules:
 *   – Email 1: 48 hours before appointment
 *   – Email 2: 18 hours before appointment
 *
 * Scheduling: uses a single ScheduledExecutorService.
 * In production, replace with Quartz or a DB-persisted job table
 * so reminders survive server restarts.
 */
public class EmailService {

    private static final Logger LOG = Logger.getLogger(EmailService.class.getName());
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy");
    private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Shared scheduler
    private static final ScheduledExecutorService SCHEDULER =
            Executors.newScheduledThreadPool(2, r -> {
                Thread t = new Thread(r, "petclinic-email-scheduler");
                t.setDaemon(true);
                return t;
            });

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Sent when a Pending/Confirmed appointment's end time has passed
     * but the customer did not show up or the slot was not completed.
     */
    public void sendOverdueNotification(Customer customer, Appointment appt) {
        sendAsync(() -> {
            String subject = "[PetClinic] Thông báo: Lịch hẹn #" + appt.getAppointmentID()
                    + " đã qua thời gian hẹn";
            String body = "<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body>"
                    + "<div style='font-family:sans-serif;max-width:560px;margin:auto;padding:24px;'>"
                    + "<h2 style='color:#856404;'>PetClinic – Lịch hẹn đã qua giờ</h2>"
                    + "<p>Xin chào <strong>" + esc(customer.getFullName()) + "</strong>,</p>"
                    + "<p>Chúng tôi nhận thấy lịch hẹn sau <strong>đã qua thời gian hẹn</strong> "
                    + "mà không có thông tin hoàn thành:</p>"
                    + "<div style='background:#fff3cd;border:1px solid #ffc107;border-radius:10px;"
                    +      "padding:18px 20px;margin:20px 0;'>"
                    + "<table style='width:100%;border-collapse:collapse;font-size:14px;'>"
                    + row("Dịch vụ",  esc(appt.getServiceName()))
                    + row("Thú cưng", esc(appt.getPetName()))
                    + row("Thời gian", fmtAppt(appt))
                    + row("Trạng thái", "<span style='color:#856404;font-weight:600;'>Quá giờ</span>")
                    + "</table></div>"
                    + "<p>Nếu bạn đã đến khám, vui lòng liên hệ chúng tôi để cập nhật.</p>"
                    + "<p>Nếu bạn không thể đến, lịch hẹn sẽ bị đánh dấu <strong>Vắng mặt</strong> sau 24 giờ.</p>"
                    + "<p style='font-size:13px;color:#8c8680;'>Hotline: <strong>(028) 123 456 789</strong></p>"
                    + "</div></body></html>";
            boolean ok = send(customer.getEmail(), subject, body);
            if (ok) {
                LOG.info("Overdue notification sent to " + customer.getEmail()
                        + " for appt #" + appt.getAppointmentID());
            } else {
                LOG.warning("Overdue notification NOT sent to " + customer.getEmail()
                        + " for appt #" + appt.getAppointmentID());
            }
        });
    }

    /**
     * Called after payment confirmed.
     * Sends invoice + booking confirmation, then schedules two reminders.
     */
    public void onPaymentConfirmed(Customer customer, Appointment appt,
                                   BigDecimal total, BigDecimal paid,
                                   boolean isFullPayment) {
        sendAsync(() -> sendConfirmationEmail(customer, appt, total, paid, isFullPayment));
        scheduleReminders(customer, appt);
    }

    /**
     * Gửi email thông báo nội bộ khi có tin nhắn liên hệ mới từ trang /contact.
     * Đây là kênh ghi nhận DUY NHẤT cho tin nhắn liên hệ (không có bảng
     * ContactMessages trong DB hiện tại) — best-effort: nếu SMTP lỗi/chưa
     * cấu hình, chỉ log cảnh báo, không throw, để không chặn phản hồi cho người dùng.
     */
    public void sendContactNotification(String fullName, String fromEmail, String phone,
                                        String subject, String messageBody) {
        sendAsync(() -> {
            final String EMAIL_FROM = System.getenv("EMAIL_FROM");
            if (EMAIL_FROM == null || EMAIL_FROM.isBlank()) {
                LOG.info("Tin nhắn liên hệ không được gửi email nội bộ (SMTP chưa cấu hình EMAIL_FROM).");
                return;
            }
            String mailSubject = "[PetClinic] Liên hệ mới: " + subject;
            String body = "<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body>"
                    + "<div style='font-family:sans-serif;max-width:560px;margin:auto;padding:24px;'>"
                    + "<h2 style='color:#0f3d24;'>🐾 PetClinic – Tin nhắn liên hệ mới</h2>"
                    + "<table style='width:100%;border-collapse:collapse;font-size:14px;'>"
                    + row("Họ tên",   esc(fullName))
                    + row("Email",    esc(fromEmail))
                    + row("SĐT",      phone != null && !phone.isBlank() ? esc(phone) : "-")
                    + row("Tiêu đề",  esc(subject))
                    + "</table>"
                    + "<p style='white-space:pre-line;margin-top:16px;'>" + esc(messageBody) + "</p>"
                    + "</div></body></html>";
            boolean ok = send(EMAIL_FROM, mailSubject, body);
            if (!ok) LOG.warning("Không gửi được email thông báo liên hệ mới (subject: " + subject + ")");
        });
    }

    /** Schedule 48h and 18h reminders. */
    public void scheduleReminders(Customer customer, Appointment appt) {
        if (appt.getAppointmentDate() == null || appt.getStartTime() == null) return;

        LocalDateTime apptDt = LocalDateTime.of(appt.getAppointmentDate(), appt.getStartTime());
        LocalDateTime now    = LocalDateTime.now();

        scheduleAt(apptDt.minusHours(48), now, () -> sendReminderEmail(customer, appt, 48));
        scheduleAt(apptDt.minusHours(18), now, () -> sendReminderEmail(customer, appt, 18));
    }

    // ── Email builders ────────────────────────────────────────────────────────

    private void sendConfirmationEmail(Customer customer, Appointment appt,
                                       BigDecimal total, BigDecimal paid, boolean isFullPayment) {
        String subject = "[PetClinic] Xác nhận đặt lịch #" + appt.getAppointmentID();
        String body = buildConfirmHtml(customer, appt, total, paid, isFullPayment);
        boolean ok = send(customer.getEmail(), subject, body);
        if (ok) {
            LOG.info("Confirmation email sent to " + customer.getEmail());
        } else {
            LOG.warning("Confirmation email NOT sent to " + customer.getEmail()
                    + " (appt #" + appt.getAppointmentID() + ") — xem log 'Email send failed' phía trên để biết nguyên nhân");
        }
    }

    private void sendReminderEmail(Customer customer, Appointment appt, int hoursAhead) {
        String subject = "[PetClinic] Nhắc nhở – Lịch khám còn " + hoursAhead + " giờ nữa";
        String body    = buildReminderHtml(customer, appt, hoursAhead);
        boolean ok = send(customer.getEmail(), subject, body);
        if (ok) {
            LOG.info("Reminder (" + hoursAhead + "h) sent to " + customer.getEmail());
        } else {
            LOG.warning("Reminder (" + hoursAhead + "h) NOT sent to " + customer.getEmail()
                    + " (appt #" + appt.getAppointmentID() + ")");
        }
    }

    // ── HTML templates ────────────────────────────────────────────────────────

    private String buildConfirmHtml(Customer customer, Appointment appt,
                                    BigDecimal total, BigDecimal paid, boolean isFullPayment) {
        String paymentNote = isFullPayment
                ? "<span style='color:#155724;font-weight:600;'>✓ Đã thanh toán toàn bộ</span>"
                : "<span style='color:#856404;font-weight:600;'>Đặt cọc " + fmt(paid) + "₫ / " + fmt(total) + "₫</span>";

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body>"
                + "<div style='font-family:sans-serif;max-width:560px;margin:auto;padding:24px;'>"
                + "<h2 style='color:#0f3d24;'>🐾 PetClinic – Xác nhận đặt lịch</h2>"
                + "<p>Xin chào <strong>" + esc(customer.getFullName()) + "</strong>,</p>"
                + "<p>Lịch khám của bạn đã được xác nhận thành công.</p>"
                + "<div style='background:#f0faf4;border:1px solid #d6f0e2;border-radius:10px;"
                +      "padding:18px 20px;margin:20px 0;'>"
                + "<table style='width:100%;border-collapse:collapse;font-size:14px;'>"
                + row("Dịch vụ",    esc(appt.getServiceName()))
                + row("Thú cưng",   esc(appt.getPetName()))
                + row("Thời gian",  fmtAppt(appt))
                + row("Bác sĩ",     appt.getStaffName() != null ? esc(appt.getStaffName()) : "Sẽ được phân công")
                + row("Thanh toán", paymentNote)
                + "</table></div>"
                + "<p style='font-size:13px;color:#8c8680;'>Nếu cần thay đổi lịch, vui lòng liên hệ trước ít nhất 12 giờ.</p>"
                + "<p style='font-size:13px;color:#8c8680;'>Hotline: <strong>(028) 123 456 789</strong></p>"
                + "<hr style='border:none;border-top:1px solid #d8d4cc;margin:20px 0;'>"
                + "<p style='font-size:12px;color:#b8b4ae;text-align:center;'>© 2025 PetClinic. 123 Đường ABC, Q.1, TP.HCM</p>"
                + "</div></body></html>";
    }

    private String buildReminderHtml(Customer customer, Appointment appt, int hoursAhead) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body>"
                + "<div style='font-family:sans-serif;max-width:560px;margin:auto;padding:24px;'>"
                + "<h2 style='color:#0f3d24;'>🐾 PetClinic – Nhắc lịch khám</h2>"
                + "<p>Xin chào <strong>" + esc(customer.getFullName()) + "</strong>,</p>"
                + "<p>Đây là nhắc nhở: bạn có lịch khám <strong>còn " + hoursAhead + " giờ nữa</strong>.</p>"
                + "<div style='background:#fff3cd;border:1px solid #ffc107;border-radius:10px;"
                +      "padding:18px 20px;margin:20px 0;'>"
                + "<table style='width:100%;border-collapse:collapse;font-size:14px;'>"
                + row("Dịch vụ",  esc(appt.getServiceName()))
                + row("Thú cưng", esc(appt.getPetName()))
                + row("Thời gian",fmtAppt(appt))
                + "</table></div>"
                + "<p style='font-size:13px;color:#8c8680;'>Địa chỉ: 123 Đường ABC, Q.1, TP.HCM</p>"
                + "<p style='font-size:13px;color:#8c8680;'>Hotline: <strong>(028) 123 456 789</strong></p>"
                + "<hr style='border:none;border-top:1px solid #d8d4cc;margin:20px 0;'>"
                + "<p style='font-size:12px;color:#b8b4ae;text-align:center;'>© 2025 PetClinic</p>"
                + "</div></body></html>";
    }

    // ── Core send ─────────────────────────────────────────────────────────────

    private boolean send(String to, String subject, String htmlBody) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth",            "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host",            "smtp.gmail.com");
            props.put("mail.smtp.port",            "587");

            final String EMAIL_FROM = System.getenv("EMAIL_FROM");
            final String user = System.getenv("SMTP_USER");
            final String pass = System.getenv("SMTP_PASS");

            Session session = Session.getInstance(props, new Authenticator() {
                @Override protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass);
                }
            });

            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(EMAIL_FROM, "PetClinic"));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            msg.setSubject(subject);
            msg.setContent(htmlBody, "text/html; charset=UTF-8");
            Transport.send(msg);
            return true;

        } catch (Exception e) {
            LOG.warning("Email send failed to " + to + ": " + e.getMessage());
            return false;
        }
    }

    private void sendAsync(Runnable task) {
        SCHEDULER.submit(() -> {
            try { task.run(); }
            catch (Exception e) { LOG.warning("Async email failed: " + e.getMessage()); }
        });
    }

    private void scheduleAt(LocalDateTime target, LocalDateTime now, Runnable task) {
        long delayMs = java.time.Duration.between(now, target).toMillis();
        if (delayMs <= 0) return; // already past
        SCHEDULER.schedule(() -> {
            try { task.run(); }
            catch (Exception e) { LOG.warning("Scheduled email failed: " + e.getMessage()); }
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    // ── HTML helpers ──────────────────────────────────────────────────────────

    private String row(String label, String value) {
        return "<tr><td style='padding:7px 0;color:#8c8680;width:130px;'>" + label + "</td>"
                + "<td style='padding:7px 0;font-weight:500;'>" + value + "</td></tr>";
    }

    private String fmtAppt(Appointment a) {
        if (a.getAppointmentDate() == null) return "-";
        String date = a.getAppointmentDate().format(DATE_ONLY);
        String time = a.getStartTime() != null
                ? a.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")) : "";
        return date + " lúc " + time;
    }

    private String fmt(BigDecimal v) {
        if (v == null) return "0";
        return String.format("%,.0f", v.doubleValue());
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }
}