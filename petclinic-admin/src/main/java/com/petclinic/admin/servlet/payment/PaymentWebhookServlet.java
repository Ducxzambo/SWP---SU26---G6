package com.petclinic.admin.servlet.payment;

import com.petclinic.backend.dao.InvoiceDAO;
import com.petclinic.backend.model.Invoice;
import com.petclinic.backend.model.Staff;
import com.petclinic.backend.service.AssignmentService;
import com.petclinic.backend.service.PaymentService;

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

/**
 * PayOS payment callback - CHỈ phục vụ luồng LỄ TÂN thu tiền hóa đơn tại quầy
 * (nút "Chuyển khoản" ở trang tổng hợp hóa đơn).
 *
 * POST /payment/webhook – PayOS async webhook (server-to-server)
 * GET  /payment/result  – Return URL sau khi quét QR chuyển khoản xong
 */
@WebServlet(urlPatterns = {
        "/payment/webhook",
        "/payment/result"
})
public class PaymentWebhookServlet extends HttpServlet {

    private static final Logger LOG =
            Logger.getLogger(PaymentWebhookServlet.class.getName());

    private final PaymentService paymentSvc = new PaymentService();
    private final InvoiceDAO invoiceDAO = new InvoiceDAO();
    private final AssignmentService assignmentSvc = new AssignmentService();

    // =========================================================================
    // POST /payment/webhook
    // PayOS server-to-server (nguồn xác nhận CHÍNH THỨC)
    // =========================================================================
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

    // =========================================================================
    // GET /payment/result?source=staff:{checkin|history}
    //
    // Lễ tân
    //
    // amount thanh toán KHÔNG suy ra từ deposit/full cố định, mà đọc trực tiếp
    // từ param "amount" được đính kèm khi tạo Payment Link
    // (PayOSClient.createPaymentLink).
    //
    // Số tiền lễ tân thu luôn là "số còn phải thu" tại thời điểm bấm nút,
    // có thể thay đổi sau khi bác sĩ/groomer phát sinh thêm chi phí.
    // =========================================================================
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

                    // Idempotent theo số tiền còn phải thu thực tế,
                    // không theo Invoice Status.
                    //
                    // Invoice có thể đã PrePaid từ lần thu trước nhưng
                    // sau đó phát sinh thêm chi phí.
                    BigDecimal amountPaid = invoiceDAO.getAmountPaid(invoiceId);
                    BigDecimal amountDue = invoice.getTotalAmount().subtract(amountPaid);

                    if (amountDue.compareTo(BigDecimal.ZERO) > 0) {
                        int attributedStaffId = (staffIdParam != null) ? staffIdParam : staff.getStaffID();
                        invoiceDAO.insertPayment(invoiceId, BigDecimal.valueOf(amount), "BankTransfer", attributedStaffId);
                        assignmentSvc.autoAssign(invoice.getAppointmentID());
                    }
                }

                session.setAttribute("flashSuccess", "Thanh toán chuyển khoản thành công!");

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

        resp.sendRedirect(req.getContextPath()  + "/receptionist/invoice?invoiceId="
                        + invoiceId + "&from=" + from);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

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