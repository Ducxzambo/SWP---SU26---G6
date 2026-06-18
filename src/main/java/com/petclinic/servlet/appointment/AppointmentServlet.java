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
 *   GET  /appointments              → danh sách lịch khám
 *   GET  /appointments/detail?id=  → chi tiết 1 appointment
 *   GET  /appointments/reschedule?id= → trang đổi lịch (slot picker)
 *   POST /appointments/reschedule   → lưu slot mới
 *   POST /appointments/cancel       → huỷ appointment
 */
@WebServlet(urlPatterns = {
    "/appointments",
    "/appointments/detail",
    "/appointments/reschedule",
    "/appointments/cancel",
    "/appointments/pay"
})
public class AppointmentServlet extends HttpServlet {

    private final AppointmentDAO    apptDAO    = new AppointmentDAO();
    private final MedicalRecordDAO  mrDAO      = new MedicalRecordDAO();
    private final InvoiceDAO        invoiceDAO = new InvoiceDAO();
    private final ServiceDAO        serviceDAO = new ServiceDAO();
    private final BookingService    bookingSvc = new BookingService();
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
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException(e);
        }
    }

    // ── POST dispatcher ───────────────────────────────────────────────────────
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
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException(e);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  LIST
    // ══════════════════════════════════════════════════════════════════════════
    private void handleList(HttpServletRequest req, HttpServletResponse resp, Customer customer)
            throws Exception {

        List<Appointment> all = apptDAO.findByCustomer(customer.getCustomerID());

        // Separate into upcoming (Pending/Confirmed) and history
        List<Appointment> upcoming = new ArrayList<>();
        List<Appointment> history  = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (Appointment a : all) {
            boolean isActive = "Pending".equals(a.getStatus()) || "Confirmed".equals(a.getStatus())
                             || "InProgress".equals(a.getStatus());
            boolean isFuture = a.getAppointmentDate() != null && !a.getAppointmentDate().isBefore(today);
            if (isActive && isFuture) upcoming.add(a);
            else                       history.add(a);
        }

        req.setAttribute("upcoming",     upcoming);
        req.setAttribute("history",      history);
        req.setAttribute("navCategories", serviceDAO.findAllCategoriesWithServices());
        req.setAttribute("unreadCount",
            new NotificationDAO().countUnread(customer.getCustomerID()));
        req.getRequestDispatcher("/WEB-INF/views/customer/appointments.jsp").forward(req, resp);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DETAIL
    // ══════════════════════════════════════════════════════════════════════════
    private void handleDetail(HttpServletRequest req, HttpServletResponse resp, Customer customer)
            throws Exception {

        int id = parseId(req.getParameter("id"));
        if (id < 0) { resp.sendRedirect(req.getContextPath() + "/appointments"); return; }

        Appointment appt = apptDAO.findById(id);
        if (appt == null || appt.getCustomerID() != customer.getCustomerID()) {
            resp.sendError(404, "Không tìm thấy lịch khám.");
            return;
        }

        MedicalRecord mr      = mrDAO.findByAppointment(id);
        Invoice       invoice = invoiceDAO.findByAppointment(id);

        req.setAttribute("appt",         appt);
        req.setAttribute("medicalRecord", mr);
        req.setAttribute("invoice",       invoice);
        req.setAttribute("navCategories", serviceDAO.findAllCategoriesWithServices());
        req.setAttribute("unreadCount",
            new NotificationDAO().countUnread(customer.getCustomerID()));
        req.getRequestDispatcher("/WEB-INF/views/customer/appointment-detail.jsp").forward(req, resp);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  RESCHEDULE GET – show slot picker
    // ══════════════════════════════════════════════════════════════════════════
    private void handleRescheduleGet(HttpServletRequest req, HttpServletResponse resp, Customer customer)
            throws Exception {

        Appointment appt = loadAndGuard(req, resp, customer);
        if (appt == null) return;

        if (!appt.canModify()) {
            req.getSession().setAttribute("flashError",
                "Không thể đổi lịch trong vòng 12 giờ trước giờ hẹn.");
            resp.sendRedirect(req.getContextPath() + "/appointments/detail?id=" + appt.getAppointmentID());
            return;
        }

        // Generate slots (>12h from now, excluding current appointment's slot)
        List<Integer> serviceIds = Collections.singletonList(appt.getServiceID());
        Map<LocalDate, List<TimeSlot>> slots = bookingSvc.generateSlotsForReschedule(
                serviceIds, appt.getAppointmentID());

        // Determine if this appointment is inpatient (slot >= 4h)
        boolean isInpatient = appt.getStartTime() != null && appt.getEndTime() != null
            && Duration.between(appt.getStartTime(), appt.getEndTime()).toMinutes() >= 240;

        req.setAttribute("appt",         appt);
        req.setAttribute("slotsJson",    slotsToJson(slots));
        req.setAttribute("isInpatient",  isInpatient);
        req.setAttribute("today",        LocalDate.now().toString());
        req.setAttribute("navCategories", serviceDAO.findAllCategoriesWithServices());
        req.setAttribute("unreadCount",
            new NotificationDAO().countUnread(customer.getCustomerID()));
        req.getRequestDispatcher("/WEB-INF/views/customer/appointment-reschedule.jsp").forward(req, resp);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  RESCHEDULE POST – save new slot
    // ══════════════════════════════════════════════════════════════════════════
    private void handleReschedulePost(HttpServletRequest req, HttpServletResponse resp, Customer customer)
            throws Exception {

        int id = parseId(req.getParameter("appointmentId"));
        Appointment appt = apptDAO.findById(id);
        if (appt == null || appt.getCustomerID() != customer.getCustomerID()) {
            resp.sendError(403); return;
        }
        if (!appt.canModify()) {
            req.getSession().setAttribute("flashError",
                "Không thể đổi lịch trong vòng 12 giờ trước giờ hẹn.");
            resp.sendRedirect(req.getContextPath() + "/appointments/detail?id=" + id);
            return;
        }

        String slotKey = req.getParameter("slotKey"); // "yyyy-MM-dd|HH:mm" or "yyyy-MM-dd|morning/afternoon"
        if (slotKey == null || !slotKey.contains("|")) {
            req.getSession().setAttribute("flashError", "Vui lòng chọn một khung giờ.");
            resp.sendRedirect(req.getContextPath() + "/appointments/reschedule?id=" + id);
            return;
        }

        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String[]  parts  = slotKey.split("\\|");
        LocalDate date   = LocalDate.parse(parts[0], df);
        String    second = parts[1]; // HH:mm or "morning"/"afternoon"

        LocalTime start, end;
        if ("morning".equalsIgnoreCase(second)) {
            start = BookingService.INPATIENT_MORNING_START;
            end   = BookingService.INPATIENT_MORNING_END;
        } else if ("afternoon".equalsIgnoreCase(second)) {
            start = BookingService.INPATIENT_AFTERNOON_START;
            end   = BookingService.INPATIENT_AFTERNOON_END;
        } else {
            DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");
            start = LocalTime.parse(second, tf);
            end   = start.plusMinutes(BookingService.SLOT_MINUTES);
        }

        // Validate slot is still >12h away
        LocalDateTime slotDt = LocalDateTime.of(date, start);
        if (!LocalDateTime.now().plusHours(12).isBefore(slotDt)) {
            req.getSession().setAttribute("flashError",
                "Khung giờ đã chọn phải cách hiện tại ít nhất 12 giờ.");
            resp.sendRedirect(req.getContextPath() + "/appointments/reschedule?id=" + id);
            return;
        }

        apptDAO.updateSlot(id, date, start, end);
        req.getSession().setAttribute("flashSuccess",
            "Đổi lịch thành công! Lịch khám đang chờ xác nhận.");
        resp.sendRedirect(req.getContextPath() + "/appointments/detail?id=" + id);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CANCEL POST
    // ══════════════════════════════════════════════════════════════════════════
    private void handleCancel(HttpServletRequest req, HttpServletResponse resp, Customer customer)
            throws Exception {

        int id = parseId(req.getParameter("appointmentId"));
        Appointment appt = apptDAO.findById(id);
        if (appt == null || appt.getCustomerID() != customer.getCustomerID()) {
            resp.sendError(403); return;
        }
        if (!appt.canModify()) {
            req.getSession().setAttribute("flashError",
                "Không thể huỷ lịch trong vòng 12 giờ trước giờ hẹn.");
            resp.sendRedirect(req.getContextPath() + "/appointments/detail?id=" + id);
            return;
        }

        String reason = req.getParameter("cancelReason");
        if (reason == null) reason = "";
        reason = reason.trim();

        apptDAO.cancel(id, reason.isEmpty() ? null : reason);
        req.getSession().setAttribute("flashSuccess", "Đã huỷ lịch khám thành công.");
        resp.sendRedirect(req.getContextPath() + "/appointments");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PAY (resume payment for Pending appointment)
    // ══════════════════════════════════════════════════════════════════════════
    private void handlePay(HttpServletRequest req, HttpServletResponse resp, Customer customer)
            throws Exception {

        Appointment appt = loadAndGuard(req, resp, customer);
        if (appt == null) return;

        if (!"Pending".equals(appt.getStatus())) {
            resp.sendRedirect(req.getContextPath() + "/appointments/detail?id=" + appt.getAppointmentID());
            return;
        }

        Invoice invoice = invoiceDAO.findByAppointment(appt.getAppointmentID());
        if (invoice == null || !"Unpaid".equals(invoice.getStatus())) {
            resp.sendRedirect(req.getContextPath() + "/appointments/detail?id=" + appt.getAppointmentID());
            return;
        }

        // Compute deposit using BookingService
        long deposit = bookingSvc.computeDeposit(invoice.getTotalAmount(),
                appt.getStartTime() != null && appt.getEndTime() != null
                && Duration.between(appt.getStartTime(), appt.getEndTime()).toHours() >= 4);

        // Restore payment session so /booking/payment works
        HttpSession sess = req.getSession(true);
        sess.setAttribute("pay_apptId",   appt.getAppointmentID());
        sess.setAttribute("pay_apptIds",  Collections.singletonList(appt.getAppointmentID()));
        sess.setAttribute("pay_invoiceId",invoice.getInvoiceID());
        sess.setAttribute("pay_total",    invoice.getTotalAmount());
        sess.setAttribute("pay_deposit",  deposit);
        sess.setAttribute("pay_inpatient", false);

        resp.sendRedirect(req.getContextPath() + "/booking/payment");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Appointment loadAndGuard(HttpServletRequest req, HttpServletResponse resp,
                                     Customer customer) throws Exception {
        int id = parseId(req.getParameter("id"));
        if (id < 0) { resp.sendRedirect(req.getContextPath() + "/appointments"); return null; }
        Appointment appt = apptDAO.findById(id);
        if (appt == null || appt.getCustomerID() != customer.getCustomerID()) {
            resp.sendError(404); return null;
        }
        return appt;
    }

    private Customer requireLogin(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession session = req.getSession(false);
        Customer c = session != null ? (Customer) session.getAttribute("customer") : null;
        if (c == null) {
            session = req.getSession(true);
            session.setAttribute("redirectAfterLogin", req.getRequestURI()
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
            if (!fd) sb.append(",");
            fd = false;
            sb.append("\"").append(e.getKey()).append("\":[");
            boolean fs = true;
            for (TimeSlot ts : e.getValue()) {
                if (!fs) sb.append(",");
                fs = false;
                sb.append("{\"key\":\"").append(ts.getSlotKey()).append("\"")
                  .append(",\"display\":\"").append(ts.getDisplayTime()).append("\"")
                  .append(",\"available\":").append(ts.isAvailable()).append("}");
            }
            sb.append("]");
        }
        return sb.append("}").toString();
    }
}
