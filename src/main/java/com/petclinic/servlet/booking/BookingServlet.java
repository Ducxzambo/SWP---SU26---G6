package com.petclinic.servlet.booking;

import com.petclinic.dao.*;
import com.petclinic.model.*;
import com.petclinic.service.BookingService;
import com.petclinic.service.PaymentService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Booking wizard:
 *   GET  /booking/new          → Step 1: service + pet + slot picker
 *   POST /booking/new          → Validate → Step 2 confirm
 *   GET  /booking/confirm      → Step 2 confirm page (from session)
 *   POST /booking/confirm      → Create Pending appointments → Step 3 payment
 *   GET  /booking/payment      → Step 3 payment page (full / partial)
 *   POST /booking/payment      → Call PayOS, redirect to QR checkout
 */
@WebServlet(urlPatterns = {"/booking/new", "/booking/confirm", "/booking/payment"})
public class BookingServlet extends HttpServlet {

    private final ServiceDAO     serviceDAO     = new ServiceDAO();
    private final PetDAO         petDAO         = new PetDAO();
    private final VaccineDAO     vaccineDAO     = new VaccineDAO();
    private final BookingService bookingSvc     = new BookingService();
    private final PaymentService paymentSvc     = new PaymentService();

    // ── GET ───────────────────────────────────────────────────────────────────
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Customer customer = requireLogin(req, resp);
        if (customer == null) return;

        try {
            switch (req.getServletPath()) {
                case "/booking/confirm": handleConfirmGet(req, resp, customer); break;
                case "/booking/payment": handlePaymentGet(req, resp, customer); break;
                default:                handleStep1Get(req, resp, customer);    break;
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
                case "/booking/confirm": handleConfirmPost(req, resp, customer); break;
                case "/booking/payment": handlePaymentPost(req, resp, customer); break;
                default:                handleStep1Post(req, resp, customer);    break;
            }
        } catch (Exception e) { e.printStackTrace(); throw new ServletException(e); }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  STEP 1 – Service / Pet / Slot picker
    // ══════════════════════════════════════════════════════════════════════════
    private void handleStep1Get(HttpServletRequest req, HttpServletResponse resp, Customer customer)
            throws Exception {

        List<ServiceCategory> cats = serviceDAO.findAllCategoriesWithServices();
        // Inpatient có UI/luồng đặt riêng (giữ logic cũ) — không hiện trong
        // danh sách category chọn dịch vụ thông thường.
        cats.removeIf(c -> c.getCategoryID() == BookingService.INPATIENT_CATEGORY_ID);

        List<Pet> pets = petDAO.findByCustomer(customer.getCustomerID());
        List<Vaccine> vaccines = vaccineDAO.findAvailable();

        // Service "placeholder" dùng làm ServiceID (FK) cho Vaccine / Inpatient —
        // xem MIGRATION.sql. Lấy service đầu tiên (IsActive) của mỗi category.
        int vaccineServiceId   = firstServiceIdOfCategory(BookingService.VACCINE_CATEGORY_ID);
        int inpatientServiceId = firstServiceIdOfCategory(BookingService.INPATIENT_CATEGORY_ID);

        // Prefill from URL params (from quick-access links)
        String prefillCat = req.getParameter("prefillCategory");
        String prefillSvc = req.getParameter("prefillService");

        Map<LocalDate, List<TimeSlot>> slots = bookingSvc.generateSlots(Collections.emptyList());

        req.setAttribute("categories",         cats);
        req.setAttribute("navCategories",      cats);
        req.setAttribute("pets",               pets);
        req.setAttribute("today",              LocalDate.now().toString());
        req.setAttribute("slotsJson",          slotsToJson(slots));
        req.setAttribute("categoriesJson",     categoriesToJson(cats));
        req.setAttribute("vaccinesJson",       vaccinesToJson(vaccines));
        req.setAttribute("vaccineCategoryId",  BookingService.VACCINE_CATEGORY_ID);
        req.setAttribute("vaccineServiceId",   vaccineServiceId);
        req.setAttribute("inpatientServiceId", inpatientServiceId);
        req.setAttribute("prefillCat",     prefillCat != null ? prefillCat : "");
        req.setAttribute("prefillSvc",     prefillSvc != null ? prefillSvc : "");
        req.getRequestDispatcher("/WEB-INF/views/booking/new.jsp").forward(req, resp);
    }

    /** Service "placeholder" đầu tiên (IsActive) của 1 category — dùng cho Vaccine/Inpatient. */
    private int firstServiceIdOfCategory(int categoryId) throws Exception {
        List<Service> svcs = serviceDAO.findByCategory(categoryId);
        return svcs.isEmpty() ? -1 : svcs.get(0).getServiceID();
    }

    private void handleStep1Post(HttpServletRequest req, HttpServletResponse resp, Customer customer)
            throws Exception {

        String[] svcIds      = req.getParameterValues("serviceIds");
        String[] petIds      = req.getParameterValues("petIds");
        String[] vaccineIds  = req.getParameterValues("vaccineIds"); // chỉ dùng khi category = Vaccine
        String   slotKey     = req.getParameter("slotKey");     // single slot
        String   inpatient   = req.getParameter("isInpatient");
        String   iDate       = req.getParameter("inpatientDate");
        String   iPeriod     = req.getParameter("inpatientPeriod"); // "morning"/"afternoon"
        String   notes       = req.getParameter("notes");

        boolean isInpatient = "true".equals(inpatient);

        // ── Validation ──────────────────────────────────────────────────────
        if (svcIds == null || svcIds.length == 0) {
            forwardStep1Error(req, resp, customer, "Vui lòng chọn ít nhất một dịch vụ."); return;
        }
        if (petIds == null || petIds.length == 0) {
            forwardStep1Error(req, resp, customer, "Vui lòng chọn ít nhất một thú cưng."); return;
        }
        if (!isInpatient && (slotKey == null || slotKey.isBlank())) {
            forwardStep1Error(req, resp, customer, "Vui lòng chọn một khung giờ."); return;
        }
        if (isInpatient && (iDate == null || iDate.isBlank())) {
            forwardStep1Error(req, resp, customer, "Vui lòng chọn ngày nhập viện."); return;
        }
        if (isInpatient && (iPeriod == null || iPeriod.isBlank())) {
            forwardStep1Error(req, resp, customer, "Vui lòng chọn buổi sáng hoặc chiều."); return;
        }

        // Load details for confirm page
        List<Integer> svcIdList = Arrays.stream(svcIds).map(Integer::parseInt).collect(Collectors.toList());
        List<Integer> petIdList = Arrays.stream(petIds).map(Integer::parseInt).collect(Collectors.toList());
        List<Service> selectedServices = serviceDAO.findByIds(svcIdList);
        List<Pet>     selectedPets     = petDAO.findByCustomer(customer.getCustomerID()).stream()
                .filter(p -> petIdList.contains(p.getPetID())).collect(Collectors.toList());

        boolean isVaccineBooking = selectedServices.stream()
                .anyMatch(s -> s.getCategoryID() == BookingService.VACCINE_CATEGORY_ID);

        List<Vaccine> selectedVaccines = Collections.emptyList();
        if (isVaccineBooking) {
            if (vaccineIds == null || vaccineIds.length == 0) {
                forwardStep1Error(req, resp, customer, "Vui lòng chọn ít nhất một loại vaccine."); return;
            }
            List<Integer> vaccineIdList = Arrays.stream(vaccineIds).map(Integer::parseInt).collect(Collectors.toList());
            selectedVaccines = vaccineDAO.findAvailable().stream()
                    .filter(v -> vaccineIdList.contains(v.getVaccineID())).collect(Collectors.toList());
        }

        // Compute total price:
        //  - Service thường: sum(service.price) × số pet
        //  - Vaccine: KHÔNG dùng Service.Price (placeholder = 0) mà dùng
        //    sum(vaccine.unitPrice) × số pet (theo Vaccines.UnitPrice thật)
        BigDecimal svcTotal = selectedServices.stream()
                .filter(s -> s.getCategoryID() != BookingService.VACCINE_CATEGORY_ID)
                .map(s -> s.getPrice() != null ? s.getPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .multiply(BigDecimal.valueOf(selectedPets.size()));
        BigDecimal vaccineTotal = selectedVaccines.stream()
                .map(Vaccine::getUnitPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .multiply(BigDecimal.valueOf(selectedPets.size()));
        BigDecimal total = svcTotal.add(vaccineTotal);

        long depositAmount = bookingSvc.computeDeposit(total, isInpatient);

        // Store in session
        HttpSession sess = req.getSession(true);
        sess.setAttribute("bk_svcIds",     svcIds);
        sess.setAttribute("bk_petIds",     petIds);
        sess.setAttribute("bk_vaccineIds", vaccineIds != null ? vaccineIds : new String[0]);
        sess.setAttribute("bk_slotKey",    slotKey);
        sess.setAttribute("bk_isInpatient",isInpatient);
        sess.setAttribute("bk_iDate",      iDate);
        sess.setAttribute("bk_iPeriod",    iPeriod);
        sess.setAttribute("bk_notes",      notes);
        sess.setAttribute("bk_total",      total);
        sess.setAttribute("bk_deposit",    depositAmount);

        // Forward to confirm
        req.setAttribute("selectedServices", selectedServices);
        req.setAttribute("selectedVaccines", selectedVaccines);
        req.setAttribute("selectedPets",     selectedPets);
        req.setAttribute("slotKey",          slotKey);
        req.setAttribute("isInpatient",      isInpatient);
        req.setAttribute("inpatientDate",    iDate);
        req.setAttribute("inpatientPeriod",  "morning".equals(iPeriod) ? "Buổi sáng (08:00–12:00)" : "Buổi chiều (13:30–17:30)");
        req.setAttribute("notes",            notes);
        req.setAttribute("totalPrice",       total);
        req.setAttribute("depositAmount",    depositAmount);
        req.setAttribute("navCategories",    serviceDAO.findAllCategoriesWithServices());
        req.getRequestDispatcher("/WEB-INF/views/booking/confirm.jsp").forward(req, resp);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  STEP 2 – Confirm (GET = re-show from session, POST = create Pending)
    // ══════════════════════════════════════════════════════════════════════════
    private void handleConfirmGet(HttpServletRequest req, HttpServletResponse resp, Customer customer)
            throws Exception {
        HttpSession sess = req.getSession(false);
        if (sess == null || sess.getAttribute("bk_svcIds") == null) {
            resp.sendRedirect(req.getContextPath() + "/booking/new"); return;
        }
        // Re-build display attrs from session
        String[]  svcIds = (String[]) sess.getAttribute("bk_svcIds");
        String[]  petIds = (String[]) sess.getAttribute("bk_petIds");
        String[]  vaccineIds = (String[]) sess.getAttribute("bk_vaccineIds");
        List<Integer> svcIdList = Arrays.stream(svcIds).map(Integer::parseInt).collect(Collectors.toList());
        List<Integer> petIdList = Arrays.stream(petIds).map(Integer::parseInt).collect(Collectors.toList());
        List<Service> svcs = serviceDAO.findByIds(svcIdList);
        List<Pet> petsSelected = petDAO.findByCustomer(customer.getCustomerID()).stream()
                .filter(p -> petIdList.contains(p.getPetID())).collect(Collectors.toList());
        List<Vaccine> vaccinesSelected = Collections.emptyList();
        if (vaccineIds != null && vaccineIds.length > 0) {
            List<Integer> vaccineIdList = Arrays.stream(vaccineIds).map(Integer::parseInt).collect(Collectors.toList());
            vaccinesSelected = vaccineDAO.findAvailable().stream()
                    .filter(v -> vaccineIdList.contains(v.getVaccineID())).collect(Collectors.toList());
        }
        req.setAttribute("selectedServices", svcs);
        req.setAttribute("selectedVaccines", vaccinesSelected);
        req.setAttribute("selectedPets",     petsSelected);
        req.setAttribute("slotKey",          sess.getAttribute("bk_slotKey"));
        req.setAttribute("isInpatient",      sess.getAttribute("bk_isInpatient"));
        req.setAttribute("inpatientDate",    sess.getAttribute("bk_iDate"));
        boolean isMorning = "morning".equals(sess.getAttribute("bk_iPeriod"));
        req.setAttribute("inpatientPeriod",  isMorning ? "Buổi sáng (08:00–12:00)" : "Buổi chiều (13:30–17:30)");
        req.setAttribute("notes",            sess.getAttribute("bk_notes"));
        req.setAttribute("totalPrice",       sess.getAttribute("bk_total"));
        req.setAttribute("depositAmount",    sess.getAttribute("bk_deposit"));
        req.setAttribute("navCategories",    serviceDAO.findAllCategoriesWithServices());
        req.getRequestDispatcher("/WEB-INF/views/booking/confirm.jsp").forward(req, resp);
    }

    private void handleConfirmPost(HttpServletRequest req, HttpServletResponse resp, Customer customer)
            throws Exception {

        HttpSession sess = req.getSession(false);
        if (sess == null || sess.getAttribute("bk_svcIds") == null) {
            resp.sendRedirect(req.getContextPath() + "/booking/new"); return;
        }

        String[]  svcIds      = (String[]) sess.getAttribute("bk_svcIds");
        String[]  petIds      = (String[]) sess.getAttribute("bk_petIds");
        String[]  vaccineIds  = (String[]) sess.getAttribute("bk_vaccineIds");
        String    slotKey     = (String)   sess.getAttribute("bk_slotKey");
        boolean   isInpatient = (Boolean)  sess.getAttribute("bk_isInpatient");
        String    iDate       = (String)   sess.getAttribute("bk_iDate");
        String    iPeriod     = (String)   sess.getAttribute("bk_iPeriod");
        String    notes       = (String)   sess.getAttribute("bk_notes");
        BigDecimal total      = (BigDecimal) sess.getAttribute("bk_total");
        long       deposit    = ((Number)  sess.getAttribute("bk_deposit")).longValue();

        List<Integer> svcIdList = Arrays.stream(svcIds).map(Integer::parseInt).collect(Collectors.toList());
        List<Integer> petIdList = Arrays.stream(petIds).map(Integer::parseInt).collect(Collectors.toList());

        // Create Pending appointments (do NOT count toward capacity yet)
        List<Integer> apptIds = bookingSvc.createAppointments(
                customer.getCustomerID(), petIdList, svcIdList,
                slotKey, isInpatient, iDate, iPeriod);

        if (apptIds.isEmpty()) {
            sess.setAttribute("flashError", "Không thể tạo lịch hẹn. Vui lòng thử lại.");
            resp.sendRedirect(req.getContextPath() + "/booking/new"); return;
        }

        int primaryApptId = apptIds.get(0);

        // Create Unpaid invoice for the primary appointment
        int invoiceId = paymentSvc.createInvoice(customer.getCustomerID(), primaryApptId, total);

        // Service line items — bỏ qua placeholder "Tiêm Vaccine" (Price=0, vô nghĩa
        // khi hiển thị) vì giá thật của vaccine được thêm riêng ở dưới theo UnitPrice.
        List<Service> svcs = serviceDAO.findByIds(svcIdList);
        for (Service s : svcs) {
            if (s.getCategoryID() == BookingService.VACCINE_CATEGORY_ID) continue;
            paymentSvc.addInvoiceItem(invoiceId, "Service", s.getName(),
                    BigDecimal.valueOf(petIdList.size()), s.getPrice());
        }

        // Vaccine line items — tính theo Vaccines.UnitPrice thật, không dùng Service.Price.
        if (vaccineIds != null && vaccineIds.length > 0) {
            List<Integer> vaccineIdList = Arrays.stream(vaccineIds).map(Integer::parseInt).collect(Collectors.toList());
            List<Vaccine> vaccines = vaccineDAO.findAvailable().stream()
                    .filter(v -> vaccineIdList.contains(v.getVaccineID())).collect(Collectors.toList());
            for (Vaccine v : vaccines) {
                paymentSvc.addInvoiceItem(invoiceId, "Vaccine", v.getName(),
                        BigDecimal.valueOf(petIdList.size()), v.getUnitPrice());
            }
        }

        // Store in session for payment step
        sess.setAttribute("pay_apptId",   primaryApptId);
        sess.setAttribute("pay_apptIds",  apptIds);
        sess.setAttribute("pay_invoiceId",invoiceId);
        sess.setAttribute("pay_total",    total);
        sess.setAttribute("pay_deposit",  deposit);
        sess.setAttribute("pay_inpatient",isInpatient);

        // Clear booking session
        clearBookingSession(sess);

        resp.sendRedirect(req.getContextPath() + "/booking/payment");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  STEP 3 – Payment page + PayOS redirect
    // ══════════════════════════════════════════════════════════════════════════
    private void handlePaymentGet(HttpServletRequest req, HttpServletResponse resp, Customer customer)
            throws Exception {

        HttpSession sess = req.getSession(false);
        if (sess == null || sess.getAttribute("pay_invoiceId") == null) {
            resp.sendRedirect(req.getContextPath() + "/appointments"); return;
        }

        req.setAttribute("totalPrice",   sess.getAttribute("pay_total"));
        req.setAttribute("depositAmount",sess.getAttribute("pay_deposit"));
        req.setAttribute("apptId",       sess.getAttribute("pay_apptId"));
        req.setAttribute("invoiceId",    sess.getAttribute("pay_invoiceId"));
        req.setAttribute("isInpatient",  sess.getAttribute("pay_inpatient"));
        req.setAttribute("navCategories",serviceDAO.findAllCategoriesWithServices());
        req.getRequestDispatcher("/WEB-INF/views/booking/payment.jsp").forward(req, resp);
    }

    private void handlePaymentPost(HttpServletRequest req, HttpServletResponse resp, Customer customer)
            throws Exception {

        HttpSession sess = req.getSession(false);
        if (sess == null || sess.getAttribute("pay_invoiceId") == null) {
            resp.sendRedirect(req.getContextPath() + "/appointments"); return;
        }

        String payType   = req.getParameter("payType"); // "full" or "partial"
        int    invoiceId = (int) sess.getAttribute("pay_invoiceId");
        int    apptId    = (int) sess.getAttribute("pay_apptId");
        BigDecimal total = (BigDecimal) sess.getAttribute("pay_total");
        long   deposit   = ((Number) sess.getAttribute("pay_deposit")).longValue();
        boolean isInpatient = (Boolean) sess.getAttribute("pay_inpatient");

        boolean isFullPayment = "full".equals(payType);
        long amountVnd = isFullPayment ? total.longValue() : deposit;

        String desc = "PetClinic #" + invoiceId;
        String checkoutUrl = paymentSvc.createPaymentLink(
                invoiceId, apptId, amountVnd, desc, isFullPayment);

        if (checkoutUrl == null || checkoutUrl.isBlank()) {
            req.getSession().setAttribute("flashError",
                    "Không thể tạo liên kết thanh toán. Vui lòng thử lại.");
            resp.sendRedirect(req.getContextPath() + "/booking/payment");
            return;
        }

        // Redirect customer to PayOS checkout (VietQR page)
        resp.sendRedirect(checkoutUrl);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void forwardStep1Error(HttpServletRequest req, HttpServletResponse resp,
                                   Customer customer, String msg) throws Exception {
        req.setAttribute("error", msg);
        handleStep1Get(req, resp, customer);
    }

    private void clearBookingSession(HttpSession sess) {
        for (String k : new String[]{"bk_svcIds","bk_petIds","bk_slotKey","bk_isInpatient",
                "bk_iDate","bk_iPeriod","bk_notes","bk_total","bk_deposit"}) {
            sess.removeAttribute(k);
        }
    }

    private Customer requireLogin(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession sess = req.getSession(false);
        Customer c = sess != null ? (Customer) sess.getAttribute("customer") : null;
        if (c == null) resp.sendRedirect(req.getContextPath() + "/auth/login");
        return c;
    }

    // ── JSON serialisers ──────────────────────────────────────────────────────

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
                        .append("\"cap\":").append((int) ts.getMaxCapacity()).append(",")
                        .append("\"fill\":").append(ts.getFillPercent()).append(",")
                        .append("\"groomLoad\":").append(ts.getGroomLoad()).append(",")
                        .append("\"groomCap\":").append(ts.getGroomCap()).append(",")
                        .append("\"vetLoad\":").append(ts.getVetLoad()).append(",")
                        .append("\"vetCap\":").append(ts.getVetCap())
                        .append("}");
            }
            sb.append("]");
        }
        return sb.append("}").toString();
    }

    private String categoriesToJson(List<ServiceCategory> cats) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < cats.size(); i++) {
            ServiceCategory cat = cats.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"id\":").append(cat.getCategoryID())
                    .append(",\"name\":\"").append(esc(cat.getName())).append("\"")
                    .append(",\"services\":[");
            if (cat.getServices() != null) {
                for (int j = 0; j < cat.getServices().size(); j++) {
                    Service s = cat.getServices().get(j);
                    if (j > 0) sb.append(",");
                    sb.append("{\"id\":").append(s.getServiceID())
                            .append(",\"name\":\"").append(esc(s.getName())).append("\"")
                            .append(",\"price\":").append(s.getPrice())
                            .append(",\"duration\":").append(s.getDurationMinutes())
                            .append("}");
                }
            }
            sb.append("]}");
        }
        return sb.append("]").toString();
    }

    private String vaccinesToJson(List<Vaccine> vaccines) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vaccines.size(); i++) {
            Vaccine v = vaccines.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"id\":").append(v.getVaccineID())
                    .append(",\"name\":\"").append(esc(v.getName())).append("\"")
                    .append(",\"price\":").append(v.getUnitPrice())
                    .append(",\"stock\":").append(v.getStockQty())
                    .append("}");
        }
        return sb.append("]").toString();
    }

    private String esc(String s) {
        return s == null ? "" : s.replace("\\","\\\\").replace("\"","\\\"");
    }
}