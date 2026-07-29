package com.petclinic.admin.servlet.receptionist;

import com.petclinic.backend.dao.AppointmentDAO;
import com.petclinic.backend.dao.InvoiceDAO;
import com.petclinic.backend.model.Appointment;
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

/**
 *  1. Ngay sau khi "Tạo & Check-in" walk-in thành công (CheckInServlet) -
 *     amount phải thu = TOÀN BỘ TotalAmount (chưa thu gì).
 *  2. Ngay sau khi xác nhận "Hoàn tất" ở Lịch sử (AppointmentHistoryServlet) -
 *     amount phải thu = TotalAmount hiện tại (có thể đã lớn hơn do vet/groomer
 *     phát sinh thêm chi phí thuốc/xét nghiệm - xem InvoiceSyncService) TRỪ
 *     đi phần đã thu từ trước (nếu có).
 *
 * GET  /receptionist/invoice?invoiceId=X&from=checkin|history → hiển thị
 * POST /receptionist/invoice  action=cash|bank                → xác nhận thu tiền
 */
@WebServlet("/receptionist/invoice")
public class InvoiceServlet extends HttpServlet {

    private final InvoiceDAO        invoiceDAO        = new InvoiceDAO();
    private final AppointmentDAO    appointmentDAO     = new AppointmentDAO();
    private final PaymentService    paymentService     = new PaymentService();
    private final AssignmentService assignmentService  = new AssignmentService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Staff staff = getAuthenticatedReceptionist(req, resp);
        if (staff == null) return;

        int invoiceId = parseId(req.getParameter("invoiceId"));
        String from = normalizeFrom(req.getParameter("from"));

        if (invoiceId <= 0) {
            resp.sendRedirect(req.getContextPath() + "/receptionist/checkin");
            return;
        }

        try {
            Invoice invoice = invoiceDAO.findById(invoiceId);
            if (invoice == null) {
                req.getSession().setAttribute("flashError", "Không tìm thấy hóa đơn #" + invoiceId + ".");
                resp.sendRedirect(req.getContextPath() + "/receptionist/" + from);
                return;
            }
            Appointment appt = appointmentDAO.findById(invoice.getAppointmentID());

            BigDecimal amountPaid = invoiceDAO.getAmountPaid(invoiceId);
            BigDecimal amountDue  = invoice.getTotalAmount().subtract(amountPaid);
            if (amountDue.compareTo(BigDecimal.ZERO) < 0) amountDue = BigDecimal.ZERO;

            req.setAttribute("invoice",     invoice);
            req.setAttribute("appointment", appt);
            req.setAttribute("amountPaid",  amountPaid);
            req.setAttribute("amountDue",   amountDue);
            req.setAttribute("fullyPaid",   amountDue.compareTo(BigDecimal.ZERO) <= 0);
            req.setAttribute("from",        from);
            req.setAttribute("staff",       staff);

            req.getRequestDispatcher("/WEB-INF/views/receptionist/invoice.jsp").forward(req, resp);

        } catch (Exception e) {
            e.printStackTrace();
            req.getSession().setAttribute("flashError", "Lỗi hệ thống khi tải hóa đơn: " + e.getMessage());
            resp.sendRedirect(req.getContextPath() + "/receptionist/" + from);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        Staff staff = getAuthenticatedReceptionist(req, resp);
        if (staff == null) return;

        int invoiceId   = parseId(req.getParameter("invoiceId"));
        String from     = normalizeFrom(req.getParameter("from"));
        String action   = req.getParameter("action"); // "cash" | "bank"
        HttpSession session = req.getSession();
        String redirectBack = req.getContextPath() + "/receptionist/invoice?invoiceId=" + invoiceId + "&from=" + from;

        if (invoiceId <= 0) {
            session.setAttribute("flashError", "Thiếu mã hóa đơn.");
            resp.sendRedirect(req.getContextPath() + "/receptionist/" + from);
            return;
        }

        try {
            Invoice invoice = invoiceDAO.findById(invoiceId);
            if (invoice == null) {
                session.setAttribute("flashError", "Không tìm thấy hóa đơn #" + invoiceId + ".");
                resp.sendRedirect(req.getContextPath() + "/receptionist/" + from);
                return;
            }

            BigDecimal amountPaid = invoiceDAO.getAmountPaid(invoiceId);
            BigDecimal amountDue  = invoice.getTotalAmount().subtract(amountPaid);

            if (amountDue.compareTo(BigDecimal.ZERO) <= 0) {
                session.setAttribute("flashWarning", "Hóa đơn này đã được thanh toán đủ.");
                resp.sendRedirect(redirectBack);
                return;
            }

            if ("cash".equals(action)) {
                invoiceDAO.insertPayment(invoiceId, amountDue, "Cash", staff.getStaffID());
                assignmentService.autoAssign(invoice.getAppointmentID());
                session.setAttribute("flashSuccess", "Đã ghi nhận thu tiền mặt "
                        + formatVnd(amountDue) + "đ.");
                resp.sendRedirect(redirectBack);

            } else if ("bank".equals(action)) {
                // Tái sử dụng NGUYÊN vẹn PaymentService/PayOSClient của luồng
                // booking online của khách hàng - chỉ khác source="staff:{from}"
                // + staffId để callback (PaymentWebhookServlet) biết quay về
                // đúng trang hóa đơn của lễ tân thay vì trang khách hàng.
                String description = "Invoice " + invoiceId;
                String source = "staff:" + from;
                String checkoutUrl = paymentService.createPaymentLink(
                        invoiceId, invoice.getAppointmentID(), amountDue.longValue(),
                        description, true, source, staff.getStaffID());

                if (checkoutUrl == null || checkoutUrl.isBlank()) {
                    session.setAttribute("flashError", "Không thể tạo liên kết thanh toán PayOS. Vui lòng thử lại.");
                    resp.sendRedirect(redirectBack);
                    return;
                }
                resp.sendRedirect(checkoutUrl);

            } else {
                resp.sendRedirect(redirectBack);
            }

        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("flashError", "Lỗi hệ thống: " + e.getMessage());
            resp.sendRedirect(redirectBack);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String normalizeFrom(String from) {
        return "history".equals(from) ? "history" : "checkin";
    }

    private int parseId(String s) {
        if (s == null || s.isBlank()) return -1;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return -1; }
    }

    private String formatVnd(BigDecimal amount) {
        return String.format("%,.0f", amount).replace(',', '.');
    }

    private Staff getAuthenticatedReceptionist(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/staff/login");
            return null;
        }
        Staff staff = (Staff) session.getAttribute("staff");
        if (staff == null || !"Receptionist".equals(staff.getRoleName())) {
            resp.sendRedirect(req.getContextPath() + "/auth/staff/login");
            return null;
        }
        return staff;
    }
}
