package com.petclinic.servlet.auth;

import com.petclinic.service.AuthService;
import com.petclinic.service.AuthService.RegisterResult;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private final AuthService authService = new AuthService();

    // vào GET --> form register
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.getRequestDispatcher("/views/auth/register.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        String fullName        = req.getParameter("fullName");
        String email           = req.getParameter("email");
        String phone           = req.getParameter("phone");
        String password        = req.getParameter("password");
        String confirmPassword = req.getParameter("confirmPassword");

        // Validate confirm password (client-side + server đều phải check)
        if (!password.equals(confirmPassword)) {
            req.setAttribute("errorMsg", "Mật khẩu xác nhận không khớp.");
            req.setAttribute("fullNameVal", fullName);
            req.setAttribute("emailVal", email);
            req.setAttribute("phoneVal", phone);
            req.getRequestDispatcher("/views/auth/register.jsp").forward(req, resp);
            return;
        }

        RegisterResult result = authService.register(fullName, email, phone, password);

        switch (result) {
            case SUCCESS -> resp.sendRedirect(req.getContextPath() + "/login?registered=1");
            case EMAIL_EXISTS -> {
                req.setAttribute("errorMsg", "Email này đã được đăng ký.");
                req.setAttribute("fullNameVal", fullName);
                req.setAttribute("emailVal", email);
                req.setAttribute("phoneVal", phone);
                req.getRequestDispatcher("/views/auth/register.jsp").forward(req, resp);
            }
            case INVALID_INPUT -> {
                req.setAttribute("errorMsg", "Vui lòng kiểm tra lại thông tin (mật khẩu tối thiểu 6 ký tự).");
                req.getRequestDispatcher("/views/auth/register.jsp").forward(req, resp);
            }
        }
    }
}
