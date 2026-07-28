package com.petclinic.admin.servlet.manager.refunds;

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
 * Refund &gt; Xác nhận đã chuyển khoản (POST-only action, no screen of its own).
 * Bước 2 của luồng xác nhận: bước 1 (hiện QR) chỉ là hiển thị ở detail.jsp,
 * KHÔNG đổi state gì - chỉ khi staff bấm nút này (sau khi đã tự chuyển
 * khoản bằng app ngân hàng) thì Refund/Invoice mới thực sự được cập nhật.
 */
@WebServlet("/manager/refunds/process")
public class RefundProcessServlet extends HttpServlet {

    private final RefundService refundService = new RefundService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");

        Staff manager = StaffAuthUtil.requireManager(req, resp);
        if (manager == null) return;

        int refundId = parseId(req.getParameter("refundId"));
        try {
            refundService.confirmProcessed(refundId, manager.getStaffID());
            req.getSession().setAttribute("flashSuccess",
                    "Đã xác nhận hoàn tiền và gửi email thông báo cho khách hàng.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            req.getSession().setAttribute("flashError", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            req.getSession().setAttribute("flashError", "Lỗi hệ thống: " + e.getMessage());
        }

        resp.sendRedirect(req.getContextPath() + "/manager/refunds/detail?id=" + refundId);
    }

    private int parseId(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return -1;
        }
    }
}
