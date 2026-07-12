package com.petclinic.servlet.groomer;

import com.petclinic.model.Appointment;
import com.petclinic.model.GroomingRecord;
import com.petclinic.model.Staff;
import com.petclinic.service.GroomingService;
import com.petclinic.service.GroomingService.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * BP-03 — Groomer side.
 *
 * GET  /groomer/session              → hàng chờ (assigned-to-me + unassigned)
 * GET  /groomer/session?action=start&appointmentID=X → tự nhận + bắt đầu
 * GET  /groomer/session?action=form&appointmentID=X  → form ghi nhận
 * GET  /groomer/session?action=view&recordID=X        → xem lại (read-only)
 * POST /groomer/session              → lưu bản ghi grooming → Done
 */
@WebServlet("/groomer/session")
public class GroomingServlet extends HttpServlet {

    private final GroomingService groomingService = new GroomingService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Staff groomer = getAuthenticatedGroomer(req, resp);
        if (groomer == null) return;

        String action = req.getParameter("action");
        if (action == null) action = "queue";

        switch (action) {
            case "queue"  -> showQueue(req, resp, groomer);
            case "start"  -> startSession(req, resp, groomer);
            case "form"   -> showForm(req, resp, groomer);
            case "view"   -> viewRecord(req, resp, groomer);
            default       -> resp.sendRedirect(req.getContextPath() + "/groomer/session");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        Staff groomer = getAuthenticatedGroomer(req, resp);
        if (groomer == null) return;

        String apptIdStr = req.getParameter("appointmentID");
        if (apptIdStr == null || apptIdStr.isBlank()) {
            resp.sendRedirect(req.getContextPath() + "/groomer/session");
            return;
        }

        int appointmentID;
        try { appointmentID = Integer.parseInt(apptIdStr); }
        catch (NumberFormatException e) {
            resp.sendRedirect(req.getContextPath() + "/groomer/session");
            return;
        }

        try {
            Appointment appt = groomingService.getAppointment(appointmentID);
            if (appt == null) {
                forwardFormError(req, resp, appointmentID, "Không tìm thấy lịch hẹn.");
                return;
            }

            GroomingRecord record = new GroomingRecord();
            record.setAppointmentID(appointmentID);
            record.setPetID(appt.getPetID());
            record.setGroomerID(groomer.getStaffID());
            record.setCoatCondition(req.getParameter("coatCondition"));
            record.setBehavior(req.getParameter("behavior"));
            record.setProductsUsed(req.getParameter("productsUsed"));
            record.setNotes(req.getParameter("notes"));
            boolean flagVet = "on".equals(req.getParameter("flagForVet"));
            record.setFlagForVet(flagVet);
            if (flagVet) record.setFlagReason(req.getParameter("flagReason"));

            SaveResult result = groomingService.saveGroomingRecord(record);
            switch (result) {
                case SUCCESS -> {
                    req.getSession().setAttribute("flashSuccess",
                            "Phiên grooming đã hoàn thành! Hóa đơn đang được tạo.");
                    triggerInvoice(appointmentID);
                    resp.sendRedirect(req.getContextPath() + "/groomer/session");
                }
                case RECORD_ALREADY_EXISTS ->
                        forwardFormError(req, resp, appointmentID, "Phiên grooming này đã có bản ghi rồi.");
                case WRONG_STATUS ->
                        forwardFormError(req, resp, appointmentID, "Lịch hẹn không ở trạng thái InProgress.");
                default ->
                        forwardFormError(req, resp, appointmentID, "Lỗi hệ thống, vui lòng thử lại.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            forwardFormError(req, resp, appointmentID, "Lỗi hệ thống: " + e.getMessage());
        }
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    private void showQueue(HttpServletRequest req, HttpServletResponse resp, Staff groomer)
            throws ServletException, IOException {
        LocalDate filterDate = parseDate(req.getParameter("date"));
        String shiftParam = req.getParameter("shift");
        try {
            List<Appointment> queue     = groomingService.getGroomerQueue(groomer.getStaffID(), filterDate);
            List<Appointment> completed = groomingService.getGroomerCompletedToday(groomer.getStaffID(), filterDate);

            if (shiftParam != null && !shiftParam.isBlank()) {
                int sf = Integer.parseInt(shiftParam);
                queue = queue.stream()
                        .filter(a -> a.getSlotShift() != null && a.getSlotShift() == sf)
                        .collect(java.util.stream.Collectors.toList());
                completed = completed.stream()
                        .filter(a -> a.getSlotShift() != null && a.getSlotShift() == sf)
                        .collect(java.util.stream.Collectors.toList());
            }
            req.setAttribute("queue",       queue);
            req.setAttribute("completed",   completed);
            req.setAttribute("filterDate",  filterDate.toString());
            req.setAttribute("isToday",     filterDate.equals(LocalDate.now()));
            req.setAttribute("shiftFilter", shiftParam != null ? shiftParam : "");
            req.setAttribute("currentGroomerID", groomer.getStaffID());
            req.getRequestDispatcher("/WEB-INF/views/groomer/session.jsp").forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Không tải được danh sách.");
            req.getRequestDispatcher("/WEB-INF/views/groomer/session.jsp").forward(req, resp);
        }
    }

    /** Bắt đầu grooming: tự nhận ca nếu chưa ai nhận, rồi chuyển InProgress + mở form. */
    private void startSession(HttpServletRequest req, HttpServletResponse resp, Staff groomer)
            throws ServletException, IOException {
        String idStr = req.getParameter("appointmentID");
        if (idStr == null) { resp.sendRedirect(req.getContextPath() + "/groomer/session"); return; }
        try {
            int apptID = Integer.parseInt(idStr);
            StartResult result = groomingService.startSession(apptID, groomer.getStaffID());
            if (result == StartResult.SUCCESS) {
                resp.sendRedirect(req.getContextPath()
                        + "/groomer/session?action=form&appointmentID=" + apptID);
                return;
            }
            String msg = switch (result) {
                case ALREADY_TAKEN        -> "Ca này đã được groomer khác nhận, không thể bắt đầu.";
                case NO_GROOMING_SERVICE  -> "Lịch hẹn này không có dịch vụ Grooming.";
                case WRONG_STATUS         -> "Trạng thái lịch hẹn không hợp lệ (phải là Arrived).";
                default                   -> "Không tìm thấy lịch hẹn.";
            };
            req.getSession().setAttribute("flashWarning", msg);
        } catch (Exception e) {
            e.printStackTrace();
            req.getSession().setAttribute("flashError", "Lỗi hệ thống.");
        }
        resp.sendRedirect(req.getContextPath() + "/groomer/session");
    }

    private void showForm(HttpServletRequest req, HttpServletResponse resp, Staff groomer)
            throws ServletException, IOException {
        String idStr = req.getParameter("appointmentID");
        if (idStr == null) { resp.sendRedirect(req.getContextPath() + "/groomer/session"); return; }
        try {
            int apptID = Integer.parseInt(idStr);
            Appointment appt = groomingService.getAppointment(apptID);
            if (appt == null) { resp.sendRedirect(req.getContextPath() + "/groomer/session"); return; }

            List<GroomingRecord> history = groomingService.getPetGroomingHistory(appt.getPetID());
            req.setAttribute("appointment", appt);
            req.setAttribute("history",     history);
            req.getRequestDispatcher("/WEB-INF/views/groomer/session-form.jsp").forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect(req.getContextPath() + "/groomer/session");
        }
    }

    private void viewRecord(HttpServletRequest req, HttpServletResponse resp, Staff groomer)
            throws ServletException, IOException {
        String idStr = req.getParameter("recordID");
        if (idStr == null) { resp.sendRedirect(req.getContextPath() + "/groomer/session"); return; }
        try {
            GroomingRecord rec = groomingService.getGroomingRecord(Integer.parseInt(idStr));
            if (rec == null) { resp.sendRedirect(req.getContextPath() + "/groomer/session"); return; }
            req.setAttribute("record", rec);
            req.getRequestDispatcher("/WEB-INF/views/groomer/session-form.jsp").forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect(req.getContextPath() + "/groomer/session");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void forwardFormError(HttpServletRequest req, HttpServletResponse resp,
                                  int appointmentID, String msg)
            throws ServletException, IOException {
        try {
            Appointment appt = groomingService.getAppointment(appointmentID);
            List<GroomingRecord> history = appt != null
                    ? groomingService.getPetGroomingHistory(appt.getPetID()) : List.of();
            req.setAttribute("appointment", appt);
            req.setAttribute("history",     history);
            req.setAttribute("error",       msg);
        } catch (Exception e) { e.printStackTrace(); }
        req.getRequestDispatcher("/WEB-INF/views/groomer/session-form.jsp").forward(req, resp);
    }

    private Staff getAuthenticatedGroomer(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null) { resp.sendRedirect(req.getContextPath() + "/auth/staff/login"); return null; }
        Staff staff = (Staff) session.getAttribute("staff");
        if (staff == null || !"Groomer".equals(staff.getRoleName())) {
            resp.sendRedirect(req.getContextPath() + "/auth/staff/login");
            return null;
        }
        return staff;
    }

    private LocalDate parseDate(String param) {
        if (param == null || param.isBlank()) return LocalDate.now();
        try { return LocalDate.parse(param); } catch (DateTimeParseException e) { return LocalDate.now(); }
    }

    private void triggerInvoice(int appointmentID) {
        System.out.println("[BP-05 STUB] Generate invoice for appointmentID=" + appointmentID);
    }
}