package com.petclinic.servlet.receptionist;

import com.petclinic.model.Appointment;
import com.petclinic.model.Staff;
import com.petclinic.service.ExaminationService;
import com.petclinic.service.ExaminationService.CheckInResult;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

/**
 * BP-02 Step 1 — Receptionist check-in.
 * <p>
 * GET  /receptionist/checkin          → show today's Confirmed appointments
 * GET  /receptionist/checkin?q=<kw>  → search by owner or pet name
 * POST /receptionist/checkin          → perform check-in (update status Confirmed → Arrived)
 */
@WebServlet("/receptionist/checkin")
public class CheckInServlet extends HttpServlet {

    private final ExaminationService examinationService = new ExaminationService();

    // ── GET: show check-in dashboard ──────────────────────────────────────────
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Auth guard — receptionist only
        HttpSession session = req.getSession(false);
        Staff staff = (session != null) ? (Staff) session.getAttribute("staff") : null;
        if (staff == null || !"Receptionist".equals(staff.getRoleName())) {
            resp.sendRedirect(req.getContextPath() + "/auth/staff/login");
            return;
        }

        try {
            String keyword = req.getParameter("q");
            List<Appointment> appointments = (keyword != null && !keyword.isBlank())
                    ? examinationService.searchForCheckIn(keyword)
                    : examinationService.getTodayConfirmedAppointments();

            req.setAttribute("appointments", appointments);
            req.setAttribute("keyword", keyword);
            req.getRequestDispatcher("/WEB-INF/views/receptionist/checkin.jsp")
                    .forward(req, resp);

        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Lỗi hệ thống khi tải danh sách lịch hẹn.");
            req.getRequestDispatcher("/WEB-INF/views/receptionist/checkin.jsp")
                    .forward(req, resp);
        }
    }

    // ── POST: perform check-in ────────────────────────────────────────────────
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");

        // Auth guard
        HttpSession session = req.getSession(false);
        Staff staff = (session != null) ? (Staff) session.getAttribute("staff") : null;
        if (staff == null || !"Receptionist".equals(staff.getRoleName())) {
            resp.sendRedirect(req.getContextPath() + "/auth/staff/login");
            return;
        }

        String appointmentIdStr = req.getParameter("appointmentID");
        String vetIdStr = req.getParameter("vetID");   // optional reassign

        if (appointmentIdStr == null || appointmentIdStr.isBlank()) {
            resp.sendRedirect(req.getContextPath() + "/receptionist/checkin?error=missing_id");
            return;
        }

        try {
            int appointmentID = Integer.parseInt(appointmentIdStr);
            Integer vetID = (vetIdStr != null && !vetIdStr.isBlank())
                    ? Integer.parseInt(vetIdStr) : null;

            CheckInResult result = examinationService.checkIn(appointmentID, vetID);

            switch (result) {
                case SUCCESS:
                    // Redirect back with flash success
                    req.getSession().setAttribute("flashSuccess",
                            "Check-in thành công! Bác sĩ sẽ nhận thông báo ngay.");
                    resp.sendRedirect(req.getContextPath() + "/receptionist/checkin");
                    break;
                case ALREADY_CHECKED_IN:
                    req.getSession().setAttribute("flashWarning", "Thú cưng này đã được check-in trước đó.");
                    resp.sendRedirect(req.getContextPath() + "/receptionist/checkin");
                    break;
                case WRONG_STATUS:
                    req.getSession().setAttribute("flashWarning",
                            "Lịch hẹn không ở trạng thái Confirmed, không thể check-in.");
                    resp.sendRedirect(req.getContextPath() + "/receptionist/checkin");
                    break;
                case NOT_FOUND:
                default:
                    req.getSession().setAttribute("flashError", "Không tìm thấy lịch hẹn.");
                    resp.sendRedirect(req.getContextPath() + "/receptionist/checkin");
            }

        } catch (NumberFormatException e) {
            resp.sendRedirect(req.getContextPath() + "/receptionist/checkin?error=invalid_id");
        } catch (Exception e) {
            e.printStackTrace();
            req.getSession().setAttribute("flashError", "Lỗi hệ thống, vui lòng thử lại.");
            resp.sendRedirect(req.getContextPath() + "/receptionist/checkin");
        }
    }
}