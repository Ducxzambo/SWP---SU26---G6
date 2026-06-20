package com.petclinic.servlet.receptionist;

import com.petclinic.dao.AppointmentDAO;
import com.petclinic.dao.StaffDAO;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * BP-02 Step 1 — Receptionist check-in.
 * <p>
 * GET  /receptionist/checkin               → danh sách Confirmed, lọc ngày + ca
 * POST /receptionist/checkin               → check-in bình thường (Confirmed → Arrived)
 * POST /receptionist/checkin?action=walkin → walk-in (tạo appointment + Arrived ngay)
 */
@WebServlet("/receptionist/checkin")
public class CheckInServlet extends HttpServlet {

    private final ExaminationService examinationService = new ExaminationService();
    private final StaffDAO staffDAO = new StaffDAO();

    // ── GET ───────────────────────────────────────────────────────────────────
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        Staff staff = (session != null) ? (Staff) session.getAttribute("staff") : null;
        if (staff == null || !"Receptionist".equals(staff.getRoleName())) {
            resp.sendRedirect(req.getContextPath() + "/auth/staff/login");
            return;
        }

        // ── Date filter (default = today) ─────────────────────────────────────
        LocalDate filterDate = LocalDate.now();
        String dateParam = req.getParameter("date");
        if (dateParam != null && !dateParam.isBlank()) {
            try {
                filterDate = LocalDate.parse(dateParam);
            } catch (DateTimeParseException ignored) {
            }
        }

        // ── Shift filter (optional) ───────────────────────────────────────────
        String shiftParam = req.getParameter("shift");
        Integer shiftFilter = null;
        if (shiftParam != null && !shiftParam.isBlank()) {
            try {
                shiftFilter = Integer.parseInt(shiftParam);
            } catch (NumberFormatException ignored) {
            }
        }

        try {
            // Load appointments
            String keyword = req.getParameter("q");
            List<Appointment> appointments = (keyword != null && !keyword.isBlank())
                    ? examinationService.searchForCheckIn(keyword, filterDate)
                    : examinationService.getConfirmedByDate(filterDate, shiftFilter);

            // Load vets for walk-in dropdown
            List<Staff> vets = staffDAO.findAllVets();

            // Slot info for today
            boolean isToday = filterDate.equals(LocalDate.now());
            int currentSlotCount = isToday ? examinationService.getCurrentShiftCount() : 0;
            boolean currentSlotFull = currentSlotCount >= AppointmentDAO.MAX_PER_SHIFT;
            int currentShift = AppointmentDAO.shiftOf(LocalTime.now());
            String currentShiftLabel = (currentShift > 0)
                    ? AppointmentDAO.shiftLabel(currentShift) : "Ngoài giờ làm việc";

            // Set attributes
            req.setAttribute("appointments", appointments);
            req.setAttribute("keyword", keyword);
            req.setAttribute("filterDate", filterDate.toString());
            req.setAttribute("shiftFilter", shiftFilter != null ? shiftFilter.toString() : "");
            req.setAttribute("isToday", isToday);
            req.setAttribute("vets", vets);
            req.setAttribute("currentSlotCount", currentSlotCount);
            req.setAttribute("currentSlotFull", currentSlotFull);
            req.setAttribute("currentShiftLabel", currentShiftLabel);

            req.getRequestDispatcher("/WEB-INF/views/receptionist/checkin.jsp").forward(req, resp);

        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Lỗi hệ thống khi tải danh sách lịch hẹn.");
            req.getRequestDispatcher("/WEB-INF/views/receptionist/checkin.jsp").forward(req, resp);
        }
    }

    // ── POST ──────────────────────────────────────────────────────────────────
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

        String action = req.getParameter("action");
        if ("walkin".equals(action)) {
            handleWalkIn(req, resp, session);
            return;
        }

        // ── Normal check-in ───────────────────────────────────────────────────
        String appointmentIdStr = req.getParameter("appointmentID");
        String vetIdStr = req.getParameter("vetID");

        if (appointmentIdStr == null || appointmentIdStr.isBlank()) {
            session.setAttribute("flashError", "Thiếu mã lịch hẹn.");
            resp.sendRedirect(req.getContextPath() + "/receptionist/checkin");
            return;
        }

        try {
            int appointmentID = Integer.parseInt(appointmentIdStr);
            Integer vetID = (vetIdStr != null && !vetIdStr.isBlank())
                    ? Integer.parseInt(vetIdStr) : null;

            CheckInResult result = examinationService.checkIn(appointmentID, vetID);
            switch (result) {
                case SUCCESS -> session.setAttribute("flashSuccess", "Check-in thành công!");
                case ALREADY_CHECKED_IN ->
                        session.setAttribute("flashWarning", "Thú cưng này đã được check-in trước đó.");
                case WRONG_STATUS -> session.setAttribute("flashWarning",
                        "Lịch hẹn không ở trạng thái Confirmed, không thể check-in.");
                default -> session.setAttribute("flashError", "Không tìm thấy lịch hẹn.");
            }
        } catch (NumberFormatException e) {
            session.setAttribute("flashError", "ID lịch hẹn không hợp lệ.");
        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("flashError", "Lỗi hệ thống, vui lòng thử lại.");
        }

        resp.sendRedirect(req.getContextPath() + "/receptionist/checkin");
    }

    // ── Walk-in ───────────────────────────────────────────────────────────────
    private void handleWalkIn(HttpServletRequest req, HttpServletResponse resp,
                              HttpSession session) throws IOException {
        String customerIdStr = req.getParameter("customerID");
        String petIdStr = req.getParameter("petID");
        String serviceIdStr = req.getParameter("serviceID");
        String vetIdStr = req.getParameter("vetID");

        if (isBlank(customerIdStr) || isBlank(petIdStr)
                || isBlank(serviceIdStr) || isBlank(vetIdStr)) {
            session.setAttribute("flashError",
                    "Walk-in thất bại: vui lòng điền đầy đủ thông tin.");
            resp.sendRedirect(req.getContextPath() + "/receptionist/checkin");
            return;
        }

        try {
            int apptID = examinationService.createWalkIn(
                    Integer.parseInt(customerIdStr),
                    Integer.parseInt(petIdStr),
                    Integer.parseInt(serviceIdStr),
                    Integer.parseInt(vetIdStr));

            if (apptID == -1) {
                session.setAttribute("flashError",
                        "Ca hiện tại đã đủ 10 thú cưng, không thể nhận thêm khách vãng lai.");
            } else {
                session.setAttribute("flashSuccess",
                        "Walk-in thành công! Lịch khám #" + apptID
                                + " đã tạo và chuyển vào hàng chờ bác sĩ.");
            }
        } catch (NumberFormatException e) {
            session.setAttribute("flashError", "Thông tin walk-in không hợp lệ.");
        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("flashError", "Lỗi hệ thống: " + e.getMessage());
        }

        resp.sendRedirect(req.getContextPath() + "/receptionist/checkin");
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
