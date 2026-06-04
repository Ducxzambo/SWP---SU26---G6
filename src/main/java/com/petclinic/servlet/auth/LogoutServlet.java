package com.petclinic.servlet.auth;

import com.petclinic.service.AuthService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

@WebServlet("/auth/logout")
public class LogoutServlet extends HttpServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Invalidate Remember-Me token in DB
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("rememberMe".equals(c.getName())) {
                    try {
                        authService.invalidateRememberMeToken(c.getValue());
                    } catch (Exception ignored) {
                    }
                    // Clear cookie in browser
                    Cookie clear = new Cookie("rememberMe", "");
                    clear.setMaxAge(0);
                    clear.setPath("/");
                    resp.addCookie(clear);
                    break;
                }
            }
        }

        // Invalidate session
        HttpSession session = req.getSession(false);
        if (session != null) session.invalidate();

        resp.sendRedirect(req.getContextPath() + "/auth/login");
    }
}
