package com.petclinic.servlet.receptionist;

import com.petclinic.dao.AppointmentDAO;
import com.petclinic.dao.StaffDAO;
import com.petclinic.model.Appointment;
import com.petclinic.model.Staff;
import com.petclinic.service.GroomingService;
import com.petclinic.service.GroomingService.CheckInResult;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * BP-03 Step 1 — Receptionist grooming check-in.
 *
 * GET  /receptionist/grooming-checkin          → list Confirmed grooming appointments
 * POST /receptionist/grooming-checkin          → check-in (Confirmed → Arrived)
 */
@WebServlet("/receptionist/grooming-checkin")
public class GroomingCheckInServlet extends HttpServlet {

    private final GroomingService groomingService = new GroomingService();
    private final StaffDAO        staffDAO        = new StaffDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        Staff staff = (session != null) ? (Staff) session.getAttribute("staff") : null;
        if (staff == null || !"Receptionist".equals(staff.getRoleName())) {
            resp.sendRedirect(req.getContextPath() + "/auth/staff/login");
            return;
        }

        LocalDate filterDate = parseDate(req.getParameter("date"));
        String shiftParam    = req.getParameter("shift");
        Integer shiftFilter  = parseShift(shiftParam);

        try {
            // Reuse AppointmentDAO but filter by Grooming category
            List<Appointment> appointments = loadGroomingConfirmed(filterDate, shiftFilter);
            List<Staff> groomers = staffDAO.findAllGroomers();

            boolean isToday = filterDate.equals(LocalDate.now());
            int currentShift = AppointmentDAO.shiftOf(LocalTime.now());

            req.setAttribute("appointments",       appointments);
            req.setAttribute("filterDate",         filterDate.toString());
            req.setAttribute("shiftFilter",        shiftFilter != null ? shiftFilter.toString() : "");
            req.setAttribute("isToday",            isToday);
            req.setAttribute("groomers",           groomers);
            req.setAttribute("currentShiftLabel",  currentShift > 0
                    ? AppointmentDAO.shiftLabel(currentShift) : "Ngoài giờ");

            req.getRequestDispatcher("/WEB-INF/views/receptionist/grooming-checkin.jsp")
                    .forward(req, resp);

        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Lỗi hệ thống.");
            req.getRequestDispatcher("/WEB-INF/views/receptionist/grooming-checkin.jsp")
                    .forward(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        HttpSession session = req.getSession(false);
        Staff staff = (session != null) ? (Staff) session.getAttribute("staff") : null;
        if (staff == null || !"Receptionist".equals(staff.getRoleName())) {
            resp.sendRedirect(req.getContextPath() + "/auth/staff/login");
            return;
        }

        String apptIdStr   = req.getParameter("appointmentID");
        String groomerIdStr = req.getParameter("groomerID");

        if (apptIdStr == null || apptIdStr.isBlank()) {
            session.setAttribute("flashError", "Thiếu mã lịch hẹn.");
            resp.sendRedirect(req.getContextPath() + "/receptionist/grooming-checkin");
            return;
        }

        try {
            int appointmentID = Integer.parseInt(apptIdStr);
            Integer groomerID = (groomerIdStr != null && !groomerIdStr.isBlank())
                    ? Integer.parseInt(groomerIdStr) : null;

            CheckInResult result = groomingService.checkIn(appointmentID, groomerID);
            switch (result) {
                case SUCCESS ->
                        session.setAttribute("flashSuccess", "Check-in grooming thành công!");
                case ALREADY_CHECKED_IN ->
                        session.setAttribute("flashWarning", "Thú cưng này đã được check-in trước đó.");
                case WRONG_STATUS ->
                        session.setAttribute("flashWarning", "Lịch hẹn không ở trạng thái Confirmed.");
                default ->
                        session.setAttribute("flashError", "Không tìm thấy lịch hẹn.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("flashError", "Lỗi hệ thống: " + e.getMessage());
        }
        resp.sendRedirect(req.getContextPath() + "/receptionist/grooming-checkin");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<Appointment> loadGroomingConfirmed(LocalDate date, Integer shift)
            throws java.sql.SQLException {
        // Uses AppointmentDAO with grooming-specific SQL
        com.petclinic.dao.AppointmentDAO dao = new com.petclinic.dao.AppointmentDAO();
        return dao.findGroomingConfirmedByDate(date, shift);
    }

    private LocalDate parseDate(String p) {
        if (p == null || p.isBlank()) return LocalDate.now();
        try { return LocalDate.parse(p); } catch (DateTimeParseException e) { return LocalDate.now(); }
    }

    private Integer parseShift(String p) {
        if (p == null || p.isBlank()) return null;
        try { return Integer.parseInt(p); } catch (NumberFormatException e) { return null; }
    }
}