package com.petclinic.admin.servlet.manager.refunds;

import com.petclinic.backend.dao.AppointmentDAO;
import com.petclinic.backend.model.Appointment;
import com.petclinic.backend.model.Invoice;
import com.petclinic.backend.model.Refund;
import com.petclinic.backend.model.Staff;
import com.petclinic.backend.service.RefundService;
import com.petclinic.backend.util.StaffAuthUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Refund &gt; Chi tiết yêu cầu hoàn tiền.
 * Hiển thị Refund + Invoice/InvoiceItems/Payments của appointment tương ứng.
 * Với refund đang Requested, còn hiện thêm ảnh VietQR + 2 action (xác nhận/
 * từ chối) - các action này POST sang RefundProcessServlet/RefundRejectServlet
 * riêng, servlet này chỉ hiển thị.
 */
@WebServlet("/manager/refunds/detail")
public class RefundDetailServlet extends HttpServlet {

    private final RefundService refundService = new RefundService();
    private final AppointmentDAO appointmentDAO = new AppointmentDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Staff manager = StaffAuthUtil.requireManager(req, resp);
        if (manager == null) return;

        try {
            int refundId = Integer.parseInt(req.getParameter("id"));
            Refund refund = refundService.getById(refundId);
            if (refund == null) {
                req.setAttribute("error", "Không tìm thấy yêu cầu hoàn tiền.");
                req.getRequestDispatcher("/WEB-INF/views/manager/refunds/list.jsp").forward(req, resp);
                return;
            }

            Invoice invoice = refundService.getInvoiceForRefund(refund);
            Appointment appt = appointmentDAO.findById(refund.getAppointmentID());
            String vietQrUrl = refund.isRequested() ? refundService.buildVietQrUrl(refund) : null;

            req.setAttribute("refund", refund);
            req.setAttribute("invoice", invoice);
            req.setAttribute("appt", appt);
            req.setAttribute("vietQrUrl", vietQrUrl);

            req.getRequestDispatcher("/WEB-INF/views/manager/refunds/detail.jsp").forward(req, resp);
        } catch (NumberFormatException e) {
            resp.sendRedirect(req.getContextPath() + "/manager/refunds");
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Không tải được chi tiết yêu cầu hoàn tiền: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/manager/refunds/list.jsp").forward(req, resp);
        }
    }
}
