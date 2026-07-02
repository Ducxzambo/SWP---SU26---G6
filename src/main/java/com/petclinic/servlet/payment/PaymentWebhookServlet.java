package com.petclinic.servlet.payment;

import com.petclinic.dao.AppointmentDAO;
import com.petclinic.dao.CustomerDAO;
import com.petclinic.dao.InvoiceDAO;
import com.petclinic.dao.ServiceDAO;
import com.petclinic.dao.NotificationDAO;
import com.petclinic.model.Appointment;
import com.petclinic.model.Customer;
import com.petclinic.model.Invoice;
import com.petclinic.service.BookingService;
import com.petclinic.service.EmailService;
import com.petclinic.service.PaymentService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *   POST /payment/webhook   – PayOS async webhook (server-to-server, không thay đổi)
 *   GET  /payment/result    – Customer return URL sau khi PayOS checkout
 */
@WebServlet(urlPatterns = {"/payment/webhook", "/payment/result"})
public class PaymentWebhookServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(PaymentWebhookServlet.class.getName());

    private final PaymentService  paymentSvc     = new PaymentService();
    private final EmailService    emailSvc       = new EmailService();
    private final AppointmentDAO  appointmentDAO = new AppointmentDAO();
    private final CustomerDAO     customerDAO    = new CustomerDAO();
    private final InvoiceDAO      invoiceDAO     = new InvoiceDAO();
    private final ServiceDAO      serviceDAO     = new ServiceDAO();

    // ═══════════════════════════════════════════════════════════════════════════
    // POST /payment/webhook
    // ═══════════════════════════════════════════════════════════════════════════
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String body      = req.getReader().lines().collect(Collectors.joining());
        String signature = req.getHeader("x-payos-signature");
        if (signature == null) signature = "";

        boolean ok = false;
        try {
            ok = paymentSvc.handleWebhook(body, signature);
            if (ok) sendConfirmationEmailFromWebhook(body);
        } catch (Exception e) {
            e.printStackTrace();
        }

        resp.setStatus(200);
        resp.setContentType("application/json");
        resp.getWriter().write(ok ? "{\"error\":0}" : "{\"error\":1,\"message\":\"ignored\"}");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GET /payment/result
    //
    // PayOS redirect URL params:
    //   code       = "00"   - giao dịch thành công
    //   status     = "PAID" | "CANCELLED" | "PENDING"
    //   cancel     = "true" - khách bấm Cancel
    //   orderCode  = order code đã tạo
    //   id         = PayOS internal transaction ID
    //
    // own params (appended khi tạo payment link):
    //   apptId     = AppointmentID
    //   invoiceId  = InvoiceID
    //   full       = "true" nếu chọn Fully Paid, "false" nếu deposit
    // ═══════════════════════════════════════════════════════════════════════════
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // ── 1. Đọc params ────────────────────────────────────────────────────
        String code        = req.getParameter("code");    // "00" = success
        String statusParam = req.getParameter("status");  // "PAID" | "CANCELLED" | "PENDING"
        String cancelParam = req.getParameter("cancel");  // "true" nếu user huỷ
        String apptIdStr   = req.getParameter("apptId");
        String invoiceIdStr= req.getParameter("invoiceId");
        String fullStr     = req.getParameter("full");

        LOG.info("[PaymentResult] code=" + code + " status=" + statusParam
                + " cancel=" + cancelParam
                + " apptId=" + apptIdStr + " invoiceId=" + invoiceIdStr);

        // ── 2. Auth check ────────────────────────────────────────────────────
        HttpSession session  = req.getSession(false);
        Customer    customer = session != null
                ? (Customer) session.getAttribute("customer") : null;
        if (customer == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return;
        }

        // ── 3. Parse IDs ─────────────────────────────────────────────────────
        int apptId    = parseId(apptIdStr);
        int invoiceId = parseId(invoiceIdStr);

        if (apptId <= 0) {
            // Không có apptId → về danh sách
            resp.sendRedirect(req.getContextPath() + "/appointments");
            return;
        }

        // ── 4. Xác định kết quả từ PayOS ────────────────────────────────────
        boolean cancelled = "true".equalsIgnoreCase(cancelParam)
                || "CANCELLED".equalsIgnoreCase(statusParam);
        boolean paid      = !cancelled
                && "00".equals(code)
                && "PAID".equalsIgnoreCase(statusParam);
        boolean full      = "true".equalsIgnoreCase(fullStr);

        // ── 5. Nếu PAID → update DB (idempotent) ────────────────────────────
        if (paid && invoiceId > 0) {
            try {
                Invoice current = invoiceDAO.findById(invoiceId);

                if (current != null && "Unpaid".equals(current.getStatus())) {
                    // Tính số tiền đã trả
                    Appointment appt = appointmentDAO.findById(apptId);
                    boolean isInpatient = appt != null && appt.getStartTime() != null && appt.getEndTime() != null
                            && java.time.Duration.between(appt.getStartTime(), appt.getEndTime()).toMinutes() >= 240;
                    long deposit = computeDeposit(isInpatient);
                    BigDecimal invoiceTotal = current.getTotalAmount();
                    if (isInpatient && (invoiceTotal == null || invoiceTotal.signum() <= 0)) {
                        invoiceTotal = BigDecimal.valueOf(deposit);
                    }
                    BigDecimal paidAmount = full ? invoiceTotal : BigDecimal.valueOf(deposit);

                    // Cập nhật Invoice status
                    String newStatus = full ? "Paid" : "PartiallyPaid";
                    invoiceDAO.updateStatus(invoiceId, newStatus);

                    // Ghi payment record
                    invoiceDAO.insertPayment(invoiceId, paidAmount, "E-Wallet");

                    // Cập nhật Appointment → Confirmed
                    if (appt != null && "Pending".equals(appt.getStatus())) {
                        appointmentDAO.updateStatus(apptId, "Confirmed");
                    }

                    // Gửi email xác nhận (async, không block response)
                    Appointment freshAppt = appointmentDAO.findById(apptId);
                    Invoice     freshInv  = invoiceDAO.findById(invoiceId);
                    if (freshAppt != null && freshInv != null) {
                        emailSvc.onPaymentConfirmed(customer, freshAppt,
                                freshInv.getTotalAmount(), paidAmount, full);
                    }

                    LOG.info("[PaymentResult] Updated: invoice #" + invoiceId
                            + " → " + newStatus + ", appt #" + apptId + " → Confirmed");
                } else {
                    LOG.info("[PaymentResult] Invoice #" + invoiceId
                            + " already processed (" + (current != null ? current.getStatus() : "null")
                            + ") — skipped (idempotent)");
                }
            } catch (Exception e) {
                LOG.warning("[PaymentResult] DB update failed: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // ── 6. Cleanup session payment attrs ────────────────────────────────
        if (session != null) {
            session.removeAttribute("pay_apptId");
            session.removeAttribute("pay_apptIds");
            session.removeAttribute("pay_invoiceId");
            session.removeAttribute("pay_total");
            session.removeAttribute("pay_deposit");
            session.removeAttribute("pay_inpatient");
        }

        // ── 7. Forward tới JSP ───────────────────────────────────────────────
        try {
            Appointment appt    = appointmentDAO.findById(apptId);
            Invoice     invoice = invoiceId > 0 ? invoiceDAO.findById(invoiceId) : null;

            req.setAttribute("appt",         appt);
            req.setAttribute("invoice",       invoice);
            req.setAttribute("paid",          paid);
            req.setAttribute("cancelled",     cancelled);
            req.setAttribute("full",          full);
            req.setAttribute("customer",      customer);
            req.setAttribute("navCategories", serviceDAO.findAllCategoriesWithServices());
            req.setAttribute("unreadCount",
                    new NotificationDAO().countUnread(customer.getCustomerID()));

            req.getRequestDispatcher("/WEB-INF/views/booking/payment-result.jsp")
                    .forward(req, resp);

        } catch (Exception e) {
            LOG.warning("[PaymentResult] Forward failed: " + e.getMessage());
            e.printStackTrace();
            // Fallback: redirect với flash message
            if (session != null) {
                session.setAttribute(paid ? "flashSuccess" : "flashError",
                        paid ? "Thanh toán thành công! Lịch hẹn đã được xác nhận."
                                : "Thanh toán bị huỷ.");
            }
            resp.sendRedirect(req.getContextPath() + "/appointments/detail?id=" + apptId);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ═══════════════════════════════════════════════════════════════════════════

    private void sendConfirmationEmailFromWebhook(String webhookBody) {
        try {
            String orderCodeStr = extractField(webhookBody, "orderCode");
            if (orderCodeStr.isBlank()) return;
            long    orderCode = Long.parseLong(orderCodeStr);
            int     invoiceId = paymentSvc.decodeInvoiceId(orderCode);
            boolean isFull    = paymentSvc.decodeIsFullPayment(orderCode);
            long    amount    = Long.parseLong(extractField(webhookBody, "amount"));

            Invoice invoice = invoiceDAO.findById(invoiceId);
            if (invoice == null) return;

            Appointment appt = appointmentDAO.findById(invoice.getAppointmentID());
            if (appt == null) return;

            Customer customer = customerDAO.findById(appt.getCustomerID());
            if (customer == null) return;

            emailSvc.onPaymentConfirmed(customer, appt,
                    invoice.getTotalAmount(), BigDecimal.valueOf(amount), isFull);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String extractField(String json, String field) {
        String key = "\"" + field + "\"";
        int idx = json.indexOf(key);
        if (idx < 0) return "";
        int colon = json.indexOf(':', idx + key.length());
        if (colon < 0) return "";
        int start = colon + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;
        if (start >= json.length()) return "";
        char first = json.charAt(start);
        if (first == '"') {
            int end = json.indexOf('"', start + 1);
            return end < 0 ? "" : json.substring(start + 1, end);
        } else {
            int end = start;
            while (end < json.length() && ",}]".indexOf(json.charAt(end)) < 0) end++;
            return json.substring(start, end).trim();
        }
    }

    private int parseId(String s) {
        if (s == null || s.isBlank()) return -1;
        try { return Integer.parseInt(s.split(",")[0].trim()); }
        catch (Exception e) { return -1; }
    }

    /** Tiền cọc cố định */
    private long computeDeposit(boolean isInpatient) {
        return isInpatient ? BookingService.DEPOSIT_INPATIENT : BookingService.DEPOSIT_NORMAL;
    }
}