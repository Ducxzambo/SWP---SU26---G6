package com.petclinic.customer.servlet.appointment;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import com.petclinic.backend.dao.*;
import com.petclinic.backend.dto.TimeSlot;
import com.petclinic.backend.model.*;
import com.petclinic.backend.service.BookingService;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@WebServlet(urlPatterns = {
        "/appointments",
        "/appointments/detail",
        "/appointments/reschedule",
        "/appointments/cancel",
        "/appointments/pay"
})
public class AppointmentServlet extends HttpServlet {

    private static final int PAGE_SIZE = 8;

    private final AppointmentDAO apptDAO    = new AppointmentDAO();
    private final MedicalRecordDAO mrDAO      = new MedicalRecordDAO();
    private final GroomingRecordDAO groomingDAO    = new GroomingRecordDAO();
    private final VaccinationRecordDAO vaccinationDAO = new VaccinationRecordDAO();
    private final InvoiceDAO invoiceDAO = new InvoiceDAO();
    private final ServiceDAO serviceDAO = new ServiceDAO();
    private final ReviewDAO reviewDAO  = new ReviewDAO();
    private final NotificationDAO notiDAO    = new NotificationDAO();
    private final RefundDAO refundDAO  = new RefundDAO();
    private final PetDAO petDAO      = new PetDAO();
    private final BookingService bookingSvc = new BookingService();

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

    private void handleList(HttpServletRequest req, HttpServletResponse resp, Customer customer)
            throws Exception {
        List<Appointment> all = apptDAO.findByCustomer(customer.getCustomerID());

        Integer petFilter = parseNullableId(req.getParameter("petId"));
        if (petFilter != null) {
            List<Appointment> filtered = new ArrayList<>();
            for (Appointment a : all) {
                if (a.getPetID() != null && a.getPetID().equals(petFilter)) filtered.add(a);
            }
            all = filtered;
        }

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

        String view = "calendar".equals(req.getParameter("view")) ? "calendar" : "list";

        if ("calendar".equals(view)) {
            req.setAttribute("calendarJson", appointmentsToJson(all));
        } else {
            int upTotalPages   = Math.max(1, (int) Math.ceil(upcoming.size() / (double) PAGE_SIZE));
            int histTotalPages = Math.max(1, (int) Math.ceil(history.size()  / (double) PAGE_SIZE));
            int upPage   = Math.min(Math.max(1, parsePage(req.getParameter("upPage"))), upTotalPages);
            int histPage = Math.min(Math.max(1, parsePage(req.getParameter("histPage"))), histTotalPages);

            req.setAttribute("upcoming",  paginate(upcoming, upPage, PAGE_SIZE));
            req.setAttribute("history",   paginate(history, histPage, PAGE_SIZE));
            req.setAttribute("upPage",    upPage);
            req.setAttribute("histPage",  histPage);
            req.setAttribute("upTotalPages",   upTotalPages);
            req.setAttribute("histTotalPages", histTotalPages);
        }

        req.setAttribute("view",             view);
        req.setAttribute("petFilter",        petFilter);
        req.setAttribute("pets",             petDAO.findByCustomerId(customer.getCustomerID()));
        req.setAttribute("upcomingCount",    upcoming.size());
        req.setAttribute("historyCount",     history.size());
        req.setAttribute("navCategories",    serviceDAO.findAllCategoriesWithServices());
        req.setAttribute("unreadCount",
                new NotificationDAO().countUnread(customer.getCustomerID()));
        req.getRequestDispatcher("/WEB-INF/views/customer/appointments/appointment.jsp")
                .forward(req, resp);
    }

    private List<Appointment> paginate(List<Appointment> list, int page, int pageSize) {
        int from = (page - 1) * pageSize;
        if (from >= list.size() || from < 0) return new ArrayList<>();
        return list.subList(from, Math.min(from + pageSize, list.size()));
    }

    private int parsePage(String s) {
        try { return Math.max(1, Integer.parseInt(s)); } catch (Exception e) { return 1; }
    }

    private Integer parseNullableId(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return null; }
    }

    private String appointmentsToJson(List<Appointment> list) {
        JsonArray arr = new JsonArray();
        for (Appointment a : list) {
            if (a.getAppointmentDate() == null) continue;
            JsonObject o = new JsonObject();
            o.addProperty("id", a.getAppointmentID());
            o.addProperty("date", a.getAppointmentDate().toString());
            o.addProperty("startTime", a.getFormattedStartTime());
            o.addProperty("endTime", a.getFormattedEndTime());
            o.addProperty("service", a.getServiceName());
            o.addProperty("pet", a.getPetName() != null ? a.getPetName() : "");
            o.addProperty("status", a.getStatus());
            arr.add(o);
        }
        return arr.toString();
    }

    private void handleDetail(HttpServletRequest req, HttpServletResponse resp, Customer customer)
            throws Exception {
        int id = parseId(req.getParameter("id"));
        if (id < 0) { resp.sendRedirect(req.getContextPath() + "/appointments"); return; }

        Appointment appt = apptDAO.findById(id);
        if (appt == null || appt.getCustomerID() != customer.getCustomerID()) {
            resp.sendError(404, "Không tìm thấy lịch khám."); return;
        }

        MedicalRecord mr      = mrDAO.findByAppointment(id);
        GroomingRecord groomingRecord = groomingDAO.findByAppointmentId(id);
        List<VaccinationRecord> vaccinationRecords = vaccinationDAO.findByAppointment(id);
        Invoice invoice = invoiceDAO.findByAppointment(id);
        Review review = reviewDAO.findByAppointment(id);
        int noti = notiDAO.countUnread( customer.getCustomerID());


        req.setAttribute("appt",          appt);
        req.setAttribute("medicalRecord", mr);
        req.setAttribute("groomingRecord",     groomingRecord);
        req.setAttribute("vaccinationRecords", vaccinationRecords);
        req.setAttribute("invoice",       invoice);
        req.setAttribute("navCategories", serviceDAO.findAllCategoriesWithServices());
        req.setAttribute("review", review);
        req.setAttribute("unreadCount", noti);
        req.getRequestDispatcher("/WEB-INF/views/customer/appointments/appointment-detail.jsp")
                .forward(req, resp);
    }

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

        // Determine if inpatient by checking slot duration (>= 4h = inpatient)
        boolean isInpatient = appt.getStartTime() != null && appt.getEndTime() != null
                && Duration.between(appt.getStartTime(), appt.getEndTime()).toMinutes() >= 240;

        List<Integer> serviceIds = appt.getServiceIds();
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

        // Validate: ngày hẹn mới phải còn trước 17:30 của ngày hôm trước appointmentDate
        if (!LocalDateTime.now().isBefore(Appointment.deadlineFor(date))) {
            req.getSession().setAttribute("flashError",
                    "Khung giờ đã chọn phải được đặt trước 17:30 của ngày liền trước ngày hẹn.");
            resp.sendRedirect(req.getContextPath() + "/appointments/reschedule?id=" + id);
            return;
        }

        apptDAO.updateSlot(id, date, start, end);
        req.getSession().setAttribute("flashSuccess",
                "Đổi lịch thành công!");
        resp.sendRedirect(req.getContextPath() + "/appointments/detail?id=" + id);
    }

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
        String trimmedReason = reason != null && !reason.isBlank() ? reason.trim() : null;

        boolean refundRequested = isChecked(req.getParameter("refundRequested"));
        if (refundRequested && "Confirmed".equals(appt.getStatus())) {
            if (!recordRefundRequest(req, id, trimmedReason)) {
                resp.sendRedirect(req.getContextPath() + "/appointments/detail?id=" + id);
                return;
            }
        }

        apptDAO.cancel(id, trimmedReason);
        req.getSession().setAttribute("flashSuccess",
                refundRequested && "Confirmed".equals(appt.getStatus())
                        ? "Đã huỷ lịch khám và ghi nhận yêu cầu hoàn tiền. Chúng tôi sẽ liên hệ xử lý trong thời gian sớm nhất."
                        : "Đã huỷ lịch khám thành công.");
        resp.sendRedirect(req.getContextPath() + "/appointments");
    }

    private boolean recordRefundRequest(HttpServletRequest req, int appointmentId, String cancelReason)
            throws Exception {
        String bankCode      = trimOrNull(req.getParameter("bankCode"));
        String accountNumber = trimOrNull(req.getParameter("accountNumber"));
        String accountName   = trimOrNull(req.getParameter("accountName"));
        if (bankCode == null || accountNumber == null || accountName == null) {
            req.getSession().setAttribute("flashError",
                    "Vui lòng cung cấp đầy đủ thông tin ngân hàng (ngân hàng, số tài khoản, tên chủ tài khoản) để yêu cầu hoàn tiền.");
            return false;
        }

        Invoice invoice = invoiceDAO.findByAppointment(appointmentId);
        if (invoice == null) {
            req.getSession().setAttribute("flashError",
                    "Không tìm thấy hoá đơn nào cho lịch hẹn này nên không thể tạo yêu cầu hoàn tiền.");
            return false;
        }

        BigDecimal paidAmount = BigDecimal.ZERO;
        for (Payment p : invoice.getPayments()) {
            if (p.getAmount() != null) paidAmount = paidAmount.add(p.getAmount());
        }

        Refund refundReq = new Refund();
        refundReq.setAppointmentID(appointmentId);
        refundReq.setTotalAmount(invoice.getTotalAmount());
        refundReq.setPaidAmount(paidAmount);
        refundReq.setReason(cancelReason);
        refundReq.setBankCode(bankCode);
        refundReq.setAccountNumber(accountNumber);
        refundReq.setAccountName(accountName);
        refundDAO.createRequest(refundReq);
        return true;
    }

    private boolean isChecked(String param) {
        return "1".equals(param) || "on".equalsIgnoreCase(param) || "true".equalsIgnoreCase(param);
    }

    private String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

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
        BigDecimal payableTotal = invoice.getTotalAmount();
        if (isInpatient && (payableTotal == null || payableTotal.signum() <= 0)) {
            payableTotal = BigDecimal.valueOf(deposit);
        }

        HttpSession sess = req.getSession(true);
        sess.setAttribute("pay_apptId",    id);
        sess.setAttribute("pay_invoiceId", invoice.getInvoiceID());
        sess.setAttribute("pay_total",     payableTotal);
        sess.setAttribute("pay_deposit",   deposit);
        sess.setAttribute("pay_inpatient", isInpatient);
        resp.sendRedirect(req.getContextPath() + "/booking/payment");
    }


    private String cannotModifyReason(Appointment appt) {
        if (!"Pending".equals(appt.getStatus()) && !"Confirmed".equals(appt.getStatus())) {
            return "Lịch hẹn có trạng thái " + appt.getStatus()
                    + " không thể chỉnh sửa.";
        }
        return "Không thể chỉnh sửa lịch hẹn sau 17:30 của ngày trước lịch hẹn.";
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
        JsonObject root = new JsonObject();
        for (Map.Entry<LocalDate, List<TimeSlot>> e : slots.entrySet()) {
            JsonArray dayArr = new JsonArray();
            for (TimeSlot ts : e.getValue()) {
                JsonObject o = new JsonObject();
                o.addProperty("key", ts.getSlotKey());
                o.addProperty("display", ts.getDisplayTime());
                o.addProperty("available", ts.isAvailable());
                o.addProperty("load", ts.getCurrentLoad());
                o.addProperty("cap", (int) ts.getMaxCapacity());
                o.addProperty("fill", ts.getFillPercent());
                dayArr.add(o);
            }
            root.add(e.getKey().toString(), dayArr);
        }
        return root.toString();
    }
}
