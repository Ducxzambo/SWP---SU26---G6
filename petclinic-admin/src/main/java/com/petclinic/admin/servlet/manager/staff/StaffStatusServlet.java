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

@WebServlet("/admin/staff/status")
public class StaffStatusServlet extends HttpServlet {

    private final StaffService staffService = new StaffService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");

        Staff manager = StaffAuthUtil.requireManager(req, resp);
        if (manager == null) return;

        try {
            int staffID = Integer.parseInt(req.getParameter("staffID"));
            boolean active = "true".equals(req.getParameter("active"));
            staffService.setActive(staffID, active, manager.getStaffID());
            req.getSession().setAttribute("flashSuccess",
                    active ? "Đã kích hoạt lại nhân viên." : "Đã vô hiệu hoá nhân viên.");
        } catch (IllegalArgumentException e) {
            req.getSession().setAttribute("flashError", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            req.getSession().setAttribute("flashError", "Lỗi hệ thống: " + e.getMessage());
        }

        resp.sendRedirect(req.getContextPath() + safeRedirect(req));
    }

    private String safeRedirect(HttpServletRequest req) {
        String target = req.getParameter("redirectTo");
        if (target != null && target.startsWith("/admin/staff")) {
            return target;
        }
        return "/admin/staff";
    }
}
