package com.petclinic.servlet.auth;

import com.petclinic.model.Customer;
import com.petclinic.service.AuthService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/auth/login")
public class LoginServlet extends HttpServlet {

    private final AuthService authService = new AuthService();

    // ── GET: show login page ─────────────────────────────────────────────────
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Already logged in?
        if (req.getSession(false) != null &&
            req.getSession(false).getAttribute("customer") != null) {
            resp.sendRedirect(req.getContextPath() + "/");
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
                            resp.sendRedirect(req.getContextPath() + "/");
                            return;
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, resp);
    }

    // ── POST: process login ──────────────────────────────────────────────────
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        String identifier  = req.getParameter("identifier");   // email or phone
        String password    = req.getParameter("password");
        String rememberMe  = req.getParameter("rememberMe");   // "on" if checked

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

            // Create session
            HttpSession session = req.getSession(true);
            session.setAttribute("customer", customer);
            session.setMaxInactiveInterval(60 * 60 * 8); // 8 hours

            // Remember-Me: set cookie with DB token
            if ("on".equals(rememberMe)) {
                String token = authService.createRememberMeToken(customer.getCustomerID());
                Cookie cookie = new Cookie("rememberMe", token);
                cookie.setMaxAge(60 * 60 * 24 * 30); // 30 days
                cookie.setHttpOnly(true);
                cookie.setPath("/");
                resp.addCookie(cookie);
            }

            // Redirect to originally requested page or home
            String redirectUrl = (String) session.getAttribute("redirectAfterLogin");
            if (redirectUrl != null) {
                session.removeAttribute("redirectAfterLogin");
                resp.sendRedirect(redirectUrl);
            } else {
                resp.sendRedirect(req.getContextPath() + "/");
            }

        } catch (Exception e) {
            e.printStackTrace();
            forwardWithError(req, resp, "Đã xảy ra lỗi, vui lòng thử lại.");
        }
    }

    private void forwardWithError(HttpServletRequest req, HttpServletResponse resp, String msg)
            throws ServletException, IOException {
        req.setAttribute("error", msg);
        req.setAttribute("identifier", req.getParameter("identifier"));
        req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, resp);
    }
}
