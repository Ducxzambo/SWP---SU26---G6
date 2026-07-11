package com.petclinic.servlet.booking;

import com.petclinic.dao.*;
import com.petclinic.dto.PetBookingRequest;
import com.petclinic.model.*;
import com.petclinic.service.BookingService;
import com.petclinic.service.PaymentService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Booking wizard:
 * GET /booking/new → Step 1: chọn loại lịch (Khám/Spa/Vaccine vs Nội trú)
 * POST /booking/new → Validate → Step 2 confirm
 * GET /booking/confirm → Step 2 confirm page (from session)
 * POST /booking/confirm → Create Pending appointments → Step 3 payment
 * GET /booking/payment → Step 3 payment page (chỉ còn "Thanh toán toàn bộ"
 *      cho booking thường; Nội trú giữ nguyên đặt cọc cố định)
 * POST /booking/payment → Call PayOS, redirect to QR checkout
 *
 * LƯU Ý KIẾN TRÚC: toàn bộ dữ liệu JSON (categories/vaccines/slots) cho
 * trang booking/new đã được CHUYỂN SANG SlotsApiServlet (/booking/slots).
 * Servlet này chỉ forward dữ liệu "tĩnh" (pets, today, prefill) — JS sẽ tự
 * gọi /booking/slots để lấy categories/vaccines/slots.
 *
 * Flow: 1 appointment - 1 pet - NHIỀU dịch vụ/vaccine - 1 slot - 1 invoice.
 * Quy trình: chọn loại lịch (thường/nội trú) → chọn pet → chọn (nhiều)
 * category dịch vụ (trừ "Điều trị"/"Chẩn đoán" — lọc sẵn ở SlotsApiServlet)
 * → chọn (nhiều) dịch vụ/vaccine trong các category đã chọn → chọn 1 khung
 * giờ. bookingPayload vẫn dùng cấu trúc JSON cũ để tái dùng UI hiện có,
 * nhưng nay serviceIds/vaccineIds có thể chứa NHIỀU phần tử:
 * [{"petId":1,"serviceIds":[11,12],"vaccineIds":[3,5]}]
 *
 * Nội trú GIỮ NGUYÊN logic cũ (ngày + buổi sáng/chiều, không qua slotKey),
 * chỉ siết lại đúng 1 thú cưng / lượt đặt.
 */
@WebServlet(urlPatterns = {"/booking/new"})
public class NewServlet extends HttpServlet {

    private final ServiceDAO serviceDAO = new ServiceDAO();
    private final PetDAO petDAO = new PetDAO();
    private final VaccineDAO vaccineDAO = new VaccineDAO();
    private final BookingService bookingSvc = new BookingService();
    private final PaymentService paymentSvc = new PaymentService();

    // ── GET ───────────────────────────────────────────────────────────────────

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Customer customer = requireLogin(req, resp);
        if (customer == null) return;

        try {
            List<Pet> pets = petDAO.findByCustomer(customer.getCustomerID());

            // prefillPet/prefillCat: dùng cho mô hình pet-centric — chọn sẵn 1 pet
            // và 1 category cho pet đó (vd: link "Đặt lịch tiêm vaccine" từ trang pet).
            String prefillPet = req.getParameter("prefillPet");
            String prefillCat = req.getParameter("prefillCat");

            req.setAttribute("navCategories", serviceDAO.findAllCategoriesWithServices());
            req.setAttribute("pets", pets);
            req.setAttribute("today", LocalDate.now().toString());
            req.setAttribute("prefillPet", prefillPet != null ? prefillPet : "");
            req.setAttribute("prefillCat", prefillCat != null ? prefillCat : "");

            req.getRequestDispatcher("/WEB-INF/views/booking/new.jsp").forward(req, resp);
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
            boolean isInpatient = "true".equals(req.getParameter("isInpatient"));
            if (isInpatient) {
                handleStep1PostInpatient(req, resp, customer);
            } else {
                handleStep1PostNormal(req, resp, customer);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException(e);
        }
    }

    /**
     * Nội trú — GIỮ NGUYÊN đặt cọc cố định 200.000đ (không thuộc phạm vi bỏ
     * cọc lần này, vì tổng chi phí nội trú thực tế chỉ biết được khi xuất
     * viện — ngoài phạm vi codebase này).
     */
    private void handleStep1PostInpatient(HttpServletRequest req, HttpServletResponse resp, Customer customer) throws Exception {
        String[] petIds = req.getParameterValues("petIds");
        String iDate = req.getParameter("inpatientDate");
        String iPeriod = req.getParameter("inpatientPeriod");
        String notes = req.getParameter("notes");

        if (petIds == null || petIds.length != 1) {
            forwardStep1Error(req, resp, customer, "Vui lòng chọn đúng 1 thú cưng cho mỗi lượt đặt lịch nội trú.");
            return;
        }
        if (iDate == null || iDate.isBlank()) {
            forwardStep1Error(req, resp, customer, "Vui lòng chọn ngày nhập viện.");
            return;
        }
        if (iPeriod == null || iPeriod.isBlank()) {
            forwardStep1Error(req, resp, customer, "Vui lòng chọn buổi sáng hoặc chiều.");
            return;
        }

        List<Integer> petIdList = Arrays.stream(petIds)
                .map(Integer::parseInt)
                .collect(Collectors.toList());

        List<Pet> selectedPets = petDAO.findByCustomer(customer.getCustomerID()).stream()
                .filter(p -> petIdList.contains(p.getPetID()))
                .collect(Collectors.toList());

        long depositAmount = bookingSvc.computeDeposit(true);

        HttpSession sess = req.getSession(true);
        sess.setAttribute("bk_petIds", petIds);
        sess.setAttribute("bk_isInpatient", true);
        sess.setAttribute("bk_iDate", iDate);
        sess.setAttribute("bk_iPeriod", iPeriod);
        sess.setAttribute("bk_notes", notes);
        sess.setAttribute("bk_total", BigDecimal.valueOf(depositAmount));
        sess.setAttribute("bk_deposit", depositAmount);

        req.setAttribute("selectedPets", selectedPets);
        req.setAttribute("isInpatient", true);
        req.setAttribute("inpatientDate", iDate);
        req.setAttribute("inpatientPeriod", "morning".equals(iPeriod) ? "Buổi sáng (08:00–12:00)" : "Buổi chiều (13:30–17:30)");
        req.setAttribute("notes", notes);
        req.setAttribute("totalPrice", BigDecimal.valueOf(depositAmount));
        req.setAttribute("depositAmount", depositAmount);
        req.setAttribute("navCategories", serviceDAO.findAllCategoriesWithServices());

        req.getRequestDispatcher("/WEB-INF/views/booking/confirm.jsp").forward(req, resp);
    }

    /**
     * Khám/Spa/Vaccine — ĐÚNG 1 thú cưng / lượt đặt lịch, nhưng nay cho phép
     * chọn NHIỀU dịch vụ và/hoặc NHIỀU vaccine (tối thiểu 1 mục). Không còn
     * đặt cọc 50.000đ — khách thanh toán 100% tổng chi phí ngay khi đặt lịch
     * (xem bookingSvc.computeDeposit(false) == 0, payment.jsp chỉ còn 1 lựa
     * chọn "Thanh toán toàn bộ").
     */
    private void handleStep1PostNormal(HttpServletRequest req, HttpServletResponse resp, Customer customer) throws Exception {
        String bookingPayload = req.getParameter("bookingPayload");
        String slotKey = req.getParameter("slotKey");
        String notes = req.getParameter("notes");

        if (bookingPayload == null || bookingPayload.isBlank()) {
            forwardStep1Error(req, resp, customer, "Vui lòng chọn thú cưng và dịch vụ.");
            return;
        }
        if (slotKey == null || slotKey.isBlank()) {
            forwardStep1Error(req, resp, customer, "Vui lòng chọn một khung giờ.");
            return;
        }

        List<PetBookingRequest> petBookings = parseBookingPayload(bookingPayload);
        if (petBookings.size() != 1) {
            forwardStep1Error(req, resp, customer, "Vui lòng chọn đúng 1 thú cưng cho mỗi lượt đặt lịch.");
            return;
        }

        PetBookingRequest onlyBooking = petBookings.get(0);
        int totalItems = onlyBooking.getServiceIds().size() + onlyBooking.getVaccineIds().size();
        if (totalItems < 1) {
            forwardStep1Error(req, resp, customer,
                    "Vui lòng chọn ít nhất 1 dịch vụ hoặc vaccine.");
            return;
        }

        Map<Integer, Pet> petById = petDAO.findByCustomer(customer.getCustomerID()).stream()
                .collect(Collectors.toMap(Pet::getPetID, p -> p));
        Map<Integer, Vaccine> vaccineById = vaccineDAO.findAvailable().stream()
                .collect(Collectors.toMap(Vaccine::getVaccineID, v -> v));

        BigDecimal total = BigDecimal.ZERO;
        List<Map<String, Object>> petBreakdown = new ArrayList<>();

        for (PetBookingRequest pb : petBookings) {
            Pet pet = petById.get(pb.getPetId());
            if (pet == null) continue;

            List<Service> svcs = pb.getServiceIds().isEmpty()
                    ? Collections.emptyList()
                    : serviceDAO.findByIds(pb.getServiceIds());

            List<Vaccine> vaccines = pb.getVaccineIds().stream()
                    .map(vaccineById::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            BigDecimal subtotal = svcs.stream()
                    .map(s -> s.getPrice() != null ? s.getPrice() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .add(vaccines.stream()
                            .map(Vaccine::getUnitPrice)
                            .reduce(BigDecimal.ZERO, BigDecimal::add));

            total = total.add(subtotal);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("pet", pet);
            row.put("services", svcs);
            row.put("vaccines", vaccines);
            row.put("subtotal", subtotal);
            petBreakdown.add(row);
        }

        // Booking thường KHÔNG còn đặt cọc — computeDeposit(false) luôn = 0.
        // Khách thanh toán 100% "total" ngay (xem PaymentServlet/payment.jsp).
        long depositAmount = bookingSvc.computeDeposit(false);

        HttpSession sess = req.getSession(true);
        sess.setAttribute("bk_payload", bookingPayload);
        sess.setAttribute("bk_slotKey", slotKey);
        sess.setAttribute("bk_isInpatient", false);
        sess.setAttribute("bk_notes", notes);
        sess.setAttribute("bk_total", total);
        sess.setAttribute("bk_deposit", depositAmount);

        req.setAttribute("petBreakdown", petBreakdown);
        req.setAttribute("slotKey", slotKey);
        req.setAttribute("isInpatient", false);
        req.setAttribute("notes", notes);
        req.setAttribute("totalPrice", total);
        req.setAttribute("depositAmount", depositAmount);
        req.setAttribute("navCategories", serviceDAO.findAllCategoriesWithServices());

        req.getRequestDispatcher("/WEB-INF/views/booking/confirm.jsp").forward(req, resp);
    }


    // ── Helpers ───────────────────────────────────────────────────────────────

    private void forwardStep1Error(HttpServletRequest req, HttpServletResponse resp, Customer customer, String msg) throws Exception {
        req.setAttribute("error", msg);
        doGet(req, resp);
    }


    private Customer requireLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession sess = req.getSession(false);
        Customer c = sess != null ? (Customer) sess.getAttribute("customer") : null;
        if (c == null) resp.sendRedirect(req.getContextPath() + "/auth/login");
        return c;
    }

    // ── bookingPayload parser ────────────────────────────────────────────────
    // Parser tối giản, KHÔNG dùng thư viện JSON ngoài (đồng bộ style với
    // PaymentService.extractJsonField) — cấu trúc cố định:
    // [{"petId":N,"serviceIds":[..],"vaccineIds":[..]}, ...]
    // (serviceIds/vaccineIds nay có thể chứa NHIỀU phần tử — không đổi so
    // với parser, vì đây vốn đã là mảng.)

    private static final Pattern PET_ID_RE = Pattern.compile("\"petId\"\\s*:\\s*(-?\\d+)");

    private List<PetBookingRequest> parseBookingPayload(String json) {
        List<PetBookingRequest> result = new ArrayList<>();
        if (json == null || json.isBlank()) return result;

        for (String obj : splitTopLevelObjects(json)) {
            Matcher m = PET_ID_RE.matcher(obj);
            if (!m.find()) continue;

            int petId = Integer.parseInt(m.group(1));
            List<Integer> serviceIds = extractIntArray(obj, "serviceIds");
            List<Integer> vaccineIds = extractIntArray(obj, "vaccineIds");

            result.add(new PetBookingRequest(petId, serviceIds, vaccineIds));
        }
        return result;
    }

    private List<String> splitTopLevelObjects(String json) {
        List<String> out = new ArrayList<>();
        int start = json.indexOf('[');
        int end = json.lastIndexOf(']');

        if (start < 0 || end < 0 || end <= start) return out;

        String inner = json.substring(start + 1, end);
        int depth = 0, objStart = -1;

        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (c == '{') {
                if (depth == 0) objStart = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && objStart >= 0) {
                    out.add(inner.substring(objStart, i + 1));
                    objStart = -1;
                }
            }
        }
        return out;
    }

    private List<Integer> extractIntArray(String obj, String key) {
        List<Integer> out = new ArrayList<>();
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\\[([^\\]]*)\\]").matcher(obj);

        if (m.find()) {
            String inside = m.group(1).trim();
            if (!inside.isEmpty()) {
                for (String part : inside.split(",")) {
                    try {
                        out.add(Integer.parseInt(part.trim()));
                    } catch (Exception ignored) {}
                }
            }
        }
        return out;
    }
}
