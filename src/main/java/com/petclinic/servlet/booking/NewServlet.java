package com.petclinic.servlet.booking;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Booking wizard:
 * GET /booking/new → Step 1: chọn loại lịch (Khám/Spa/Vaccine vs Nội trú)
 * POST /booking/new → Validate → Step 2 confirm
 * GET /booking/confirm → Step 2 confirm page (from session)
 * POST /booking/confirm → Create Pending appointments → Step 3 payment
 * GET /booking/payment → Step 3 payment page
 * POST /booking/payment → Call PayOS, redirect to QR checkout
 *
 * [{"petId":1,"serviceIds":[11,12],"vaccineIds":[3,5]}]
 */
@WebServlet(urlPatterns = {"/booking/new"})
public class NewServlet extends HttpServlet {

    private final ServiceDAO serviceDAO = new ServiceDAO();
    private final PetDAO petDAO = new PetDAO();
    private final VaccineDAO vaccineDAO = new VaccineDAO();
    private final BookingService bookingSvc = new BookingService();

    // ── GET ───────────────────────────────────────────────────────────────────

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Customer customer = requireLogin(req, resp);
        if (customer == null) return;

        try {
            String prefillCat = req.getParameter("prefillCat");

            req.setAttribute("navCategories", serviceDAO.findAllCategoriesWithServices());
            req.setAttribute("today", LocalDate.now().toString());
            req.setAttribute("prefillCat", prefillCat != null ? prefillCat : "");
            req.setAttribute("resumeData", buildResumeJson(req));

            req.getRequestDispatcher("/WEB-INF/views/booking/new.jsp").forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException(e);
        }
    }

    /**
     * Nếu session đang giữ dữ liệu 1 lượt đặt lịch đã nhập dở (vd khách bấm
     * "Quay lại chỉnh sửa" từ trang confirm, hoặc quay lại giữa chừng), đóng
     * gói lại thành JSON để JS khôi phục ĐÚNG trạng thái đã chọn trước đó
     * (thú cưng/category/dịch vụ/vaccine/khung giờ/ghi chú), thay vì bắt đầu
     * lại từ đầu. Trả về null nếu không có gì để khôi phục.
     */
    private String buildResumeJson(HttpServletRequest req) {
        HttpSession sess = req.getSession(false);
        if (sess == null || sess.getAttribute("bk_isInpatient") == null) return null;

        boolean isInpatient = Boolean.TRUE.equals(sess.getAttribute("bk_isInpatient"));

        JsonObject o = new JsonObject();
        o.addProperty("isInpatient", isInpatient);
        o.addProperty("notes", (String) sess.getAttribute("bk_notes"));

        if (isInpatient) {
            o.addProperty("inpatientDate", (String) sess.getAttribute("bk_iDate"));
            o.addProperty("inpatientPeriod", (String) sess.getAttribute("bk_iPeriod"));
        } else {
            // bookingPayload la 1 chuoi JSON co san (xem PetBookingRequest) —
            // gui thang cho client, JS se tu parse lai (khong can dich 2 lan).
            o.addProperty("payload", (String) sess.getAttribute("bk_payload"));
            o.addProperty("slotKey", (String) sess.getAttribute("bk_slotKey"));
        }

        return o.toString();
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
        String iDate = req.getParameter("inpatientDate");
        String iPeriod = req.getParameter("inpatientPeriod");
        String notes = req.getParameter("notes");

        if (iDate == null || iDate.isBlank()) {
            forwardStep1Error(req, resp, customer, "Vui lòng chọn ngày nhập viện.");
            return;
        }
        if (iPeriod == null || iPeriod.isBlank()) {
            forwardStep1Error(req, resp, customer, "Vui lòng chọn buổi sáng hoặc chiều.");
            return;
        }

        long depositAmount = bookingSvc.computeDeposit(true);

        HttpSession sess = req.getSession(true);
        sess.setAttribute("bk_isInpatient", true);
        sess.setAttribute("bk_iDate", iDate);
        sess.setAttribute("bk_iPeriod", iPeriod);
        sess.setAttribute("bk_notes", notes);
        sess.setAttribute("bk_total", BigDecimal.valueOf(depositAmount));
        sess.setAttribute("bk_deposit", depositAmount);

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
     * Khám/Spa/Vaccine.
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

        BookingSelection selection = BookingSelection.parse(bookingPayload);
        int totalItems = selection.getServiceIds().size() + selection.getVaccineIds().size();
        if (totalItems < 1) {
            forwardStep1Error(req, resp, customer,
                    "Vui lòng chọn ít nhất 1 dịch vụ hoặc vaccine.");
            return;
        }

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

        BigDecimal total = BigDecimal.ZERO;
        for (Service s : svcs) total = total.add(s.getPrice() != null ? s.getPrice() : BigDecimal.ZERO);
        for (Vaccine v : vaccines) total = total.add(v.getUnitPrice() != null ? v.getUnitPrice() : BigDecimal.ZERO);

        // Khách thanh toán 100% "total".
        long depositAmount = bookingSvc.computeDeposit(false);

        HttpSession sess = req.getSession(true);
        sess.setAttribute("bk_payload", bookingPayload);
        sess.setAttribute("bk_slotKey", slotKey);
        sess.setAttribute("bk_isInpatient", false);
        sess.setAttribute("bk_notes", notes);
        sess.setAttribute("bk_total", total);
        sess.setAttribute("bk_deposit", depositAmount);

        req.setAttribute("services", svcs);
        req.setAttribute("vaccines", vaccines);
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

}
