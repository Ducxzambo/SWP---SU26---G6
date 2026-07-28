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
import java.util.List;

/**
 * Staff &gt; Danh sách nhân viên (list/search screen).
 * Read-only browse of all staff (active + inactive) with keyword/role/status
 * filters. Row actions link out to Detail; deactivate/reactivate is a quick
 * inline POST that preserves the current filters on redirect.
 */
@WebServlet("/manager/staff")
public class StaffListServlet extends HttpServlet {

    private final StaffService staffService = new StaffService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Staff manager = StaffAuthUtil.requireManager(req, resp);
        if (manager == null) return;

        try {
            String keyword = req.getParameter("q");
            String role = req.getParameter("role");
            String status = req.getParameter("status");

            List<Staff> staffList = staffService.search(keyword, role, status);
            List<Role> roles = staffService.getAllRoles();

            req.setAttribute("staffList", staffList);
            req.setAttribute("roles", roles);
            req.setAttribute("keyword", keyword);
            req.setAttribute("role", role);
            req.setAttribute("status", status);

            req.getRequestDispatcher("/WEB-INF/views/manager/staff/list.jsp").forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Không tải được danh sách nhân viên: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/manager/staff/list.jsp").forward(req, resp);
        }
    }
}
