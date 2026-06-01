package com.petclinic.servlet.auth;

import com.petclinic.service.AuthService;
import com.petclinic.service.AuthService.ResetResult;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

/**
 * Xử lý bước 2: User nhấn link trong email → nhập mật khẩu mới.
 */
@WebServlet("/reset-password")
public class ResetPasswordServlet extends HttpServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String token = req.getParameter("token");
        if (token == null || token.isBlank()) {
            resp.sendRedirect(req.getContextPath() + "/forgot-password");
            return;
        }
        req.setAttribute("token", token);
        req.getRequestDispatcher("/views/auth/reset-password.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String token       = req.getParameter("token");
        String newPassword = req.getParameter("newPassword");
        String confirm     = req.getParameter("confirmPassword");

        if (!newPassword.equals(confirm)) {
            req.setAttribute("token", token);
            req.setAttribute("errorMsg", "Mật khẩu xác nhận không khớp.");
            req.getRequestDispatcher("/views/auth/reset-password.jsp").forward(req, resp);
            return;
        }

        ResetResult result = authService.resetPassword(token, newPassword);
        switch (result) {
            case SUCCESS ->
                resp.sendRedirect(req.getContextPath() + "/login?reset=1");
            case WEAK_PASSWORD -> {
                req.setAttribute("token", token);
                req.setAttribute("errorMsg", "Mật khẩu tối thiểu 6 ký tự.");
                req.getRequestDispatcher("/views/auth/reset-password.jsp").forward(req, resp);
            }
            case INVALID_TOKEN, EXPIRED_TOKEN -> {
                req.setAttribute("errorMsg", "Link đã hết hạn hoặc không hợp lệ. Vui lòng thử lại.");
                req.getRequestDispatcher("/views/auth/forgot-password.jsp").forward(req, resp);
            }
        }
    }
}
