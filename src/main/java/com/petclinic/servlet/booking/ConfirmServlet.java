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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@WebServlet(urlPatterns = {"/booking/confirm"})
public class ConfirmServlet extends HttpServlet {

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
            HttpSession sess = req.getSession(false);
            if (sess == null || sess.getAttribute("bk_isInpatient") == null) {
                resp.sendRedirect(req.getContextPath() + "/booking/new");
                return;
            }

            boolean isInpatient = (Boolean) sess.getAttribute("bk_isInpatient");
            if (isInpatient) {
                String[] petIds = (String[]) sess.getAttribute("bk_petIds");
                List<Integer> petIdList = Arrays.stream(petIds)
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());

                List<Pet> selectedPets = petDAO.findByCustomer(customer.getCustomerID()).stream()
                        .filter(p -> petIdList.contains(p.getPetID()))
                        .collect(Collectors.toList());

                boolean isMorning = "morning".equals(sess.getAttribute("bk_iPeriod"));

                req.setAttribute("selectedPets", selectedPets);
                req.setAttribute("isInpatient", true);
                req.setAttribute("inpatientDate", sess.getAttribute("bk_iDate"));
                req.setAttribute("inpatientPeriod", isMorning ? "Buổi sáng (08:00–12:00)" : "Buổi chiều (13:30–17:30)");
            }
            else {
                String bookingPayload = (String) sess.getAttribute("bk_payload");
                List<PetBookingRequest> petBookings = parseBookingPayload(bookingPayload);

                Map<Integer, Pet> petById = petDAO.findByCustomer(customer.getCustomerID()).stream()
                        .collect(Collectors.toMap(Pet::getPetID, p -> p));
                Map<Integer, Vaccine> vaccineById = vaccineDAO.findAvailable().stream()
                        .collect(Collectors.toMap(Vaccine::getVaccineID, v -> v));

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

                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("pet", pet);
                    row.put("services", svcs);
                    row.put("vaccines", vaccines);
                    row.put("subtotal", subtotal);
                    petBreakdown.add(row);
                }

                req.setAttribute("petBreakdown", petBreakdown);
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

            List<Integer> apptIds;
            int invoiceId;

            // Invoice được tạo NGAY SAU khi tạo appointment - khách CHƯA thanh
            // toán cọc ở bước này -> depositPaid luôn = false → status = 'Unpaid'.
            final boolean depositPaidAtCreation = false;

            if (isInpatient) {
                String[] petIds = (String[]) sess.getAttribute("bk_petIds");
                String iDate = (String) sess.getAttribute("bk_iDate");
                String iPeriod = (String) sess.getAttribute("bk_iPeriod");

                List<Integer> petIdList = Arrays.stream(petIds)
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());

                int inpatientServiceId = firstServiceIdOfCategory(BookingService.INPATIENT_CATEGORY_ID);
                apptIds = bookingSvc.createAppointments(customer.getCustomerID(), petIdList, Collections.singletonList(inpatientServiceId), null, true, iDate, iPeriod);

                if (apptIds.isEmpty()) {
                    sess.setAttribute("flashError", "Không thể tạo lịch hẹn. Vui lòng thử lại.");
                    resp.sendRedirect(req.getContextPath() + "/booking/new");
                    return;
                }

                // Nội trú: totalAmount của invoice = tiền cọc
                invoiceId = paymentSvc.createInvoice(customer.getCustomerID(), apptIds.get(0),
                        BigDecimal.valueOf(deposit), depositPaidAtCreation);
                paymentSvc.addInvoiceItem(invoiceId, "Other", "Đặt cọc nội trú", BigDecimal.ONE, BigDecimal.valueOf(deposit));
            } else {
                String bookingPayload = (String) sess.getAttribute("bk_payload");
                String slotKey = (String) sess.getAttribute("bk_slotKey");
                List<PetBookingRequest> petBookings = parseBookingPayload(bookingPayload);

                // đúng 1 appointment cho 1 pet + 1 dịch vụ (hoặc 1 vaccine) duy nhất
                apptIds = bookingSvc.createAppointmentsForPets(customer.getCustomerID(), petBookings, slotKey);

                if (apptIds.isEmpty()) {
                    sess.setAttribute("flashError", "Không thể tạo lịch hẹn. Vui lòng thử lại.");
                    resp.sendRedirect(req.getContextPath() + "/booking/new");
                    return;
                }

                // totalAmount của invoice = giá đúng 1 dịch vụ (hoặc vaccine) đã chọn.
                invoiceId = paymentSvc.createInvoice(customer.getCustomerID(), apptIds.get(0), total, depositPaidAtCreation);

                PetBookingRequest pb = petBookings.get(0);
                Pet pet = petDAO.findByCustomer(customer.getCustomerID()).stream()
                        .filter(p -> p.getPetID() == pb.getPetId())
                        .findFirst().orElse(null);
                String petName = pet != null ? pet.getName() : ("Pet " + pb.getPetId());

                if (!pb.getServiceIds().isEmpty()) {
                    Service svc = serviceDAO.findByIds(pb.getServiceIds()).stream().findFirst().orElse(null);
                    if (svc != null) {
                        paymentSvc.addInvoiceItem(invoiceId, "Service", petName + " - " + svc.getName(), BigDecimal.ONE, svc.getPrice());
                    }
                } else if (!pb.getVaccineIds().isEmpty()) {
                    Vaccine v = vaccineDAO.findAvailable().stream()
                            .filter(x -> x.getVaccineID() == pb.getVaccineIds().get(0))
                            .findFirst().orElse(null);
                    if (v != null) {
                        paymentSvc.addInvoiceItem(invoiceId, "Vaccine", petName + " - " + v.getName(), BigDecimal.ONE, v.getUnitPrice());
                    }
                }
            }

            sess.setAttribute("pay_apptId", apptIds.get(0));
            sess.setAttribute("pay_apptIds", apptIds);
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

    // ── bookingPayload parser ────────────────────────────────────────────────

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