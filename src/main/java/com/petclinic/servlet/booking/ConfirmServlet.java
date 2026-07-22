package com.petclinic.servlet.booking;

import com.petclinic.dao.*;
import com.petclinic.dto.BookingSelection;
import com.petclinic.model.*;
import com.petclinic.service.BookingService;
import com.petclinic.service.PaymentService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@WebServlet(urlPatterns = {"/booking/confirm"})
public class ConfirmServlet extends HttpServlet {

    private final ServiceDAO serviceDAO = new ServiceDAO();
    private final VaccineDAO vaccineDAO = new VaccineDAO();
    private final BookingService bookingSvc = new BookingService();
    private final PaymentService paymentSvc = new PaymentService();

    // ── GET ───────────────────────────────────────────────────────────────────

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Customer customer = requireLogin(req, resp);
        if (customer == null) return;

        try {
            HttpSession sess = req.getSession(false);
            if (sess == null || sess.getAttribute("bk_isInpatient") == null) {
                resp.sendRedirect(req.getContextPath() + "/booking/new");
                return;
            }

            boolean isInpatient = (Boolean) sess.getAttribute("bk_isInpatient");
            if (isInpatient) {
                boolean isMorning = "morning".equals(sess.getAttribute("bk_iPeriod"));

                req.setAttribute("isInpatient", true);
                req.setAttribute("inpatientDate", sess.getAttribute("bk_iDate"));
                req.setAttribute("inpatientPeriod", isMorning ? "Buổi sáng (08:00–12:00)" : "Buổi chiều (13:30–17:30)");
            }
            else {
                // services/vaccines gom TẤT CẢ dịch vụ + vaccine đã chọn cho lượt
                // đặt lịch này (danh sách, không giới hạn 1 mục) — hiển thị đầy
                // đủ ở confirm.jsp.
                String bookingPayload = (String) sess.getAttribute("bk_payload");
                BookingSelection selection = BookingSelection.parse(bookingPayload);

                Map<Integer, Vaccine> vaccineById = new LinkedHashMap<>();
                for (Vaccine v : vaccineDAO.findAvailable()) vaccineById.put(v.getVaccineID(), v);

                List<Service> svcs = selection.getServiceIds().isEmpty()
                        ? Collections.emptyList()
                        : serviceDAO.findByIds(selection.getServiceIds());

                List<Vaccine> vaccines = new ArrayList<>();
                for (Integer vid : selection.getVaccineIds()) {
                    Vaccine v = vaccineById.get(vid);
                    if (v != null) vaccines.add(v);
                }

                req.setAttribute("services", svcs);
                req.setAttribute("vaccines", vaccines);
                req.setAttribute("slotKey", sess.getAttribute("bk_slotKey"));
                req.setAttribute("isInpatient", false);
            }

            req.setAttribute("notes", sess.getAttribute("bk_notes"));
            req.setAttribute("totalPrice", sess.getAttribute("bk_total"));
            req.setAttribute("depositAmount", sess.getAttribute("bk_deposit"));
            req.setAttribute("navCategories", serviceDAO.findAllCategoriesWithServices());

            req.getRequestDispatcher("/WEB-INF/views/booking/confirm.jsp").forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException(e);
        }
    }

    // ── POST ──────────────────────────────────────────────────────────────────

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        Customer customer = requireLogin(req, resp);
        if (customer == null) return;

        try {
            HttpSession sess = req.getSession(false);
            if (sess == null || sess.getAttribute("bk_isInpatient") == null) {
                resp.sendRedirect(req.getContextPath() + "/booking/new");
                return;
            }

            boolean isInpatient = (Boolean) sess.getAttribute("bk_isInpatient");
            String notes = (String) sess.getAttribute("bk_notes");
            BigDecimal total = (BigDecimal) sess.getAttribute("bk_total");
            long deposit = ((Number) sess.getAttribute("bk_deposit")).longValue();

            int apptId;
            int invoiceId;


            if (isInpatient) {
                String iDate = (String) sess.getAttribute("bk_iDate");
                String iPeriod = (String) sess.getAttribute("bk_iPeriod");

                int inpatientServiceId = firstServiceIdOfCategory(BookingService.INPATIENT_CATEGORY_ID);
                if (inpatientServiceId <= 0) {
                    sess.setAttribute("flashError",
                            "Hệ thống chưa cấu hình dịch vụ đại diện cho nhóm \"Dịch vụ nội trú\". "
                                    + "Vui lòng liên hệ quản trị viên để thêm ít nhất 1 dịch vụ (IsActive=1) cho nhóm này.");
                    resp.sendRedirect(req.getContextPath() + "/booking/new");
                    return;
                }
                apptId = bookingSvc.createInpatientAppointment(customer.getCustomerID(), inpatientServiceId, iDate, iPeriod);

                if (apptId <= 0) {
                    sess.setAttribute("flashError", "Không thể tạo lịch hẹn. Vui lòng thử lại.");
                    resp.sendRedirect(req.getContextPath() + "/booking/new");
                    return;
                }

                invoiceId = paymentSvc.createInvoice(customer.getCustomerID(), apptId,
                        BigDecimal.valueOf(deposit));
                paymentSvc.addInvoiceItem(invoiceId, "Other", "Đặt cọc nội trú", BigDecimal.ONE, BigDecimal.valueOf(deposit));
            } else {
                String bookingPayload = (String) sess.getAttribute("bk_payload");
                String slotKey = (String) sess.getAttribute("bk_slotKey");
                BookingSelection selection = BookingSelection.parse(bookingPayload);

                if (selection.isEmpty()) {
                    sess.setAttribute("flashError", "Không thể tạo lịch hẹn. Vui lòng thử lại.");
                    resp.sendRedirect(req.getContextPath() + "/booking/new");
                    return;
                }

                // 1 appointment cho NHIỀU dịch vụ/vaccine đã chọn.
                // AppointmentServices được ghi bên trong createNormalAppointment
                // (1 dòng/dịch vụ thực sự chọn; riêng Vaccine chỉ 1 dòng đại diện).
                apptId = bookingSvc.createNormalAppointment(customer.getCustomerID(), selection, slotKey);

                if (apptId <= 0) {
                    sess.setAttribute("flashError", "Không thể tạo lịch hẹn. Vui lòng thử lại.");
                    resp.sendRedirect(req.getContextPath() + "/booking/new");
                    return;
                }

                // totalAmount của invoice = tổng giá TẤT CẢ dịch vụ + vaccine đã chọn.
                invoiceId = paymentSvc.createInvoice(customer.getCustomerID(), apptId, total);

                if (!selection.getServiceIds().isEmpty()) {
                    List<Service> svcs = serviceDAO.findByIds(selection.getServiceIds());
                    for (Service svc : svcs) {
                        paymentSvc.addInvoiceItem(invoiceId, "Service", svc.getName(),
                                BigDecimal.ONE, svc.getPrice());
                    }
                }
                if (!selection.getVaccineIds().isEmpty()) {
                    Map<Integer, Vaccine> vaccineById = new LinkedHashMap<>();
                    for (Vaccine v : vaccineDAO.findAvailable()) vaccineById.put(v.getVaccineID(), v);
                    for (Integer vaccineId : new LinkedHashSet<>(selection.getVaccineIds())) {
                        Vaccine v = vaccineById.get(vaccineId);
                        if (v != null) {
                            paymentSvc.addInvoiceItem(invoiceId, "Vaccine", v.getName(),
                                    BigDecimal.ONE, v.getUnitPrice());
                        }
                    }
                }
            }

            sess.setAttribute("pay_apptId", apptId);
            sess.setAttribute("pay_invoiceId", invoiceId);
            sess.setAttribute("pay_total", total);
            sess.setAttribute("pay_deposit", deposit);
            sess.setAttribute("pay_inpatient", isInpatient);

            clearBookingSession(sess);
            resp.sendRedirect(req.getContextPath() + "/booking/payment");
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException(e);
        }
    }


    // ── Helpers ───────────────────────────────────────────────────────────────
    private int firstServiceIdOfCategory(int categoryId) throws Exception {
        List<Service> svcs = serviceDAO.findByCategory(categoryId);
        return svcs.isEmpty() ? -1 : svcs.get(0).getServiceID();
    }
    private void clearBookingSession(HttpSession sess) {
        for (String k : new String[]{"bk_payload", "bk_petIds", "bk_slotKey", "bk_isInpatient",
                "bk_iDate", "bk_iPeriod", "bk_notes", "bk_total", "bk_deposit"}) {
            sess.removeAttribute(k);
        }
    }
    private Customer requireLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession sess = req.getSession(false);
        Customer c = sess != null ? (Customer) sess.getAttribute("customer") : null;
        if (c == null) resp.sendRedirect(req.getContextPath() + "/auth/login");
        return c;
    }
}
