package com.petclinic.servlet.payment;

import com.petclinic.dao.AppointmentDAO;
import com.petclinic.dao.CustomerDAO;
import com.petclinic.dao.InvoiceDAO;
import com.petclinic.dao.ServiceDAO;
import com.petclinic.model.Appointment;
import com.petclinic.model.Customer;
import com.petclinic.model.Invoice;
import com.petclinic.service.EmailService;
import com.petclinic.service.PaymentService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Two endpoints:
 *   POST /payment/webhook   – PayOS async webhook (server-to-server)
 *   GET  /payment/result    – Customer return URL after PayOS checkout
 */
@WebServlet(urlPatterns = {"/payment/webhook", "/payment/result"})
public class PaymentWebhookServlet extends HttpServlet {

    private final PaymentService  paymentSvc     = new PaymentService();
    private final EmailService    emailSvc       = new EmailService();
    private final AppointmentDAO  appointmentDAO = new AppointmentDAO();
    private final CustomerDAO     customerDAO    = new CustomerDAO();
    private final InvoiceDAO      invoiceDAO     = new InvoiceDAO();
    private final ServiceDAO      serviceDAO     = new ServiceDAO();

    // ── Webhook (server-to-server from PayOS) ─────────────────────────────────
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Read raw body
        String body = req.getReader().lines().collect(Collectors.joining());
        // PayOS sends signature in header x-payos-signature
        String signature = req.getHeader("x-payos-signature");
        if (signature == null) signature = "";

        boolean ok = false;
        try {
            ok = paymentSvc.handleWebhook(body, signature);
            if (ok) {
                // Fire confirmation email asynchronously
                sendConfirmationEmailFromWebhook(body);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // PayOS expects HTTP 200 regardless
        resp.setStatus(200);
        resp.setContentType("application/json");
        resp.getWriter().write(ok ? "{\"error\":0}" : "{\"error\":1,\"message\":\"ignored\"}");
    }

    // ── Return page (customer lands here after PayOS checkout) ─────────────────
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String apptIdStr   = req.getParameter("apptId");
        String invoiceIdStr= req.getParameter("invoiceId");
        String fullStr     = req.getParameter("full");
        String status      = req.getParameter("status"); // PayOS: "PAID" | "CANCELLED"

        if (apptIdStr == null) { resp.sendRedirect(req.getContextPath() + "/appointments"); return; }

        int     apptId    = Integer.parseInt(apptIdStr);
        boolean wasCancelled = "CANCELLED".equalsIgnoreCase(status);

        if (wasCancelled) {
            // Customer cancelled on PayOS page — appointment stays Pending
            req.getSession().setAttribute("flashError",
                    "Thanh toán bị huỷ. Lịch hẹn của bạn vẫn đang chờ xác nhận. Bạn có thể thanh toán lại từ chi tiết lịch hẹn.");
            resp.sendRedirect(req.getContextPath() + "/appointments/detail?id=" + apptId);
            return;
        }

        // Payment likely succeeded (webhook may arrive before/after this redirect)
        // Load invoice to show result
        try {
            Invoice invoice = invoiceIdStr != null
                    ? invoiceDAO.findByAppointment(apptId) : null;
            Appointment appt = appointmentDAO.findById(apptId);

            req.setAttribute("appt",         appt);
            req.setAttribute("invoice",       invoice);
            req.setAttribute("navCategories", serviceDAO.findAllCategoriesWithServices());

            HttpSession sess = req.getSession(false);
            Customer customer = sess != null ? (Customer) sess.getAttribute("customer") : null;
            req.setAttribute("customer", customer);

            // Clean up payment session attrs
            if (sess != null) {
                sess.removeAttribute("pay_apptId");
                sess.removeAttribute("pay_apptIds");
                sess.removeAttribute("pay_invoiceId");
                sess.removeAttribute("pay_total");
                sess.removeAttribute("pay_deposit");
                sess.removeAttribute("pay_inpatient");
            }

            req.getRequestDispatcher("/WEB-INF/views/booking/payment-result.jsp")
                    .forward(req, resp);

        } catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect(req.getContextPath() + "/appointments");
        }
    }

    // ── Send confirmation email triggered by webhook ───────────────────────────
    private void sendConfirmationEmailFromWebhook(String webhookBody) {
        try {
            String orderCodeStr = extractField(webhookBody, "orderCode");
            if (orderCodeStr.isBlank()) return;
            long    orderCode = Long.parseLong(orderCodeStr);
            int     invoiceId = (int)(orderCode / 10);
            boolean isFull    = (orderCode % 10) == 1;
            long    amount    = Long.parseLong(extractField(webhookBody, "amount"));

            // Load invoice directly by ID
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
}