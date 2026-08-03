package com.petclinic.admin.servlet.manager.shift;

import com.petclinic.backend.dao.StaffAttendanceDAO;
import com.petclinic.backend.dto.StaffAttendanceSummary;
import com.petclinic.backend.model.Staff;
import com.petclinic.backend.util.StaffAuthUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@WebServlet("/manager/attendance/statistics")
public class StaffAttendanceStatsServlet extends HttpServlet {

    private final StaffAttendanceDAO attendanceDAO = new StaffAttendanceDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Staff manager = StaffAuthUtil.requireManager(req, resp);
        if (manager == null) return;

        try {
            LocalDate fromDate = parseDate(req.getParameter("fromDate"));
            LocalDate toDate   = parseDate(req.getParameter("toDate"));

            List<StaffAttendanceSummary> summary = attendanceDAO.getAttendanceSummary(fromDate, toDate);
            long alertCount = summary.stream().filter(StaffAttendanceSummary::isAnyAlert).count();

            req.setAttribute("summary", summary);
            req.setAttribute("alertCount", alertCount);
            req.setAttribute("fromDate", req.getParameter("fromDate"));
            req.setAttribute("toDate", req.getParameter("toDate"));
            req.getRequestDispatcher("/WEB-INF/views/manager/shift/attendance-stats.jsp").forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Không tải được thống kê chấm công: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/manager/shift/attendance-stats.jsp").forward(req, resp);
        }
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s); } catch (DateTimeParseException e) { return null; }
    }
}