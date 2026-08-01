package com.petclinic.admin.servlet.payment;

import com.petclinic.backend.dao.AppointmentDAO;
import com.petclinic.backend.dao.CustomerDAO;
import com.petclinic.backend.dao.InvoiceDAO;
import com.petclinic.backend.dto.ReceiptData;
import com.petclinic.backend.model.Appointment;
import com.petclinic.backend.model.Customer;
import com.petclinic.backend.model.Invoice;
import com.petclinic.backend.model.Staff;
import com.petclinic.backend.service.EmailService;
import com.petclinic.backend.service.PaymentService;

import com.petclinic.backend.service.ReceiptService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@WebServlet(urlPatterns = {
        "/payment/webhook",
        "/payment/result"
})
public class PaymentWebhookServlet extends HttpServlet {

    private static final Logger LOG =
            Logger.getLogger(PaymentWebhookServlet.class.getName());

    private final PaymentService paymentSvc = new PaymentService();
    private final InvoiceDAO invoiceDAO = new InvoiceDAO();
    private final ReceiptService receiptService = new ReceiptService();
    private final EmailService emailService   = new EmailService();
    private final CustomerDAO customerDAO    = new CustomerDAO();
    private final AppointmentDAO appointmentDAO    = new AppointmentDAO();

    // PayOS server-to-server webhook callback
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String body = req.getReader().lines().collect(Collectors.joining());

        String signature = req.getHeader("x-payos-signature");
        if (signature == null) {
            signature = "";
        }

        boolean ok = false;

        try {
            ok = paymentSvc.handleWebhook(body, signature);
        } catch (Exception e) {
            e.printStackTrace();
        }

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        resp.getWriter().write(ok ? "{\"error\":0}" : "{\"error\":1,\"message\":\"ignored\"}"
        );
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);

        Staff staff = (session != null)
                ? (Staff) session.getAttribute("staff")
                : null;

        if (staff == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/staff/login");
            return;
        }

        String source = req.getParameter("source");

        int colonIdx = (source != null) ? source.indexOf(':') : -1;

        String from = (colonIdx >= 0) ? source.substring(colonIdx + 1) : "checkin";

        String code = req.getParameter("code");
        String statusParam = req.getParameter("status");
        String cancelParam = req.getParameter("cancel");

        int invoiceId = parseId(req.getParameter("invoiceId"));
        long amount = parseAmount(req.getParameter("amount"));
        Integer staffIdParam = parseNullableId(req.getParameter("staffId"));

        LOG.info("[PaymentResult]" + " code=" + code + " status=" + statusParam
                        + " cancel=" + cancelParam + " invoiceId=" + invoiceId
                        + " amount=" + amount);

        if (invoiceId <= 0) {
            resp.sendRedirect(req.getContextPath() + "/receptionist/checkin");
            return;
        }

        boolean cancelled = "true".equalsIgnoreCase(cancelParam)
                        || "CANCELLED".equalsIgnoreCase(statusParam);

        boolean paid = !cancelled && "00".equals(code)
                && "PAID".equalsIgnoreCase(statusParam);

        if (paid && amount > 0) {
            try {
                Invoice invoice = invoiceDAO.findById(invoiceId);
                if (invoice != null) {
                    BigDecimal amountPaid = invoiceDAO.getAmountPaid(invoiceId);
                    BigDecimal amountDue = invoice.getTotalAmount().subtract(amountPaid);
                    if (amountDue.compareTo(BigDecimal.ZERO) > 0) {
                        int attributedStaffId = (staffIdParam != null) ? staffIdParam : staff.getStaffID();
                        invoiceDAO.insertPayment(invoiceId, BigDecimal.valueOf(amount), "BankTransfer", attributedStaffId);
                    }
                }
                session.setAttribute("flashSuccess", "Thanh toán chuyển khoản thành công!");
                sendReceiptEmail(invoiceId);
                resp.sendRedirect(req.getContextPath() + "/receptionist/invoice/receipt?invoiceId=" + invoiceId + "&from=" + from);
                return;

            } catch (Exception e) {
                LOG.warning("[PaymentResult] DB update failed: " + e.getMessage());
                e.printStackTrace();
                session.setAttribute("flashError", "Lỗi cập nhật thanh toán: " + e.getMessage());
            }
        } else if (cancelled) {
            session.setAttribute("flashWarning", "Giao dịch chuyển khoản đã bị huỷ.");
        } else {
            session.setAttribute("flashWarning", "Chưa xác nhận được thanh toán, vui lòng thử lại.");
        }

        resp.sendRedirect(req.getContextPath()  + "/receptionist/invoice?invoiceId=" + invoiceId + "&from=" + from);

    }

    // Helpers
    private void sendReceiptEmail(int invoiceId) {
        try {
            Invoice invoice = invoiceDAO.findById(invoiceId);
            if (invoice == null) return;
            Appointment appt = appointmentDAO.findById(invoice.getAppointmentID());
            Customer customer = customerDAO.findById(invoice.getCustomerID());
            if (customer == null || customer.getEmail() == null || customer.getEmail().isBlank()) return;
            ReceiptData receipt = receiptService.buildReceiptForDownload(invoice, appt, customer);
            if (receipt == null) return;
            String pdfUrl = System.getenv().getOrDefault("APP_BASE_URL", "") + "/invoices/pdf?invoiceId=" + invoiceId;
            emailService.sendInvoiceEmail(customer, receipt, pdfUrl);
        } catch (Exception ignored) {}
    }

    private long parseAmount(String s) {
        if (s == null || s.isBlank()) {
            return -1;
        }

        try {
            return Long.parseLong(s.trim());
        } catch (Exception e) {
            return -1;
        }
    }

    private Integer parseNullableId(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }

        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private int parseId(String s) {
        if (s == null || s.isBlank()) {
            return -1;
        }

        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return -1;
        }
    }
}