package com.petclinic.admin.servlet.manager.staff;

import com.petclinic.backend.model.Role;
import com.petclinic.backend.model.Staff;
import com.petclinic.backend.dto.StaffPerformance;
import com.petclinic.backend.service.StaffService;
import com.petclinic.backend.util.StaffAuthUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Staff &gt; Thống kê hiệu suất (statistics screen).
 * Compares every active staff member's completed/cancelled/no-show case
 * counts and revenue over a date range, optionally scoped to one role -
 * this is the "phân tích" screen; StaffDetailServlet shows the same shape
 * of data but scoped to one person, all-time.
 */
@WebServlet("/admin/staff/statistics")
public class StaffStatisticsServlet extends HttpServlet {

    private final StaffService staffService = new StaffService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Staff manager = StaffAuthUtil.requireManager(req, resp);
        if (manager == null) return;

        if ("export".equals(req.getParameter("action"))) {
            exportStatistics(req, resp);
            return;
        }

        try {
            LocalDate fromDate = parseDate(req.getParameter("fromDate"));
            LocalDate toDate = parseDate(req.getParameter("toDate"));
            String role = req.getParameter("role");

            List<StaffPerformance> performance = staffService.getPerformance(fromDate, toDate, role);
            List<Role> roles = staffService.getAllRoles();

            int maxCases = performance.stream()
                    .mapToInt(StaffPerformance::getCompletedCases)
                    .max().orElse(0);
            int totalCompleted = performance.stream()
                    .mapToInt(StaffPerformance::getCompletedCases)
                    .sum();
            BigDecimal totalRevenue = performance.stream()
                    .map(StaffPerformance::getRevenue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            req.setAttribute("performance", performance);
            req.setAttribute("roles", roles);
            req.setAttribute("maxCases", Math.max(maxCases, 1));
            req.setAttribute("totalCompleted", totalCompleted);
            req.setAttribute("totalRevenue", totalRevenue);
            req.setAttribute("staffCount", performance.size());
            req.setAttribute("fromDate", req.getParameter("fromDate"));
            req.setAttribute("toDate", req.getParameter("toDate"));
            req.setAttribute("role", role);

            req.getRequestDispatcher("/WEB-INF/views/manager/staff/statistics.jsp").forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Không tải được thống kê: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/manager/staff/statistics.jsp").forward(req, resp);
        }
    }

    private void exportStatistics(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            LocalDate fromDate = parseDate(req.getParameter("fromDate"));
            LocalDate toDate = parseDate(req.getParameter("toDate"));
            String role = req.getParameter("role");
            List<StaffPerformance> rows = staffService.getPerformance(fromDate, toDate, role);

            resp.setContentType("text/csv;charset=UTF-8");
            resp.setHeader("Content-Disposition", "attachment; filename=\"staff-performance.csv\"");

            try (PrintWriter out = resp.getWriter()) {
                out.write("\uFEFF");
                out.println("Staff Name,Role,Completed Cases,Cancelled Cases,No-Show Cases,"
                        + "Completion Rate (%),Revenue");
                for (StaffPerformance p : rows) {
                    out.println(csv(p.getFullName()) + ","
                            + csv(p.getRoleName()) + ","
                            + p.getCompletedCases() + ","
                            + p.getCancelledCases() + ","
                            + p.getNoShowCases() + ","
                            + String.format("%.1f", p.getCompletionRate()) + ","
                            + p.getRevenue());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Không xuất được thống kê.");
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        return LocalDate.parse(value);
    }

    private String csv(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
