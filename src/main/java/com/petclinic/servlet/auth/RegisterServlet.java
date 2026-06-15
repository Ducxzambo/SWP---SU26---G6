package com.petclinic.servlet.auth;

import com.petclinic.service.AuthService;
import com.petclinic.service.AuthService.*;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

/**
 * Registration flow (multi-step):
 *   GET  /auth/register        → show registration form
 *   POST /auth/register        → validate + send email OTP → redirect to OTP page
 *   GET  /auth/register/verify → show OTP verification form
 *   POST /auth/register/verify → verify email OTP + create account
 *
 * Phone: format-only validation (10 digits, starts with 0). No SMS sent.
 */
@WebServlet(urlPatterns = {"/auth/register", "/auth/register/verify"})
public class RegisterServlet extends HttpServlet {

    private final AuthService authService = new AuthService();

    // Regex: email cơ bản, phone Việt Nam (10 chữ số, bắt đầu bằng 0)
    private static final java.util.regex.Pattern EMAIL_RE =
            java.util.regex.Pattern.compile("^[\\w.+\\-]+@[\\w\\-]+(\\.[\\w\\-]+)+$");
    private static final java.util.regex.Pattern PHONE_RE =
            java.util.regex.Pattern.compile("^0\\d{9}$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String path = req.getServletPath();
        if ("/auth/register/verify".equals(path)) {
            HttpSession session = req.getSession(false);
            if (session == null || session.getAttribute("pendingReg") == null) {
                resp.sendRedirect(req.getContextPath() + "/auth/register");
                return;
            }
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

    // ── Step 1: validate → send email OTP ────────────────────────────────────
    private void handleRegister(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String fullName = req.getParameter("fullName");
        String email    = req.getParameter("email");
        String phone    = req.getParameter("phone");
        String password = req.getParameter("password");
        String confirm  = req.getParameter("confirmPassword");

        // Required fields
        if (isEmpty(fullName) || isEmpty(email) || isEmpty(password)) {
            forwardRegisterWithError(req, resp, "Vui lòng nhập đầy đủ thông tin bắt buộc.");
            return;
        }
        // Email format
        if (!EMAIL_RE.matcher(email.trim()).matches()) {
            forwardRegisterWithError(req, resp, "Email không đúng định dạng.");
            return;
        }
        // Password match
        if (!password.equals(confirm)) {
            forwardRegisterWithError(req, resp, "Mật khẩu xác nhận không khớp.");
            return;
        }
        // Password strength
        if (!com.petclinic.util.PasswordUtil.isStrongPassword(password)) {
            forwardRegisterWithError(req, resp,
                    "Mật khẩu phải có ít nhất 6 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt.");
            return;
        }
        // Phone format (optional field – only validate if provided)
        if (!isEmpty(phone) && !PHONE_RE.matcher(phone.trim()).matches()) {
            forwardRegisterWithError(req, resp,
                    "Số điện thoại không hợp lệ. Vui lòng nhập đúng 10 chữ số, bắt đầu bằng 0.");
            return;
        }

        try {
            SendOtpResult result = authService.initiateRegistration(
                    fullName, email.trim(), isEmpty(phone) ? null : phone.trim(), password);
            switch (result) {
                case EMAIL_TAKEN:
                    forwardRegisterWithError(req, resp, "Email này đã được đăng ký."); return;
                case PHONE_TAKEN:
                    forwardRegisterWithError(req, resp, "Số điện thoại này đã được đăng ký."); return;
                case SEND_FAILED:
                    forwardRegisterWithError(req, resp, "Không thể gửi mã xác minh qua email. Vui lòng thử lại."); return;
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

        HttpSession session = req.getSession(true);
        session.setAttribute("pendingReg",          "true");
        session.setAttribute("pendingReg_fullName", fullName);
        session.setAttribute("pendingReg_email",    email.trim());
        session.setAttribute("pendingReg_phone",    isEmpty(phone) ? null : phone.trim());
        session.setAttribute("pendingReg_password", password);
        session.setMaxInactiveInterval(60 * 15); // 15 min OTP window

        resp.sendRedirect(req.getContextPath() + "/auth/register/verify");
    }

    // ── Step 2: verify email OTP → create account ────────────────────────────
    private void handleVerify(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("pendingReg") == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/register");
            return;
        }

        String fullName = (String) session.getAttribute("pendingReg_fullName");
        String email    = (String) session.getAttribute("pendingReg_email");
        String phone    = (String) session.getAttribute("pendingReg_phone");
        String password = (String) session.getAttribute("pendingReg_password");
        String emailOtp = req.getParameter("emailOtp");

        if (isEmpty(emailOtp) || emailOtp.trim().length() != 6) {
            req.setAttribute("error", "Vui lòng nhập đủ 6 chữ số của mã xác minh.");
            req.getRequestDispatcher("/WEB-INF/views/auth/register-verify.jsp").forward(req, resp);
            return;
        }

        try {
            RegisterResult result = authService.completeRegistration(
                    fullName, email, phone, password, emailOtp.trim());

            switch (result) {
                case WRONG_EMAIL_OTP:
                    req.setAttribute("error", "Mã xác minh không đúng hoặc đã hết hạn.");
                    req.getRequestDispatcher("/WEB-INF/views/auth/register-verify.jsp").forward(req, resp);
                    return;
                case EMAIL_TAKEN:
                    req.setAttribute("error", "Email này vừa được đăng ký. Vui lòng dùng email khác.");
                    req.getRequestDispatcher("/WEB-INF/views/auth/register-verify.jsp").forward(req, resp);
                    return;
                case PHONE_TAKEN:
                    req.setAttribute("error", "Số điện thoại này vừa được đăng ký.");
                    req.getRequestDispatcher("/WEB-INF/views/auth/register-verify.jsp").forward(req, resp);
                    return;
                case SUCCESS:
                    session.removeAttribute("pendingReg");
                    session.removeAttribute("pendingReg_fullName");
                    session.removeAttribute("pendingReg_email");
                    session.removeAttribute("pendingReg_phone");
                    session.removeAttribute("pendingReg_password");
                    session.setAttribute("flashSuccess", "Đăng ký thành công! Vui lòng đăng nhập.");
                    resp.sendRedirect(req.getContextPath() + "/auth/login");
                    return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Lỗi hệ thống, vui lòng thử lại.");
            req.getRequestDispatcher("/WEB-INF/views/auth/register-verify.jsp").forward(req, resp);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void forwardRegisterWithError(HttpServletRequest req, HttpServletResponse resp, String msg)
            throws ServletException, IOException {
        req.setAttribute("error",    msg);
        req.setAttribute("fullName", req.getParameter("fullName"));
        req.setAttribute("email",    req.getParameter("email"));
        req.setAttribute("phone",    req.getParameter("phone"));
        req.getRequestDispatcher("/WEB-INF/views/auth/register.jsp").forward(req, resp);
    }

    private boolean isEmpty(String s) { return s == null || s.isBlank(); }
}