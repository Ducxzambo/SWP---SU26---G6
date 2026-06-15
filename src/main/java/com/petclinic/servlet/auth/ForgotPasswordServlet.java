package com.petclinic.servlet.auth;

import com.petclinic.service.AuthService;
import com.petclinic.service.AuthService.*;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

/**
 * Forgot-password flow (3 pages):
 *   /auth/forgot            – enter email
 *   /auth/forgot/verify     – enter OTP (sent to email)
 *   /auth/forgot/reset      – enter new password
 */
@WebServlet(urlPatterns = {"/auth/forgot", "/auth/forgot/verify", "/auth/forgot/reset"})
public class ForgotPasswordServlet extends HttpServlet {

    private final AuthService authService = new AuthService();

    private static final java.util.regex.Pattern EMAIL_RE =
            java.util.regex.Pattern.compile("^[\\w.+\\-]+@[\\w\\-]+(\\.[\\w\\-]+)+$");

    // ── GET ───────────────────────────────────────────────────────────────────
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String path = req.getServletPath();
        HttpSession session = req.getSession(false);

        switch (path) {
            case "/auth/forgot/verify":
                if (session == null || session.getAttribute("forgot_email") == null) {
                    resp.sendRedirect(req.getContextPath() + "/auth/forgot"); return;
                }
                req.getRequestDispatcher("/WEB-INF/views/auth/forgot-verify.jsp").forward(req, resp);
                break;

            case "/auth/forgot/reset":
                if (session == null || !Boolean.TRUE.equals(session.getAttribute("forgot_verified"))) {
                    resp.sendRedirect(req.getContextPath() + "/auth/forgot"); return;
                }
                req.getRequestDispatcher("/WEB-INF/views/auth/forgot-reset.jsp").forward(req, resp);
                break;

            default:
                req.getRequestDispatcher("/WEB-INF/views/auth/forgot.jsp").forward(req, resp);
        }
    }

    // ── POST ──────────────────────────────────────────────────────────────────
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        String path = req.getServletPath();

        switch (path) {
            case "/auth/forgot":         handleForgotStep1(req, resp); break;
            case "/auth/forgot/verify":  handleForgotStep2(req, resp); break;
            case "/auth/forgot/reset":   handleForgotStep3(req, resp); break;
            default: resp.sendRedirect(req.getContextPath() + "/auth/forgot");
        }
    }

    // ── Step 1: nhập email ────────────────────────────────────────────────────
    private void handleForgotStep1(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String email = req.getParameter("email");
        if (email == null || email.isBlank()) {
            req.setAttribute("error", "Vui lòng nhập địa chỉ email.");
            req.getRequestDispatcher("/WEB-INF/views/auth/forgot.jsp").forward(req, resp);
            return;
        }
        email = email.trim();
        if (!EMAIL_RE.matcher(email).matches()) {
            req.setAttribute("error", "Địa chỉ email không đúng định dạng.");
            req.setAttribute("email", email);
            req.getRequestDispatcher("/WEB-INF/views/auth/forgot.jsp").forward(req, resp);
            return;
        }

        try {
            ForgotResult result = authService.initiateForgotPassword(email);
            switch (result) {
                case USER_NOT_FOUND:
                    req.setAttribute("error", "Không tìm thấy tài khoản với email này.");
                    req.setAttribute("email", email);
                    req.getRequestDispatcher("/WEB-INF/views/auth/forgot.jsp").forward(req, resp);
                    return;
                case SEND_FAILED:
                    req.setAttribute("error", "Không thể gửi mã xác minh. Vui lòng thử lại.");
                    req.setAttribute("email", email);
                    req.getRequestDispatcher("/WEB-INF/views/auth/forgot.jsp").forward(req, resp);
                    return;
                default:
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Lỗi hệ thống, vui lòng thử lại.");
            req.getRequestDispatcher("/WEB-INF/views/auth/forgot.jsp").forward(req, resp);
            return;
        }

        HttpSession session = req.getSession(true);
        session.setAttribute("forgot_email", email);
        session.setMaxInactiveInterval(60 * 15);

        resp.sendRedirect(req.getContextPath() + "/auth/forgot/verify");
    }

    // ── Step 2: xác minh OTP ─────────────────────────────────────────────────
    private void handleForgotStep2(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        String email = session != null ? (String) session.getAttribute("forgot_email") : null;
        if (email == null) { resp.sendRedirect(req.getContextPath() + "/auth/forgot"); return; }

        String otp = req.getParameter("otp");
        if (otp == null || otp.isBlank() || otp.trim().length() != 6) {
            req.setAttribute("error", "Vui lòng nhập đủ 6 chữ số của mã xác minh.");
            req.getRequestDispatcher("/WEB-INF/views/auth/forgot-verify.jsp").forward(req, resp);
            return;
        }

        boolean verified = authService.verifyForgotOtp(email, otp.trim());
        if (!verified) {
            req.setAttribute("error", "Mã xác minh không đúng hoặc đã hết hạn.");
            req.getRequestDispatcher("/WEB-INF/views/auth/forgot-verify.jsp").forward(req, resp);
            return;
        }

        session.setAttribute("forgot_verified", Boolean.TRUE);
        resp.sendRedirect(req.getContextPath() + "/auth/forgot/reset");
    }

    // ── Step 3: đặt lại mật khẩu ─────────────────────────────────────────────
    private void handleForgotStep3(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        String email     = session != null ? (String) session.getAttribute("forgot_email") : null;
        boolean verified = session != null && Boolean.TRUE.equals(session.getAttribute("forgot_verified"));

        if (email == null || !verified) {
            resp.sendRedirect(req.getContextPath() + "/auth/forgot"); return;
        }

        String password = req.getParameter("password");
        String confirm  = req.getParameter("confirmPassword");

        if (password == null || password.isBlank()) {
            req.setAttribute("error", "Vui lòng nhập mật khẩu mới.");
            req.getRequestDispatcher("/WEB-INF/views/auth/forgot-reset.jsp").forward(req, resp);
            return;
        }
        if (!password.equals(confirm)) {
            req.setAttribute("error", "Mật khẩu xác nhận không khớp.");
            req.getRequestDispatcher("/WEB-INF/views/auth/forgot-reset.jsp").forward(req, resp);
            return;
        }

        try {
            ResetResult result = authService.resetPassword(email, password, verified);
            switch (result) {
                case WEAK_PASSWORD:
                    req.setAttribute("error",
                            "Mật khẩu phải có ít nhất 6 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt.");
                    req.getRequestDispatcher("/WEB-INF/views/auth/forgot-reset.jsp").forward(req, resp);
                    return;
                case USER_NOT_FOUND:
                    req.setAttribute("error", "Tài khoản không tồn tại.");
                    req.getRequestDispatcher("/WEB-INF/views/auth/forgot-reset.jsp").forward(req, resp);
                    return;
                case SUCCESS:
                    session.removeAttribute("forgot_email");
                    session.removeAttribute("forgot_verified");
                    session.setAttribute("flashSuccess", "Đặt lại mật khẩu thành công! Vui lòng đăng nhập.");
                    resp.sendRedirect(req.getContextPath() + "/auth/login");
                    return;
                default:
                    req.setAttribute("error", "Lỗi không xác định.");
                    req.getRequestDispatcher("/WEB-INF/views/auth/forgot-reset.jsp").forward(req, resp);
            }
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Lỗi hệ thống, vui lòng thử lại.");
            req.getRequestDispatcher("/WEB-INF/views/auth/forgot-reset.jsp").forward(req, resp);
        }
    }
}