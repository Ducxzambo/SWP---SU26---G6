package com.petclinic.admin.servlet.manager.refunds;

import com.petclinic.backend.model.Appointment;
import com.petclinic.backend.model.Invoice;
import com.petclinic.backend.model.Staff;
import com.petclinic.backend.service.RefundService;
import com.petclinic.backend.util.StaffAuthUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@WebServlet("/manager/refunds/create")
public class RefundCreateServlet extends HttpServlet {

    private final RefundService refundService = new RefundService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Staff manager = StaffAuthUtil.requireManager(req, resp);
        if (manager == null) return;

        try {
            String apptIdParam = req.getParameter("appointmentId");
            if (apptIdParam == null || apptIdParam.isBlank()) {
                showPicker(req, resp);
            } else {
                showForm(req, resp, Integer.parseInt(apptIdParam), null);
            }
        } catch (NumberFormatException e) {
            resp.sendRedirect(req.getContextPath() + "/manager/refunds/create");
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/manager/refunds/create-picker.jsp").forward(req, resp);
        }
    }

    private void showPicker(HttpServletRequest req, HttpServletResponse resp)
            throws Exception {
        String keyword = req.getParameter("q");
        String status = req.getParameter("status");
        List<Appointment> appointments = refundService.getEligibleAppointments(keyword, status);
        req.setAttribute("appointments", appointments);
        req.setAttribute("keyword", keyword);
        req.setAttribute("status", status);
        req.getRequestDispatcher("/WEB-INF/views/manager/refunds/create-picker.jsp").forward(req, resp);
    }

    private void showForm(HttpServletRequest req, HttpServletResponse resp, int appointmentId,
                          String formError) throws Exception {
        Appointment appt = refundService.getEligibleAppointment(appointmentId);
        if (appt == null) {
            req.getSession().setAttribute("flashError",
                    "Lịch hẹn không hợp lệ hoặc không ở trạng thái có thể hoàn tiền.");
            resp.sendRedirect(req.getContextPath() + "/manager/refunds/create");
            return;
        }
        Invoice invoice = refundService.getInvoiceForAppointment(appointmentId);

        req.setAttribute("appt", appt);
        req.setAttribute("invoice", invoice);
        if (formError != null) req.setAttribute("error", formError);
        req.getRequestDispatcher("/WEB-INF/views/manager/refunds/create-form.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");

        Staff manager = StaffAuthUtil.requireManager(req, resp);
        if (manager == null) return;

        int appointmentId = parseId(req.getParameter("appointmentId"));
        try {
            String reason = req.getParameter("reason");
            boolean fullRefund = "true".equals(req.getParameter("fullRefund"));
            BigDecimal customAmount = parseAmount(req.getParameter("amount"));
            String bankCode = req.getParameter("bankCode");
            String accountNumber = req.getParameter("accountNumber");
            String accountName = req.getParameter("accountName");

            int refundId = refundService.createManualRequest(appointmentId, reason, customAmount,
                    fullRefund, bankCode, accountNumber, accountName);

            req.getSession().setAttribute("flashSuccess",
                    "Đã tạo yêu cầu hoàn tiền. Tiếp tục xác nhận chuyển khoản bên dưới.");
            resp.sendRedirect(req.getContextPath() + "/manager/refunds/detail?id=" + refundId);
        } catch (IllegalArgumentException e) {
            try {
                showForm(req, resp, appointmentId, e.getMessage());
            } catch (Exception ex) {
                ex.printStackTrace();
                resp.sendRedirect(req.getContextPath() + "/manager/refunds/create");
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                showForm(req, resp, appointmentId, "Lỗi hệ thống: " + e.getMessage());
            } catch (Exception ex) {
                ex.printStackTrace();
                resp.sendRedirect(req.getContextPath() + "/manager/refunds/create");
            }
        }
    }

    private int parseId(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return -1;
        }
    }

    private BigDecimal parseAmount(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return new BigDecimal(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
