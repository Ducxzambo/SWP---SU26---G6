package com.petclinic.servlet.booking;

import com.petclinic.dao.*;
import com.petclinic.dto.PetBookingRequest;
import com.petclinic.model.*;
import com.petclinic.service.PaymentService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WebServlet(urlPatterns = {"/booking/payment"})
public class PaymentServlet extends HttpServlet {

    private final ServiceDAO serviceDAO = new ServiceDAO();
    private final PaymentService paymentSvc = new PaymentService();

    // ── GET ───────────────────────────────────────────────────────────────────

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Customer customer = requireLogin(req, resp);
        if (customer == null) return;

        try {
            HttpSession sess = req.getSession(false);
            if (sess == null || sess.getAttribute("pay_invoiceId") == null) {
                resp.sendRedirect(req.getContextPath() + "/appointments");
                return;
            }

            req.setAttribute("totalPrice", sess.getAttribute("pay_total"));
            req.setAttribute("depositAmount", sess.getAttribute("pay_deposit"));
            req.setAttribute("apptId", sess.getAttribute("pay_apptId"));
            req.setAttribute("invoiceId", sess.getAttribute("pay_invoiceId"));
            req.setAttribute("isInpatient", sess.getAttribute("pay_inpatient"));
            req.setAttribute("navCategories", serviceDAO.findAllCategoriesWithServices());

            req.getRequestDispatcher("/WEB-INF/views/booking/payment.jsp").forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException(e);
        }
    }

    // ── POST ──────────────────────────────────────────────────────────────────

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession sess = req.getSession(false);
        if (sess == null || sess.getAttribute("pay_invoiceId") == null) {
            resp.sendRedirect(req.getContextPath() + "/appointments");
            return;
        }

//        String payType = req.getParameter("payType"); // "full" or "partial"
        int invoiceId = (int) sess.getAttribute("pay_invoiceId");
        int apptId = (int) sess.getAttribute("pay_apptId");
        BigDecimal total = (BigDecimal) sess.getAttribute("pay_total");
        long deposit = ((Number) sess.getAttribute("pay_deposit")).longValue();

//        boolean isFullPayment = "full".equals(payType);
//        long amountVnd = isFullPayment ? total.longValue() : deposit;
        String desc = "PetClinic " + invoiceId;

        String checkoutUrl;
        try {
            checkoutUrl = paymentSvc.createPaymentLink(invoiceId, apptId, deposit, desc, false);
        } catch (Exception ex) {
            ex.printStackTrace();
            req.getSession().setAttribute("flashError", "Không thể tạo liên kết thanh toán. Vui lòng thử lại.");
            resp.sendRedirect(req.getContextPath() + "/booking/payment");
            return;
        }

        if (checkoutUrl == null || checkoutUrl.isBlank()) {
            req.getSession().setAttribute("flashError", "Không thể tạo liên kết thanh toán. Vui lòng thử lại.");
            resp.sendRedirect(req.getContextPath() + "/booking/payment");
            return;
        }

        resp.sendRedirect(checkoutUrl);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private Customer requireLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession sess = req.getSession(false);
        Customer c = sess != null ? (Customer) sess.getAttribute("customer") : null;
        if (c == null) resp.sendRedirect(req.getContextPath() + "/auth/login");
        return c;
    }

    // ── bookingPayload parser ────────────────────────────────────────────────
    // Parser tối giản, KHÔNG dùng thư viện JSON ngoài (đồng bộ style với
    // PaymentService.extractJsonField) — chỉ cần đủ cho đúng 1 cấu trúc cố
    // định: [{"petId":N,"serviceIds":[..],"vaccineIds":[..]}, ...]

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
