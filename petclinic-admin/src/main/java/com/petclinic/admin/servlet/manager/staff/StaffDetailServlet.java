package com.petclinic.admin.servlet.manager.staff;

import com.petclinic.backend.model.Staff;
import com.petclinic.backend.dto.StaffPerformance;
import com.petclinic.backend.dto.StaffServiceBreakdown;
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
 * Staff &gt; Hồ sơ nhân viên (detail screen).
 * Read-only profile view plus an all-time performance snapshot (case counts,
 * revenue, top services). Editing and admin actions (deactivate, reset
 * password) live on their own screens/endpoints - this page only links out
 * to them, it doesn't process any of those POSTs itself.
 */
@WebServlet("/admin/staff/detail")
public class StaffDetailServlet extends HttpServlet {

    private final StaffService staffService = new StaffService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Staff manager = StaffAuthUtil.requireManager(req, resp);
        if (manager == null) return;

        try {
            int staffID = Integer.parseInt(req.getParameter("id"));
            Staff staff = staffService.getForManagement(staffID);
            if (staff == null) {
                req.setAttribute("error", "Không tìm thấy nhân viên.");
                req.getRequestDispatcher("/WEB-INF/views/manager/staff/list.jsp").forward(req, resp);
                return;
            }

            StaffPerformance performance = staffService.getPerformanceForStaff(staffID);
            List<StaffServiceBreakdown> breakdown = staffService.getServiceBreakdown(staffID);

            req.setAttribute("staff", staff);
            req.setAttribute("performance", performance);
            req.setAttribute("breakdown", breakdown);
            req.setAttribute("isSelf", staffID == manager.getStaffID());

            req.getRequestDispatcher("/WEB-INF/views/manager/staff/detail.jsp").forward(req, resp);
        } catch (NumberFormatException e) {
            resp.sendRedirect(req.getContextPath() + "/admin/staff");
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Không tải được hồ sơ nhân viên: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/manager/staff/list.jsp").forward(req, resp);
        }
    }
}
