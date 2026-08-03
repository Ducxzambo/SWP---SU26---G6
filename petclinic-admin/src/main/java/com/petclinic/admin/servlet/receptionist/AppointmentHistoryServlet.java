package com.petclinic.admin.servlet.receptionist;

import com.petclinic.backend.dao.AppointmentDAO;
import com.petclinic.backend.dao.InvoiceDAO;
import com.petclinic.backend.dao.StaffDAO;
import com.petclinic.backend.model.Appointment;
import com.petclinic.backend.model.Invoice;
import com.petclinic.backend.model.Staff;
import com.petclinic.backend.service.ExaminationService;
import com.petclinic.backend.service.ExaminationService.FinalizeResult;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * BP-02/03 — Lịch sử lịch hẹn cho Lễ tân + Hoàn tất lịch hẹn.
 *
 * GET  /receptionist/history              → toàn bộ lịch hẹn trong ngày (mọi trạng thái)
 * POST /receptionist/history?action=finalize → xác nhận Hoàn tất (Status → Done)
 */
@WebServlet("/receptionist/history")
public class AppointmentHistoryServlet extends HttpServlet {

    private final ExaminationService examinationService = new ExaminationService();
    private final InvoiceDAO invoiceDAO = new InvoiceDAO();
    private final StaffDAO staffDAO = new StaffDAO();


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Staff staff = getAuthenticatedReceptionist(req, resp);
        if (staff == null) return;

        LocalDate filterDate = parseDate(req.getParameter("date"));
        Integer shiftFilter  = parseShift(req.getParameter("shift"));

        try {
            List<Appointment> history = examinationService.getAppointmentHistory(filterDate, shiftFilter);

            req.setAttribute("history",     history);
            List<Staff> vets     = staffDAO.findAllVets();
            List<Staff> groomers = staffDAO.findAllGroomers();
            req.setAttribute("vets",     vets);
            req.setAttribute("groomers", groomers);
            req.setAttribute("filterDate",  filterDate.toString());
            req.setAttribute("shiftFilter", shiftFilter != null ? shiftFilter.toString() : "");
            req.setAttribute("isToday",     filterDate.equals(LocalDate.now()));

            req.getRequestDispatcher("/WEB-INF/views/receptionist/history.jsp").forward(req, resp);

        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Lỗi hệ thống khi tải lịch sử lịch hẹn.");
            req.getRequestDispatcher("/WEB-INF/views/receptionist/history.jsp").forward(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        Staff staff = getAuthenticatedReceptionist(req, resp);
        if (staff == null) return;

        String action = req.getParameter("action");
        HttpSession session = req.getSession(false);

        if ("reassignStaff".equals(action)) {
            String apptIdStr  = req.getParameter("appointmentID");
            String catName    = req.getParameter("categoryName");
            String staffIdStr = req.getParameter("staffID");
            if (apptIdStr == null || catName == null || staffIdStr == null) {
                session.setAttribute("flashError", "Thiếu thông tin đổi nhân viên.");
                resp.sendRedirect(req.getContextPath() + "/receptionist/history"); return;
            }
            try {
                examinationService.reassignStaffByCategory(
                        Integer.parseInt(apptIdStr), catName, Integer.parseInt(staffIdStr));
                session.setAttribute("flashSuccess", "Đã cập nhật nhân viên phụ trách.");
            } catch (Exception e) {
                e.printStackTrace();
                session.setAttribute("flashError", "Lỗi khi đổi nhân viên: " + e.getMessage());
            }
            resp.sendRedirect(req.getContextPath() + "/receptionist/history"); return;
        }

        if (!"finalize".equals(action)) {
            resp.sendRedirect(req.getContextPath() + "/receptionist/history");
            return;
        }

        String apptIdStr = req.getParameter("appointmentID");
        if (apptIdStr == null || apptIdStr.isBlank()) {
            session.setAttribute("flashError", "Thiếu mã lịch hẹn.");
            resp.sendRedirect(req.getContextPath() + "/receptionist/history");
            return;
        }

        try {
            int appointmentID = Integer.parseInt(apptIdStr);
            List<String> missing = new ArrayList<>();
            FinalizeResult result = examinationService.finalizeAppointment(appointmentID, missing);

            switch (result) {
                case SUCCESS -> {
                        session.setAttribute("flashSuccess", "Đã hoàn tất lịch hẹn #" + appointmentID + "!");

                    // BP-04: chuyển sang màn hình tổng hợp hóa đơn để lễ tân thu
                    // phần còn lại (nếu có phát sinh thêm chi phí thuốc/xét
                    // nghiệm lúc khám) - tiền mặt hoặc
                    // chuyển khoản QR PayOS, y hệt màn hình ở bước check-in.
                    try {
                        Invoice invoice = invoiceDAO.findByAppointment(appointmentID);
                        if (invoice != null) {
                            resp.sendRedirect(req.getContextPath() + "/receptionist/invoice?invoiceId="
                                    + invoice.getInvoiceID() + "&from=history");
                            return;
                        }
                    } catch (Exception invEx) {
                        invEx.printStackTrace();
                    }
                }

                case NOT_READY ->
                        session.setAttribute("flashWarning",
                                "Chưa thể hoàn tất: dịch vụ thuộc [" + String.join(", ", missing)
                                        + "] chưa có bản ghi (bệnh án/kết quả grooming).");
                case WRONG_STATUS ->
                        session.setAttribute("flashWarning", "Lịch hẹn không ở trạng thái có thể hoàn tất.");
                default ->
                        session.setAttribute("flashError", "Không tìm thấy lịch hẹn.");
            }
        } catch (NumberFormatException e) {
            session.setAttribute("flashError", "ID lịch hẹn không hợp lệ.");
        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("flashError", "Lỗi hệ thống: " + e.getMessage());
        }

        resp.sendRedirect(req.getContextPath() + "/receptionist/history");
    }

    private Staff getAuthenticatedReceptionist(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/staff/login");
            return null;
        }
        Staff staff = (Staff) session.getAttribute("staff");
        if (staff == null || !"Receptionist".equals(staff.getRoleName())) {
            resp.sendRedirect(req.getContextPath() + "/auth/staff/login");
            return null;
        }
        return staff;
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