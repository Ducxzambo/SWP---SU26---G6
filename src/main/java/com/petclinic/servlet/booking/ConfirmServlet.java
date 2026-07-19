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
                // petBreakdown gom TẤT CẢ dịch vụ + vaccine đã chọn cho 1 pet
                // (danh sách, không giới hạn 1 mục) — hiển thị đầy đủ ở confirm.jsp.
                String bookingPayload = (String) sess.getAttribute("bk_payload");
                List<PetBookingRequest> petBookings = PetBookingRequest.parseList(bookingPayload);

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

            int apptId;
            int invoiceId;


            if (isInpatient) {
                String[] petIds = (String[]) sess.getAttribute("bk_petIds");
                String iDate = (String) sess.getAttribute("bk_iDate");
                String iPeriod = (String) sess.getAttribute("bk_iPeriod");

                if (petIds == null || petIds.length != 1) {
                    sess.setAttribute("flashError", "Vui lòng chọn đúng 1 thú cưng cho mỗi lượt đặt lịch nội trú.");
                    resp.sendRedirect(req.getContextPath() + "/booking/new");
                    return;
                }
                int petId = Integer.parseInt(petIds[0]);

                int inpatientServiceId = firstServiceIdOfCategory(BookingService.INPATIENT_CATEGORY_ID);
                if (inpatientServiceId <= 0) {
                    sess.setAttribute("flashError",
                            "Hệ thống chưa cấu hình dịch vụ đại diện cho nhóm \"Dịch vụ nội trú\". "
                                    + "Vui lòng liên hệ quản trị viên để thêm ít nhất 1 dịch vụ (IsActive=1) cho nhóm này.");
                    resp.sendRedirect(req.getContextPath() + "/booking/new");
                    return;
                }
                apptId = bookingSvc.createInpatientAppointment(customer.getCustomerID(), petId, inpatientServiceId, iDate, iPeriod);

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
                List<PetBookingRequest> petBookings = PetBookingRequest.parseList(bookingPayload);

                if (petBookings.isEmpty()) {
                    sess.setAttribute("flashError", "Không thể tạo lịch hẹn. Vui lòng thử lại.");
                    resp.sendRedirect(req.getContextPath() + "/booking/new");
                    return;
                }
                PetBookingRequest pb = petBookings.get(0);

                // 1 appointment cho 1 pet + NHIỀU dịch vụ/vaccine đã chọn.
                // AppointmentServices được ghi bên trong createAppointmentForPet
                // (1 dòng/dịch vụ thực sự chọn; riêng Vaccine chỉ 1 dòng đại diện).
                apptId = bookingSvc.createNormalAppointment(customer.getCustomerID(), pb, slotKey);

                if (apptId <= 0) {
                    sess.setAttribute("flashError", "Không thể tạo lịch hẹn. Vui lòng thử lại.");
                    resp.sendRedirect(req.getContextPath() + "/booking/new");
                    return;
                }

                // totalAmount của invoice = tổng giá TẤT CẢ dịch vụ + vaccine đã chọn.
                invoiceId = paymentSvc.createInvoice(customer.getCustomerID(), apptId, total);

                Pet pet = petDAO.findByCustomer(customer.getCustomerID()).stream()
                        .filter(p -> p.getPetID() == pb.getPetId())
                        .findFirst().orElse(null);
                String petName = pet != null ? pet.getName() : ("Pet " + pb.getPetId());

                if (!pb.getServiceIds().isEmpty()) {
                    List<Service> svcs = serviceDAO.findByIds(pb.getServiceIds());
                    for (Service svc : svcs) {
                        paymentSvc.addInvoiceItem(invoiceId, "Service", petName + " - " + svc.getName(),
                                BigDecimal.ONE, svc.getPrice());
                    }
                }
                if (!pb.getVaccineIds().isEmpty()) {
                    Map<Integer, Vaccine> vaccineById = vaccineDAO.findAvailable().stream()
                            .collect(Collectors.toMap(Vaccine::getVaccineID, v -> v));
                    for (Integer vaccineId : new LinkedHashSet<>(pb.getVaccineIds())) {
                        Vaccine v = vaccineById.get(vaccineId);
                        if (v != null) {
                            paymentSvc.addInvoiceItem(invoiceId, "Vaccine", petName + " - " + v.getName(),
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
