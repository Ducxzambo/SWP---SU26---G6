package com.petclinic.admin.servlet.manager.refunds;

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
import java.util.List;

/**
 * Refund &gt; Yêu cầu hoàn tiền (list screen).
 * Requested luôn hiển thị trước (hàng đợi cần xử lý) - xem RefundDAO.search().
 */
@WebServlet("/manager/refunds")
public class RefundListServlet extends HttpServlet {

    private final RefundService refundService = new RefundService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Staff manager = StaffAuthUtil.requireManager(req, resp);
        if (manager == null) return;

        try {
            String status = req.getParameter("status");
            String sort = req.getParameter("sort");

            List<Refund> refunds = refundService.listRefunds(status, sort);
            long requestedCount = refunds.stream().filter(Refund::isRequested).count();

            req.setAttribute("refunds", refunds);
            req.setAttribute("requestedCount", requestedCount);
            req.setAttribute("status", status);
            req.setAttribute("sort", sort);

            req.getRequestDispatcher("/WEB-INF/views/manager/refunds/list.jsp").forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Không tải được danh sách yêu cầu hoàn tiền: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/manager/refunds/list.jsp").forward(req, resp);
        }
    }
}
