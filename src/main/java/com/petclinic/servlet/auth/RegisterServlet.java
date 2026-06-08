package com.petclinic.servlet.auth;

import com.petclinic.service.AuthService;
import com.petclinic.service.AuthService.RegisterResult;
import com.petclinic.service.AuthService.SendOtpResult;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Registration flow (multi-step):
 * GET  /auth/register        → show registration form
 * POST /auth/register        → validate + send OTPs → redirect to OTP page
 * GET  /auth/register/verify → show OTP verification form
 * POST /auth/register/verify → verify OTPs + create account
 */
@WebServlet(urlPatterns = {"/auth/register", "/auth/register/verify"})
public class RegisterServlet extends HttpServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String path = req.getServletPath();
        if ("/auth/register/verify".equals(path)) {
            // Guard: must have pending registration in session
            HttpSession session = req.getSession(false);
            if (session == null || session.getAttribute("pendingReg") == null) {
                resp.sendRedirect(req.getContextPath() + "/auth/register");
                return;
            }
            String phone = (String) session.getAttribute("pendingReg_phone");
            req.setAttribute("hasPhone", phone != null && !phone.isBlank());
            req.getRequestDispatcher("/WEB-INF/views/auth/register-verify.jsp").forward(req, resp);
        } else {
            req.getRequestDispatcher("/WEB-INF/views/auth/register.jsp").forward(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        String path = req.getServletPath();

        if ("/auth/register/verify".equals(path)) {
            handleVerify(req, resp);
        } else {
            handleRegister(req, resp);
        }
    }

    // ── Step 1: collect data, send OTPs ──────────────────────────────────────
    private void handleRegister(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String fullName = req.getParameter("fullName");
        String email = req.getParameter("email");
        String phone = req.getParameter("phone");
        String password = req.getParameter("password");
        String confirm = req.getParameter("confirmPassword");

        // Basic server-side validation
        if (isEmpty(fullName) || isEmpty(email) || isEmpty(password)) {
            forwardRegisterWithError(req, resp, "Vui lòng nhập đầy đủ thông tin bắt buộc.");
            return;
        }
        if (!password.equals(confirm)) {
            forwardRegisterWithError(req, resp, "Mật khẩu xác nhận không khớp.");
            return;
        }
        if (!com.petclinic.util.PasswordUtil.isStrongPassword(password)) {
            forwardRegisterWithError(req, resp,
                    "Mật khẩu phải có ít nhất 6 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt.");
            return;
        }

        try {
            SendOtpResult result = authService.initiateRegistration(fullName, email, phone, password);
            switch (result) {
                case EMAIL_TAKEN:
                    forwardRegisterWithError(req, resp, "Email này đã được đăng ký.");
                    return;
                case PHONE_TAKEN:
                    forwardRegisterWithError(req, resp, "Số điện thoại này đã được đăng ký.");
                    return;
                case SEND_FAILED:
                    forwardRegisterWithError(req, resp, "Không thể gửi mã xác minh. Vui lòng thử lại.");
                    return;
                default:
                    break;
            }
        } catch (IllegalArgumentException e) {
            forwardRegisterWithError(req, resp, e.getMessage());
            return;
        } catch (Exception e) {
            e.printStackTrace();
            forwardRegisterWithError(req, resp, "Lỗi hệ thống, vui lòng thử lại.");
            return;
        }

        // Save pending registration data in session (NOT the password in plain text long-term)
        HttpSession session = req.getSession(true);
        session.setAttribute("pendingReg", "true");
        session.setAttribute("pendingReg_fullName", fullName);
        session.setAttribute("pendingReg_email", email);
        session.setAttribute("pendingReg_phone", phone);
        session.setAttribute("pendingReg_password", password);   // held briefly for OTP step
        session.setMaxInactiveInterval(60 * 15);                  // 15 min OTP window

        resp.sendRedirect(req.getContextPath() + "/auth/register/verify");
    }

    // ── Step 2: verify OTPs and create account ────────────────────────────────
    private void handleVerify(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("pendingReg") == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/register");
            return;
        }

        String fullName = (String) session.getAttribute("pendingReg_fullName");
        String email = (String) session.getAttribute("pendingReg_email");
        String phone = (String) session.getAttribute("pendingReg_phone");
        String password = (String) session.getAttribute("pendingReg_password");
        String emailOtp = req.getParameter("emailOtp");
        String phoneOtp = req.getParameter("phoneOtp");

        try {
            RegisterResult result = authService.completeRegistration(
                    fullName, email, phone, password, emailOtp, phoneOtp);

            switch (result) {
                case WRONG_EMAIL_OTP:
                    forwardVerifyWithError(req, resp, "Mã xác minh email không đúng hoặc đã hết hạn.", phone);
                    return;
                case WRONG_PHONE_OTP:
                    forwardVerifyWithError(req, resp, "Mã xác minh điện thoại không đúng hoặc đã hết hạn.", phone);
                    return;
                case EMAIL_TAKEN:
                    forwardVerifyWithError(req, resp, "Email này vừa được đăng ký. Vui lòng dùng email khác.", phone);
                    return;
                case PHONE_TAKEN:
                    forwardVerifyWithError(req, resp, "Số điện thoại này vừa được đăng ký.", phone);
                    return;
                case SUCCESS:
                    // Clean up session pending data
                    session.removeAttribute("pendingReg");
                    session.removeAttribute("pendingReg_fullName");
                    session.removeAttribute("pendingReg_email");
                    session.removeAttribute("pendingReg_phone");
                    session.removeAttribute("pendingReg_password");
                    session.setAttribute("flashSuccess",
                            "Đăng ký thành công! Vui lòng đăng nhập.");
                    resp.sendRedirect(req.getContextPath() + "/auth/login");
                    return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            forwardVerifyWithError(req, resp, "Lỗi hệ thống, vui lòng thử lại.", phone);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void forwardRegisterWithError(HttpServletRequest req, HttpServletResponse resp, String msg)
            throws ServletException, IOException {
        req.setAttribute("error", msg);
        req.setAttribute("fullName", req.getParameter("fullName"));
        req.setAttribute("email", req.getParameter("email"));
        req.setAttribute("phone", req.getParameter("phone"));
        req.getRequestDispatcher("/WEB-INF/views/auth/register.jsp").forward(req, resp);
    }

    private void forwardVerifyWithError(HttpServletRequest req, HttpServletResponse resp,
                                        String msg, String phone)
            throws ServletException, IOException {
        req.setAttribute("error", msg);
        req.setAttribute("hasPhone", phone != null && !phone.isBlank());
        req.getRequestDispatcher("/WEB-INF/views/auth/register-verify.jsp").forward(req, resp);
    }

    private boolean isEmpty(String s) {
        return s == null || s.isBlank();
    }
}
