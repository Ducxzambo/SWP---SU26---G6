package com.petclinic.customer.servlet.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import com.petclinic.backend.model.Customer;
import com.petclinic.backend.service.AuthService;

import java.io.IOException;

@WebServlet("/auth/login")
public class LoginServlet extends HttpServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Already logged in?
        if (req.getSession(false) != null &&
            req.getSession(false).getAttribute("customer") != null) {
            Customer c = (Customer) req.getSession(false).getAttribute("customer");
            redirectAfterAuth(req, resp, req.getSession(false), c);
            return;
        }

        // Check Remember-Me cookie
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("rememberMe".equals(c.getName()) && c.getValue() != null) {
                    try {
                        Customer customer = authService.resolveRememberMeToken(c.getValue());
                        if (customer != null) {
                            HttpSession session = req.getSession(true);
                            session.setAttribute("customer", customer);
                            redirectAfterAuth(req, resp, session, customer);
                            return;
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        String identifier  = req.getParameter("identifier");
        String password    = req.getParameter("password");
        String rememberMe  = req.getParameter("rememberMe");
        if (identifier == null || identifier.isBlank() ||
            password   == null || password.isBlank()) {
            forwardWithError(req, resp, "Vui lòng nhập đầy đủ thông tin.");
            return;
        }

        try {
            Customer customer = authService.login(identifier.trim(), password);
            if (customer == null) {
                forwardWithError(req, resp, "Email/Số điện thoại hoặc mật khẩu không đúng.");
                return;
            }

            HttpSession session = req.getSession(true);
            session.setAttribute("customer", customer);
            session.setMaxInactiveInterval(60 * 60 * 8); // 8 hours

            if ("on".equals(rememberMe)) {
                String token = authService.createRememberMeToken(customer.getCustomerID());
                Cookie cookie = new Cookie("rememberMe", token);
                cookie.setMaxAge(60 * 60 * 24 * 30); // 30 days
                cookie.setHttpOnly(true);
                cookie.setPath("/");
                resp.addCookie(cookie);
            }

            redirectAfterAuth(req, resp, session, customer);

        } catch (Exception e) {
            e.printStackTrace();
            forwardWithError(req, resp, "Đã xảy ra lỗi, vui lòng thử lại.");
        }
    }

    private void redirectAfterAuth(HttpServletRequest req, HttpServletResponse resp,
                                    HttpSession session, Customer customer) throws IOException {
        if (isEmpty(customer.getEmail()) || isEmpty(customer.getPhone())) {
            resp.sendRedirect(req.getContextPath() + "/profile");
            return;
        }
        String redirectUrl = (String) session.getAttribute("redirectAfterLogin");
        if (redirectUrl != null) {
            session.removeAttribute("redirectAfterLogin");
            resp.sendRedirect(redirectUrl);
        } else {
            resp.sendRedirect(req.getContextPath() + "/home");
        }
    }

    private boolean isEmpty(String s) { return s == null || s.isBlank(); }

    private void forwardWithError(HttpServletRequest req, HttpServletResponse resp, String msg)
            throws ServletException, IOException {
        req.setAttribute("error", msg);
        req.setAttribute("identifier", req.getParameter("identifier"));
        req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, resp);
    }
}
