package com.petclinic.servlet.appointment;

import com.petclinic.dao.*;
import com.petclinic.model.*;
import com.petclinic.service.BookingService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * URL map:
 *   GET  /appointments                  → list (upcoming + history)
 *   GET  /appointments/detail?id=       → detail view
 *   GET  /appointments/reschedule?id=   → slot picker
 *   POST /appointments/reschedule       → save new slot
 *   POST /appointments/cancel           → cancel with reason
 *   GET  /appointments/pay?id=          → resume payment for Pending
 */
@WebServlet(urlPatterns = {
        "/appointments",
        "/appointments/detail",
        "/appointments/reschedule",
        "/appointments/cancel",
        "/appointments/pay"
})
public class AppointmentServlet extends HttpServlet {

    private final AppointmentDAO   apptDAO    = new AppointmentDAO();
    private final MedicalRecordDAO mrDAO      = new MedicalRecordDAO();
    private final InvoiceDAO       invoiceDAO = new InvoiceDAO();
    private final ServiceDAO       serviceDAO = new ServiceDAO();
    private final BookingService   bookingSvc = new BookingService();

    // ── GET ───────────────────────────────────────────────────────────────────
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Customer customer = requireLogin(req, resp);
        if (customer == null) return;
        try {
            switch (req.getServletPath()) {
                case "/appointments":            handleList(req, resp, customer);          break;
                case "/appointments/detail":     handleDetail(req, resp, customer);        break;
                case "/appointments/reschedule": handleRescheduleGet(req, resp, customer); break;
                case "/appointments/pay":        handlePay(req, resp, customer);           break;
                default: resp.sendRedirect(req.getContextPath() + "/appointments");
            }
        } catch (Exception e) { e.printStackTrace(); throw new ServletException(e); }
    }

    // ── POST ──────────────────────────────────────────────────────────────────
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        Customer customer = requireLogin(req, resp);
        if (customer == null) return;
        try {
            switch (req.getServletPath()) {
                case "/appointments/reschedule": handleReschedulePost(req, resp, customer); break;
                case "/appointments/cancel":     handleCancel(req, resp, customer);         break;
                default: resp.sendRedirect(req.getContextPath() + "/appointments");
            }
        } catch (Exception e) { e.printStackTrace(); throw new ServletException(e); }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  LIST
    // ══════════════════════════════════════════════════════════════════════════
    private void handleList(HttpServletRequest req, HttpServletResponse resp, Customer customer)
            throws Exception {
        List<Appointment> all = apptDAO.findByCustomer(customer.getCustomerID());

        List<Appointment> upcoming = new ArrayList<>();
        List<Appointment> history  = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (Appointment a : all) {
            boolean futureOrToday = a.getAppointmentDate() != null
                    && !a.getAppointmentDate().isBefore(today);
            boolean active = "Pending".equals(a.getStatus())
                    || "Confirmed".equals(a.getStatus())
                    || "InProgress".equals(a.getStatus());
            if (active && futureOrToday) upcoming.add(a);
            else                          history.add(a);
        }

        req.setAttribute("upcoming",      upcoming);
        req.setAttribute("history",       history);
        req.setAttribute("navCategories", serviceDAO.findAllCategoriesWithServices());
        req.setAttribute("unreadCount",
                new NotificationDAO().countUnread(customer.getCustomerID()));
        req.getRequestDispatcher("/WEB-INF/views/customer/appointments/appointment.jsp")
                .forward(req, resp);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DETAIL
    //  Không guard bằng canModify() — mọi appointment của customer đều xem được.
    // ══════════════════════════════════════════════════════════════════════════
    private void handleDetail(HttpServletRequest req, HttpServletResponse resp, Customer customer)
            throws Exception {
        int id = parseId(req.getParameter("id"));
        if (id < 0) { resp.sendRedirect(req.getContextPath() + "/appointments"); return; }

        Appointment appt = apptDAO.findById(id);
        // 404 chỉ khi không tìm thấy HOẶC không thuộc về customer này
        if (appt == null || appt.getCustomerID() != customer.getCustomerID()) {
            resp.sendError(404, "Không tìm thấy lịch khám."); return;
        }

        MedicalRecord mr      = mrDAO.findByAppointment(id);
        Invoice       invoice = invoiceDAO.findByAppointment(id);

        req.setAttribute("appt",          appt);
        req.setAttribute("medicalRecord", mr);
        req.setAttribute("invoice",       invoice);
        req.setAttribute("navCategories", serviceDAO.findAllCategoriesWithServices());
        req.setAttribute("unreadCount",
                new NotificationDAO().countUnread(customer.getCustomerID()));
        req.getRequestDispatcher("/WEB-INF/views/customer/appointments/appointment-detail.jsp")
                .forward(req, resp);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  RESCHEDULE GET
    //  Guard: chỉ cho reschedule nếu canReschedule() == true
    // ══════════════════════════════════════════════════════════════════════════
    private void handleRescheduleGet(HttpServletRequest req, HttpServletResponse resp,
                                     Customer customer) throws Exception {
        int id = parseId(req.getParameter("id"));
        if (id < 0) { resp.sendRedirect(req.getContextPath() + "/appointments"); return; }

        Appointment appt = apptDAO.findById(id);
        if (appt == null || appt.getCustomerID() != customer.getCustomerID()) {
            resp.sendError(404); return;
        }
        // Redirect về detail với flash nếu không thể reschedule
        if (!appt.canReschedule()) {
            req.getSession().setAttribute("flashError",
                    cannotModifyReason(appt));
            resp.sendRedirect(req.getContextPath()
                    + "/appointments/detail?id=" + id);
            return;
        }

        // Determine if inpatient by checking slot duration (≥ 4h = inpatient)
        boolean isInpatient = appt.getStartTime() != null && appt.getEndTime() != null
                && Duration.between(appt.getStartTime(), appt.getEndTime()).toMinutes() >= 240;

        List<Integer> serviceIds = Collections.singletonList(appt.getServiceID());
        Map<LocalDate, List<TimeSlot>> slots =
                bookingSvc.generateSlotsForReschedule(serviceIds, appt.getAppointmentID());

        req.setAttribute("appt",         appt);
        req.setAttribute("isInpatient",  isInpatient);
        req.setAttribute("slotsJson",    slotsToJson(slots));
        req.setAttribute("today",        LocalDate.now().toString());
        req.setAttribute("navCategories", serviceDAO.findAllCategoriesWithServices());
        req.setAttribute("unreadCount",
                new NotificationDAO().countUnread(customer.getCustomerID()));
        req.getRequestDispatcher("/WEB-INF/views/customer/appointments/appointment-reschedule.jsp")
                .forward(req, resp);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  RESCHEDULE POST
    // ══════════════════════════════════════════════════════════════════════════
    private void handleReschedulePost(HttpServletRequest req, HttpServletResponse resp,
                                      Customer customer) throws Exception {
        int id = parseId(req.getParameter("appointmentId"));
        Appointment appt = apptDAO.findById(id);
        if (appt == null || appt.getCustomerID() != customer.getCustomerID()) {
            resp.sendError(403); return;
        }
        if (!appt.canReschedule()) {
            req.getSession().setAttribute("flashError", cannotModifyReason(appt));
            resp.sendRedirect(req.getContextPath() + "/appointments/detail?id=" + id);
            return;
        }

        String slotKey = req.getParameter("slotKey");
        // Handle inpatient: slotKey = "yyyy-MM-dd|morning" or "yyyy-MM-dd|afternoon"
        if (slotKey == null || slotKey.isBlank()) {
            req.getSession().setAttribute("flashError", "Vui lòng chọn một khung giờ.");
            resp.sendRedirect(req.getContextPath() + "/appointments/reschedule?id=" + id);
            return;
        }

        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");
        String[] parts = slotKey.split("\\|");
        LocalDate date  = LocalDate.parse(parts[0], df);
        LocalTime start, end;

        String periodOrTime = parts.length > 1 ? parts[1] : "";
        if ("morning".equalsIgnoreCase(periodOrTime)) {
            start = LocalTime.of(8, 0); end = LocalTime.of(12, 0);
        } else if ("afternoon".equalsIgnoreCase(periodOrTime)) {
            start = LocalTime.of(13, 30); end = LocalTime.of(17, 30);
        } else {
            start = LocalTime.parse(periodOrTime, tf);
            end   = start.plusMinutes(BookingService.SLOT_MINUTES);
        }

        // Validate: slot must still be > 12h away
        if (!LocalDateTime.now().plusHours(12).isBefore(LocalDateTime.of(date, start))) {
            req.getSession().setAttribute("flashError",
                    "Khung giờ đã chọn phải cách hiện tại ít nhất 12 giờ.");
            resp.sendRedirect(req.getContextPath() + "/appointments/reschedule?id=" + id);
            return;
        }

        apptDAO.updateSlot(id, date, start, end);
        req.getSession().setAttribute("flashSuccess",
                "Đổi lịch thành công! Lịch hẹn đang chờ xác nhận.");
        resp.sendRedirect(req.getContextPath() + "/appointments/detail?id=" + id);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CANCEL
    // ══════════════════════════════════════════════════════════════════════════
    private void handleCancel(HttpServletRequest req, HttpServletResponse resp,
                              Customer customer) throws Exception {
        int id = parseId(req.getParameter("appointmentId"));
        Appointment appt = apptDAO.findById(id);
        if (appt == null || appt.getCustomerID() != customer.getCustomerID()) {
            resp.sendError(403); return;
        }
        if (!appt.canCancel()) {
            req.getSession().setAttribute("flashError", cannotModifyReason(appt));
            resp.sendRedirect(req.getContextPath() + "/appointments/detail?id=" + id);
            return;
        }

        String reason = req.getParameter("cancelReason");
        apptDAO.cancel(id, reason != null && !reason.isBlank() ? reason.trim() : null);
        req.getSession().setAttribute("flashSuccess", "Đã huỷ lịch khám thành công.");
        resp.sendRedirect(req.getContextPath() + "/appointments");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PAY (resume payment for Pending appointment)
    // ══════════════════════════════════════════════════════════════════════════
    private void handlePay(HttpServletRequest req, HttpServletResponse resp,
                           Customer customer) throws Exception {
        int id = parseId(req.getParameter("id"));
        Appointment appt = apptDAO.findById(id);
        if (appt == null || appt.getCustomerID() != customer.getCustomerID()) {
            resp.sendError(404); return;
        }
        if (!"Pending".equals(appt.getStatus())) {
            resp.sendRedirect(req.getContextPath() + "/appointments/detail?id=" + id); return;
        }
        Invoice invoice = invoiceDAO.findByAppointment(id);
        if (invoice == null || !"Unpaid".equals(invoice.getStatus())) {
            resp.sendRedirect(req.getContextPath() + "/appointments/detail?id=" + id); return;
        }

        boolean isInpatient = appt.getStartTime() != null && appt.getEndTime() != null
                && Duration.between(appt.getStartTime(), appt.getEndTime()).toMinutes() >= 240;
        long deposit = bookingSvc.computeDeposit(invoice.getTotalAmount(), isInpatient);

        HttpSession sess = req.getSession(true);
        sess.setAttribute("pay_apptId",    id);
        sess.setAttribute("pay_apptIds",   Collections.singletonList(id));
        sess.setAttribute("pay_invoiceId", invoice.getInvoiceID());
        sess.setAttribute("pay_total",     invoice.getTotalAmount());
        sess.setAttribute("pay_deposit",   deposit);
        sess.setAttribute("pay_inpatient", isInpatient);
        resp.sendRedirect(req.getContextPath() + "/booking/payment");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String cannotModifyReason(Appointment appt) {
        if (!"Pending".equals(appt.getStatus()) && !"Confirmed".equals(appt.getStatus())) {
            return "Lịch hẹn có trạng thái " + appt.getStatus()
                    + " không thể chỉnh sửa.";
        }
        return "Không thể chỉnh sửa lịch hẹn trong vòng 12 giờ trước giờ hẹn.";
    }

    private Customer requireLogin(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession sess = req.getSession(false);
        Customer c = sess != null ? (Customer) sess.getAttribute("customer") : null;
        if (c == null) {
            HttpSession s = req.getSession(true);
            s.setAttribute("redirectAfterLogin", req.getRequestURI()
                    + (req.getQueryString() != null ? "?" + req.getQueryString() : ""));
            resp.sendRedirect(req.getContextPath() + "/auth/login");
        }
        return c;
    }

    private int parseId(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return -1; }
    }

    private String slotsToJson(Map<LocalDate, List<TimeSlot>> slots) {
        StringBuilder sb = new StringBuilder("{");
        boolean fd = true;
        for (Map.Entry<LocalDate, List<TimeSlot>> e : slots.entrySet()) {
            if (!fd) sb.append(","); fd = false;
            sb.append("\"").append(e.getKey()).append("\":[");
            boolean fs = true;
            for (TimeSlot ts : e.getValue()) {
                if (!fs) sb.append(","); fs = false;
                sb.append("{")
                        .append("\"key\":\"").append(ts.getSlotKey()).append("\",")
                        .append("\"display\":\"").append(ts.getDisplayTime()).append("\",")
                        .append("\"available\":").append(ts.isAvailable()).append(",")
                        .append("\"load\":").append(ts.getCurrentLoad()).append(",")
                        .append("\"cap\":").append((int)ts.getMaxCapacity()).append(",")
                        .append("\"fill\":").append(ts.getFillPercent())
                        .append("}");
            }
            sb.append("]");
        }
        return sb.append("}").toString();
    }
}
