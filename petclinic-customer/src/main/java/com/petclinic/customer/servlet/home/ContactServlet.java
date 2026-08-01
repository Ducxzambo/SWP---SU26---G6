package com.petclinic.customer.servlet.home;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import com.petclinic.backend.dao.NotificationDAO;
import com.petclinic.backend.dao.ServiceDAO;
import com.petclinic.backend.model.Customer;
import com.petclinic.backend.service.EmailService;

import java.io.IOException;

@WebServlet(urlPatterns = {"/contact"})
public class ContactServlet extends HttpServlet {

    private final ServiceDAO serviceDAO      = new ServiceDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();
    private final EmailService emailService    = new EmailService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        try {
            req.setAttribute("navCategories", serviceDAO.findAllCategoriesWithServices());
            Customer customer = attachCustomerContext(req);

            if (customer != null) {
                req.setAttribute("prefillName",  customer.getFullName());
                req.setAttribute("prefillEmail", customer.getEmail());
                req.setAttribute("prefillPhone", customer.getPhone());
            }

            req.getRequestDispatcher("/WEB-INF/views/guest/contact.jsp").forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        HttpSession session = req.getSession(true);

        String fullName = trim(req.getParameter("fullName"));
        String email    = trim(req.getParameter("email"));
        String phone    = trim(req.getParameter("phone"));
        String subject  = trim(req.getParameter("subject"));
        String message  = trim(req.getParameter("message"));

        if (fullName.isEmpty() || email.isEmpty() || subject.isEmpty() || message.isEmpty()) {
            session.setAttribute("flashError", "Vui lòng điền đầy đủ Họ tên, Email, Tiêu đề và Nội dung.");
            resp.sendRedirect(req.getContextPath() + "/contact");
            return;
        }
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            session.setAttribute("flashError", "Địa chỉ email không hợp lệ.");
            resp.sendRedirect(req.getContextPath() + "/contact");
            return;
        }

        try {
            emailService.sendContactNotification(fullName, email, phone, subject, message);

            session.setAttribute("flashSuccess",
                    "Cảm ơn " + fullName + "! Chúng tôi đã nhận được tin nhắn và sẽ phản hồi sớm nhất.");
        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("flashError", "Đã xảy ra lỗi khi gửi tin nhắn. Vui lòng thử lại sau.");
        }

        resp.sendRedirect(req.getContextPath() + "/contact");
    }

    private Customer attachCustomerContext(HttpServletRequest req) throws Exception {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("customer") != null) {
            Customer c = (Customer) session.getAttribute("customer");
            req.setAttribute("unreadCount", notificationDAO.countUnread(c.getCustomerID()));
            return c;
        }
        return null;
    }

    private String trim(String s) { return s == null ? "" : s.trim(); }
}
