package com.petclinic.customer.servlet.booking;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import com.petclinic.backend.dao.ServiceDAO;
import com.petclinic.backend.model.Customer;
import com.petclinic.backend.service.PaymentService;

import java.io.IOException;
import java.math.BigDecimal;

@WebServlet(urlPatterns = {"/booking/payment"})
public class PaymentServlet extends HttpServlet {

    private final ServiceDAO serviceDAO = new ServiceDAO();
    private final PaymentService paymentSvc = new PaymentService();

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

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession sess = req.getSession(false);
        if (sess == null || sess.getAttribute("pay_invoiceId") == null) {
            resp.sendRedirect(req.getContextPath() + "/appointments");
            return;
        }

        String payType = req.getParameter("payType"); // "full" or "partial"
        int invoiceId = (int) sess.getAttribute("pay_invoiceId");
        int apptId = (int) sess.getAttribute("pay_apptId");
        BigDecimal total = (BigDecimal) sess.getAttribute("pay_total");
        long deposit = ((Number) sess.getAttribute("pay_deposit")).longValue();
        boolean isInpatient = Boolean.TRUE.equals(sess.getAttribute("pay_inpatient"));
        boolean isFullPayment = "full".equals(payType);
        long amountVnd = isFullPayment ? total.longValue() : deposit;
        String desc = "PetClinic " + invoiceId;

        String checkoutUrl;
        try {
            checkoutUrl = paymentSvc.createPaymentLink(invoiceId, apptId, amountVnd, desc, isFullPayment);
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

    // Helpers
    private Customer requireLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession sess = req.getSession(false);
        Customer c = sess != null ? (Customer) sess.getAttribute("customer") : null;
        if (c == null) resp.sendRedirect(req.getContextPath() + "/auth/login");
        return c;
    }
}
