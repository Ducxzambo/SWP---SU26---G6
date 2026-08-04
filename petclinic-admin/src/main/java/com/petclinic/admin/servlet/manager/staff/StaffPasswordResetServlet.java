package com.petclinic.admin.servlet.manager.staff;

import com.petclinic.backend.model.Staff;
import com.petclinic.backend.service.StaffService;
import com.petclinic.backend.util.StaffAuthUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/admin/staff/reset-password")
public class StaffPasswordResetServlet extends HttpServlet {

    private final StaffService staffService = new StaffService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");

        Staff manager = StaffAuthUtil.requireManager(req, resp);
        if (manager == null) return;

        int staffID = Integer.parseInt(req.getParameter("staffID"));
        try {
            String newPassword = req.getParameter("newPassword");
            String confirmPassword = req.getParameter("confirmPassword");
            staffService.resetPassword(staffID, newPassword, confirmPassword);
            req.getSession().setAttribute("flashSuccess", "Đã đặt lại mật khẩu cho nhân viên.");
        } catch (IllegalArgumentException e) {
            req.getSession().setAttribute("flashError", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            req.getSession().setAttribute("flashError", "Lỗi hệ thống: " + e.getMessage());
        }

        resp.sendRedirect(req.getContextPath() + "/admin/staff/detail?id=" + staffID);
    }
}
