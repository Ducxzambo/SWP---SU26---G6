package com.petclinic.servlet.auth;

import com.petclinic.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

/**
 * Xử lý bước 1: Nhập email để yêu cầu link reset.
 */
@WebServlet("/forgot-password")
public class ForgotPasswordServlet extends HttpServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.getRequestDispatcher("/views/auth/forgot-password.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String email   = req.getParameter("email");
        String baseUrl = req.getScheme() + "://" + req.getServerName()
                       + ":" + req.getServerPort() + req.getContextPath();

        // Luôn hiển thị thông báo thành công (tránh user enumeration)
        authService.requestPasswordReset(email, baseUrl);
        req.setAttribute("successMsg",
            "Nếu email tồn tại, bạn sẽ nhận được hướng dẫn trong vài phút.");
        req.getRequestDispatcher("/views/auth/forgot-password.jsp").forward(req, resp);
    }
}
