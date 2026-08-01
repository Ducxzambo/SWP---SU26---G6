package com.petclinic.admin.servlet.manager.staff;

import com.petclinic.backend.model.Role;
import com.petclinic.backend.model.Staff;
import com.petclinic.backend.service.StaffService;
import com.petclinic.backend.util.StaffAuthUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@WebServlet("/manager/staff/edit")
public class StaffEditServlet extends HttpServlet {

    private final StaffService staffService = new StaffService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Staff manager = StaffAuthUtil.requireManager(req, resp);
        if (manager == null) return;

        try {
            String idParam = req.getParameter("id");
            Staff staff = null;
            if (idParam != null && !idParam.isBlank()) {
                staff = staffService.getForManagement(Integer.parseInt(idParam));
                if (staff == null) {
                    resp.sendRedirect(req.getContextPath() + "/manager/staff");
                    return;
                }
            }

            List<Role> roles = staffService.getAllRoles();
            req.setAttribute("staff", staff);
            req.setAttribute("roles", roles);
            req.setAttribute("isNew", staff == null);

            req.getRequestDispatcher("/WEB-INF/views/manager/staff/edit.jsp").forward(req, resp);
        } catch (NumberFormatException e) {
            resp.sendRedirect(req.getContextPath() + "/manager/staff");
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Không tải được biểu mẫu: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/manager/staff/edit.jsp").forward(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");

        Staff manager = StaffAuthUtil.requireManager(req, resp);
        if (manager == null) return;

        String idParam = req.getParameter("staffID");
        boolean isNew = idParam == null || idParam.isBlank();

        Staff staff = new Staff();
        if (!isNew) staff.setStaffID(Integer.parseInt(idParam));
        staff.setFullName(trim(req.getParameter("fullName")));
        staff.setEmail(trim(req.getParameter("email")));
        staff.setPhone(trim(req.getParameter("phone")));
        staff.setRoleID(parseIntOrZero(req.getParameter("roleID")));
        staff.setSpecialization(trim(req.getParameter("specialization")));
        staff.setLicenseNumber(trim(req.getParameter("licenseNumber")));
        String hireDateParam = req.getParameter("hireDate");
        if (hireDateParam != null && !hireDateParam.isBlank()) {
            staff.setHireDate(LocalDate.parse(hireDateParam));
        }

        try {
            if (isNew) {
                String password = req.getParameter("password");
                int newID = staffService.createStaff(staff, password);
                req.getSession().setAttribute("flashSuccess", "Đã thêm nhân viên mới.");
                resp.sendRedirect(req.getContextPath() + "/manager/staff/detail?id=" + newID);
            } else {
                staffService.updateStaff(staff);
                req.getSession().setAttribute("flashSuccess", "Đã cập nhật thông tin nhân viên.");
                resp.sendRedirect(req.getContextPath() + "/manager/staff/detail?id=" + staff.getStaffID());
            }
        } catch (IllegalArgumentException e) {
            renderFormError(req, resp, staff, isNew, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            renderFormError(req, resp, staff, isNew, "Lỗi hệ thống: " + e.getMessage());
        }
    }

    private void renderFormError(HttpServletRequest req, HttpServletResponse resp,
                                 Staff staff, boolean isNew, String message)
            throws ServletException, IOException {
        try {
            req.setAttribute("error", message);
            req.setAttribute("staff", staff);
            req.setAttribute("isNew", isNew);
            req.setAttribute("roles", staffService.getAllRoles());
            req.getRequestDispatcher("/WEB-INF/views/manager/staff/edit.jsp").forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect(req.getContextPath() + "/manager/staff");
        }
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private int parseIntOrZero(String value) {
        try {
            return value == null || value.isBlank() ? 0 : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
