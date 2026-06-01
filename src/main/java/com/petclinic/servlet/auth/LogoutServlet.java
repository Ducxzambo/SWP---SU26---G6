package com.petclinic.servlet.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Hủy session
        HttpSession session = req.getSession(false);
        if (session != null) session.invalidate();

        // Xóa remember-me cookie
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("rememberEmail".equals(c.getName())) {
                    c.setValue("");
                    c.setMaxAge(0);
                    c.setPath(req.getContextPath());
                    resp.addCookie(c);
                }
            }
        }

        resp.sendRedirect(req.getContextPath() + "/login?logout=1");
    }
}
