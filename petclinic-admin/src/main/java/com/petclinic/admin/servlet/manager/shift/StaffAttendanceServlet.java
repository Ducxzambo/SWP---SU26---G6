package com.petclinic.admin.servlet.manager.shift;

import com.petclinic.backend.dao.AppointmentDAO;
import com.petclinic.backend.dao.StaffAttendanceDAO;
import com.petclinic.backend.dao.StaffDAO;
import com.petclinic.backend.model.Staff;
import com.petclinic.backend.model.StaffAttendance;
import com.petclinic.backend.util.StaffAuthUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/manager/attendance")
public class StaffAttendanceServlet extends HttpServlet {

    private final StaffAttendanceDAO attendanceDAO = new StaffAttendanceDAO();
    private final StaffDAO staffDAO = new StaffDAO();

    private static final List<String> EDITABLE_STATUSES = List.of("Present", "Late", "Absent", "OnLeave");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Staff manager = StaffAuthUtil.requireManager(req, resp);
        if (manager == null) return;

        try {
            LocalDate date = parseDate(req.getParameter("date"));
            autoBackfillAbsences(date);

            boolean isToday = date.equals(LocalDate.now());
            int currentShift = AppointmentDAO.shiftOf(LocalTime.now());

            String shiftParam = req.getParameter("shift");
            String keyword = req.getParameter("q");

            Integer shiftFilter;
            boolean shiftAutoApplied = false;
            if (shiftParam != null) {
                shiftFilter = parseShift(shiftParam);
            } else if (isToday && currentShift > 0) {
                shiftFilter = currentShift;
                shiftAutoApplied = true;
            } else {
                shiftFilter = null;
            }

            List<StaffAttendance> attendance = attendanceDAO.findByDate(date, shiftFilter, keyword);
            List<Staff> staffList = staffDAO.findAllVetsGroomers();

            req.setAttribute("filterDate", date.toString());
            req.setAttribute("today", LocalDate.now().toString());
            req.setAttribute("isToday", isToday);
            req.setAttribute("attendance", attendance);
            req.setAttribute("staffList", staffList);
            req.setAttribute("currentShift", currentShift);
            req.setAttribute("shiftFilter", shiftFilter != null ? shiftFilter.toString() : "");
            req.setAttribute("shiftAutoApplied", shiftAutoApplied);
            req.setAttribute("keyword", keyword);

            // Ngày sớm nhất được phép chọn cho form đăng ký nghỉ dài hạn
            req.setAttribute("minRangeStart", LocalDate.now().plusDays(1).toString());

            req.getRequestDispatcher("/WEB-INF/views/manager/shift/attendance.jsp").forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Không tải được dữ liệu chấm công: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/manager/shift/attendance.jsp").forward(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        Staff manager = StaffAuthUtil.requireManager(req, resp);
        if (manager == null) return;

        String action = req.getParameter("action");
        if ("markRange".equals(action)) {
            handleMarkRange(req, resp);
        } else if ("updateStatus".equals(action)) {
            handleUpdateStatus(req, resp);
        } else {
            handleCheckInShift(req, resp);
        }
    }

    // Check-in 1 ca cụ thể, chỉ khi ca chưa kết thúc
    private void handleCheckInShift(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String redirectDate = req.getParameter("date");
        try {
            int staffId = Integer.parseInt(req.getParameter("staffId"));
            LocalDate date = LocalDate.parse(req.getParameter("date"));
            int shift = Integer.parseInt(req.getParameter("shift"));
            String notes = req.getParameter("notes");

            if (!date.equals(LocalDate.now())) {
                req.getSession().setAttribute("flashError",
                        "Chỉ được check-in cho các ca trong NGÀY HÔM NAY.");
                resp.sendRedirect(req.getContextPath() + "/manager/attendance?date=" + redirectDate);
                return;
            }

            LocalTime now        = LocalTime.now();
            LocalTime shiftStart = AppointmentDAO.shiftStart(shift);
            LocalTime shiftEnd   = AppointmentDAO.shiftEnd(shift);

            if (!now.isBefore(shiftEnd)) {
                autoBackfillAbsences(date);
                req.getSession().setAttribute("flashError",
                        "Ca " + shift + " đã kết thúc (" + shiftEnd + "), không thể check-in nữa. "
                                + "Nhân viên chưa chấm công đã được tự động đánh dấu Vắng.");
                resp.sendRedirect(req.getContextPath() + "/manager/attendance?date=" + date);
                return;
            }

            String status = now.isBefore(shiftStart) ? "Present" : "Late";
            attendanceDAO.checkInShift(staffId, date, shift, status, notes);
            req.getSession().setAttribute("flashSuccess",
                    "Đã ghi nhận chấm công (" + status + ") cho ca " + shift + " ngày " + date + ".");
            resp.sendRedirect(req.getContextPath() + "/manager/attendance?date=" + date);
        } catch (Exception e) {
            e.printStackTrace();
            req.getSession().setAttribute("flashError", "Lỗi khi ghi nhận chấm công: " + e.getMessage());
            resp.sendRedirect(req.getContextPath() + "/manager/attendance"
                    + (redirectDate != null && !redirectDate.isBlank() ? "?date=" + redirectDate : ""));
        }
    }

    // Sửa status, ch cho ca trong ngày
    private void handleUpdateStatus(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String redirectDate = req.getParameter("date");
        try {
            int attendanceId = Integer.parseInt(req.getParameter("attendanceId"));
            String newStatus = req.getParameter("newStatus");

            StaffAttendance existing = attendanceDAO.findById(attendanceId);
            if (existing == null) {
                req.getSession().setAttribute("flashError", "Không tìm thấy bản ghi chấm công.");
                resp.sendRedirect(req.getContextPath() + "/manager/attendance?date=" + redirectDate);
                return;
            }
            if (!existing.getWorkDate().equals(LocalDate.now())) {
                req.getSession().setAttribute("flashError",
                        "Chỉ được chỉnh sửa trạng thái chấm công trong ngày hôm nay.");
                resp.sendRedirect(req.getContextPath() + "/manager/attendance?date=" + existing.getWorkDate());
                return;
            }
            if (!EDITABLE_STATUSES.contains(newStatus)) {
                req.getSession().setAttribute("flashError", "Trạng thái không hợp lệ.");
                resp.sendRedirect(req.getContextPath() + "/manager/attendance?date=" + redirectDate);
                return;
            }

            attendanceDAO.updateStatus(attendanceId, newStatus);
            req.getSession().setAttribute("flashSuccess", "Đã cập nhật trạng thái chấm công.");
            resp.sendRedirect(req.getContextPath() + "/manager/attendance?date=" + existing.getWorkDate());
        } catch (Exception e) {
            e.printStackTrace();
            req.getSession().setAttribute("flashError", "Lỗi khi sửa trạng thái: " + e.getMessage());
            resp.sendRedirect(req.getContextPath() + "/manager/attendance"
                    + (redirectDate != null && !redirectDate.isBlank() ? "?date=" + redirectDate : ""));
        }
    }

    // Đăng ký nghỉ dài hạn, bắt đầu từ rangeStart
    private void handleMarkRange(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            int staffId = Integer.parseInt(req.getParameter("staffId"));
            LocalDate from = LocalDate.parse(req.getParameter("fromDate"));
            LocalDate to   = LocalDate.parse(req.getParameter("toDate"));
            String status = req.getParameter("status"); // Absent | OnLeave
            if (!"Absent".equals(status) && !"OnLeave".equals(status)) status = "OnLeave";
            String notes = req.getParameter("notes");

            LocalDate earliestAllowed = LocalDate.now().plusDays(1);
            if (from.isBefore(earliestAllowed)) {
                req.getSession().setAttribute("flashError",
                        "Chỉ được đăng ký nghỉ dài hạn bắt đầu từ ngày mai (" + earliestAllowed
                                + ") trở đi, không áp dụng cho hôm nay hoặc quá khứ.");
                resp.sendRedirect(req.getContextPath() + "/manager/attendance");
                return;
            }
            if (to.isBefore(from)) {
                req.getSession().setAttribute("flashError", "Ngày kết thúc phải sau ngày bắt đầu.");
                resp.sendRedirect(req.getContextPath() + "/manager/attendance");
                return;
            }

            int days = attendanceDAO.markRangeAbsence(staffId, from, to, status, notes);

            HttpSession session = req.getSession();
            session.setAttribute("flashWarning",
                    "Đã ghi nhận nghỉ (" + status + ") cho " + days + " ngày, từ " + from + " đến " + to
                            + ". Các ngày bị ảnh hưởng được đánh dấu đỏ bên dưới — vui lòng kiểm tra và"
                            + " điều chỉnh GroomCap/VetCap nếu cần.");
            session.setAttribute("capacityAlertFrom", from);
            session.setAttribute("capacityAlertTo", to);

            resp.sendRedirect(req.getContextPath() + "/manager/capacity");
        } catch (Exception e) {
            e.printStackTrace();
            req.getSession().setAttribute("flashError", "Lỗi khi đăng ký nghỉ: " + e.getMessage());
            resp.sendRedirect(req.getContextPath() + "/manager/attendance");
        }
    }

    private void autoBackfillAbsences(LocalDate date) throws java.sql.SQLException {
        if (date.isAfter(LocalDate.now())) return;
        LocalDateTime now = LocalDateTime.now();
        List<Staff> staffList = staffDAO.findAllVetsGroomers();
        List<Integer> staffIds = new ArrayList<>();
        for (Staff s : staffList) staffIds.add(s.getStaffID());
        for (int shift = 1; shift <= 4; shift++) {
            LocalDateTime shiftEndDt = LocalDateTime.of(date, AppointmentDAO.shiftEnd(shift));
            if (now.isAfter(shiftEndDt)) {
                attendanceDAO.autoMarkAbsentForShift(shift, date, staffIds);
            }
        }
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return LocalDate.now();
        try { return LocalDate.parse(s); } catch (DateTimeParseException e) { return LocalDate.now(); }
    }

    private Integer parseShift(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            int v = Integer.parseInt(s.trim());
            return (v >= 1 && v <= 4) ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}