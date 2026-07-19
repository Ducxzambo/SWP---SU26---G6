package com.petclinic.servlet.profile;

import com.petclinic.dao.CustomerDAO;
import com.petclinic.dao.NotificationDAO;
import com.petclinic.dao.ServiceDAO;
import com.petclinic.model.Customer;
import com.petclinic.service.ProfileService;
import com.petclinic.service.ProfileService.*;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;

/**
 * URL map:
 *   GET  /profile               → show account/profile page
 *   POST /profile/name          → update full name
 *   POST /profile/phone         → update phone number
 *   POST /profile/email         → step 1: validate new email + send OTP
 *                                  (only allowed while account has no email yet — locked once set)
 *   GET  /profile/email/verify  → show OTP form for pending email change
 *   POST /profile/email/verify  → step 2: verify OTP + apply new email
 *   POST /profile/email/resend  → resend OTP for pending email change (JSON)
 *   POST /profile/password      → change password (no current-password check required)
 */
@WebServlet(urlPatterns = {
        "/profile", "/profile/name", "/profile/phone",
        "/profile/email", "/profile/email/verify", "/profile/email/resend",
        "/profile/password"
})
public class ProfileServlet extends HttpServlet {

    private final CustomerDAO     customerDAO    = new CustomerDAO();
    private final ServiceDAO      serviceDAO     = new ServiceDAO();
    private final NotificationDAO notifDAO       = new NotificationDAO();
    private final ProfileService  profileService = new ProfileService();

    private static final java.util.regex.Pattern EMAIL_RE =
            java.util.regex.Pattern.compile("^[\\w.+\\-]+@[\\w\\-]+(\\.[\\w\\-]+)+$");
    private static final java.util.regex.Pattern PHONE_RE =
            java.util.regex.Pattern.compile("^0\\d{9}$");

    // ── GET ──────────────────────────────────────────────────────────────────
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Customer customer = requireLogin(req, resp);
        if (customer == null) return;
        try {
            switch (req.getServletPath()) {
                case "/profile":              handleShow(req, resp, customer); break;
                case "/profile/email/verify": handleShowVerifyEmail(req, resp, customer); break;
                default: resp.sendRedirect(req.getContextPath() + "/profile");
            }
        } catch (Exception e) { e.printStackTrace(); throw new ServletException(e); }
    }

    // ── POST ─────────────────────────────────────────────────────────────────
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");

        if ("/profile/email/resend".equals(req.getServletPath())) {
            if (requireLogin(req, resp) == null) return;
            handleResendEmailOtp(req, resp);
            return;
        }

        Customer customer = requireLogin(req, resp);
        if (customer == null) return;
        try {
            switch (req.getServletPath()) {
                case "/profile/name":         handleUpdateName(req, resp, customer);          break;
                case "/profile/phone":        handleUpdatePhone(req, resp, customer);         break;
                case "/profile/email":        handleInitiateEmailChange(req, resp, customer); break;
                case "/profile/email/verify": handleCompleteEmailChange(req, resp, customer); break;
                case "/profile/password":     handleChangePassword(req, resp, customer);      break;
                default: resp.sendRedirect(req.getContextPath() + "/profile");
            }
        } catch (Exception e) { e.printStackTrace(); throw new ServletException(e); }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SHOW PAGE
    // ══════════════════════════════════════════════════════════════════════════
    private void handleShow(HttpServletRequest req, HttpServletResponse resp, Customer customer)
            throws Exception {
        renderProfile(req, resp, customer, null, null, null, null, null);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UPDATE FULL NAME
    // ══════════════════════════════════════════════════════════════════════════
    private void handleUpdateName(HttpServletRequest req, HttpServletResponse resp, Customer customer)
            throws Exception {
        String fullName = trim(req.getParameter("fullName"));
        if (fullName.isEmpty()) {
            renderProfile(req, resp, customer, "Họ và tên không được để trống.", "name", fullName, null, null);
            return;
        }
        profileService.updateFullName(customer.getCustomerID(), fullName);
        refreshSessionCustomer(req, customer.getCustomerID());
        req.getSession().setAttribute("flashSuccess", "Đã cập nhật họ và tên.");
        resp.sendRedirect(req.getContextPath() + "/profile");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UPDATE PHONE
    // ══════════════════════════════════════════════════════════════════════════
    private void handleUpdatePhone(HttpServletRequest req, HttpServletResponse resp, Customer customer)
            throws Exception {
        String phone = trim(req.getParameter("phone"));
        if (phone.isEmpty()) {
            renderProfile(req, resp, customer, "Số điện thoại không được để trống.", "phone", null, phone, null);
            return;
        }
        if (!PHONE_RE.matcher(phone).matches()) {
            renderProfile(req, resp, customer,
                    "Số điện thoại không hợp lệ. Vui lòng nhập đúng 10 chữ số, bắt đầu bằng 0.",
                    "phone", null, phone, null);
            return;
        }

        PhoneUpdateResult result = profileService.updatePhone(customer.getCustomerID(), phone);
        if (result == PhoneUpdateResult.PHONE_TAKEN) {
            renderProfile(req, resp, customer,
                    "Số điện thoại này đã được sử dụng bởi tài khoản khác.", "phone", null, phone, null);
            return;
        }

        refreshSessionCustomer(req, customer.getCustomerID());
        req.getSession().setAttribute("flashSuccess", "Đã cập nhật số điện thoại.");
        resp.sendRedirect(req.getContextPath() + "/profile");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  EMAIL CHANGE – Step 1: validate + send OTP to new email
    // ══════════════════════════════════════════════════════════════════════════
    private void handleInitiateEmailChange(HttpServletRequest req, HttpServletResponse resp, Customer customer)
            throws Exception {
        String newEmail = trim(req.getParameter("newEmail"));
        if (newEmail.isEmpty()) {
            renderProfile(req, resp, customer, "Vui lòng nhập email mới.", "email", null, null, newEmail);
            return;
        }
        if (!EMAIL_RE.matcher(newEmail).matches()) {
            renderProfile(req, resp, customer, "Email không đúng định dạng.", "email", null, null, newEmail);
            return;
        }

        EmailChangeStart result = profileService.initiateEmailChange(
                customer.getCustomerID(), customer.getEmail(), newEmail);

        switch (result) {
            case ALREADY_SET:
                renderProfile(req, resp, customer,
                        "Email của tài khoản đã được thiết lập và không thể thay đổi.", "email", null, null, newEmail);
                return;
            case SAME_AS_CURRENT:
                renderProfile(req, resp, customer, "Đây đã là email hiện tại của bạn.", "email", null, null, newEmail);
                return;
            case EMAIL_TAKEN:
                renderProfile(req, resp, customer,
                        "Email này đã được đăng ký bởi tài khoản khác.", "email", null, null, newEmail);
                return;
            case SEND_FAILED:
                renderProfile(req, resp, customer,
                        "Không thể gửi mã xác minh qua email. Vui lòng thử lại.", "email", null, null, newEmail);
                return;
            default:
                break;
        }

        HttpSession session = req.getSession(true);
        session.setAttribute("pendingEmailChange", newEmail);
        resp.sendRedirect(req.getContextPath() + "/profile/email/verify");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  EMAIL CHANGE – show OTP form
    // ══════════════════════════════════════════════════════════════════════════
    private void handleShowVerifyEmail(HttpServletRequest req, HttpServletResponse resp, Customer customer)
            throws Exception {
        HttpSession session = req.getSession(false);
        String pending = session != null ? (String) session.getAttribute("pendingEmailChange") : null;
        if (pending == null) {
            resp.sendRedirect(req.getContextPath() + "/profile");
            return;
        }
        setCommonAttrs(req, customer);
        req.setAttribute("pendingEmail", pending);
        req.getRequestDispatcher("/WEB-INF/views/customer/profile/verify-email.jsp").forward(req, resp);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  EMAIL CHANGE – Step 2: verify OTP + apply
    // ══════════════════════════════════════════════════════════════════════════
    private void handleCompleteEmailChange(HttpServletRequest req, HttpServletResponse resp, Customer customer)
            throws Exception {
        HttpSession session = req.getSession(false);
        String pending = session != null ? (String) session.getAttribute("pendingEmailChange") : null;
        if (pending == null) {
            resp.sendRedirect(req.getContextPath() + "/profile");
            return;
        }

        String otp = trim(req.getParameter("otp"));
        if (otp.isEmpty() || otp.length() != 6) {
            setCommonAttrs(req, customer);
            req.setAttribute("pendingEmail", pending);
            req.setAttribute("error", "Vui lòng nhập đủ 6 chữ số của mã xác minh.");
            req.getRequestDispatcher("/WEB-INF/views/customer/profile/verify-email.jsp").forward(req, resp);
            return;
        }

        EmailChangeComplete result = profileService.completeEmailChange(customer.getCustomerID(), pending, otp);

        switch (result) {
            case WRONG_OTP:
                setCommonAttrs(req, customer);
                req.setAttribute("pendingEmail", pending);
                req.setAttribute("error", "Mã xác minh không đúng hoặc đã hết hạn.");
                req.getRequestDispatcher("/WEB-INF/views/customer/profile/verify-email.jsp").forward(req, resp);
                return;
            case EMAIL_TAKEN:
                session.removeAttribute("pendingEmailChange");
                session.setAttribute("flashError",
                        "Email này vừa được đăng ký bởi tài khoản khác. Vui lòng thử email khác.");
                resp.sendRedirect(req.getContextPath() + "/profile");
                return;
            case ALREADY_SET:
                session.removeAttribute("pendingEmailChange");
                session.setAttribute("flashError", "Email của tài khoản đã được thiết lập và không thể thay đổi.");
                resp.sendRedirect(req.getContextPath() + "/profile");
                return;
            case SUCCESS:
                session.removeAttribute("pendingEmailChange");
                refreshSessionCustomer(req, customer.getCustomerID());
                session.setAttribute("flashSuccess", "Cập nhật email thành công!");
                resp.sendRedirect(req.getContextPath() + "/profile");
                return;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  EMAIL CHANGE – resend OTP (JSON)
    // ══════════════════════════════════════════════════════════════════════════
    private void handleResendEmailOtp(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        String pending = session != null ? (String) session.getAttribute("pendingEmailChange") : null;
        boolean ok = pending != null && profileService.resendEmailChangeOtp(pending);

        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write("{\"ok\":" + ok + "}");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CHANGE PASSWORD
    // ══════════════════════════════════════════════════════════════════════════
    private void handleChangePassword(HttpServletRequest req, HttpServletResponse resp, Customer customer)
            throws Exception {
        String newPassword     = req.getParameter("newPassword");
        String confirmPassword = req.getParameter("confirmPassword");

        if (isEmpty(newPassword)) {
            renderProfile(req, resp, customer, "Vui lòng nhập đầy đủ thông tin mật khẩu.", "password", null, null, null);
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            renderProfile(req, resp, customer, "Mật khẩu xác nhận không khớp.", "password", null, null, null);
            return;
        }

        PasswordChangeResult result = profileService.changePassword(customer, newPassword);
        switch (result) {
            case WEAK_PASSWORD:
                renderProfile(req, resp, customer,
                        "Mật khẩu mới phải có ít nhất 6 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt.",
                        "password", null, null, null);
                return;
            case SAME_AS_CURRENT:
                renderProfile(req, resp, customer,
                        "Mật khẩu mới phải khác mật khẩu hiện tại.", "password", null, null, null);
                return;
            case SUCCESS:
                refreshSessionCustomer(req, customer.getCustomerID());
                req.getSession().setAttribute("flashSuccess", "Đã đổi mật khẩu thành công.");
                resp.sendRedirect(req.getContextPath() + "/profile");
                return;
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void renderProfile(HttpServletRequest req, HttpServletResponse resp, Customer customer,
                                String errorMsg, String errorSection,
                                String nameInput, String phoneInput, String newEmailInput)
            throws Exception {

        setCommonAttrs(req, customer);
        req.setAttribute("customer", customer);
        req.setAttribute("profileIncomplete", isEmpty(customer.getEmail()) || isEmpty(customer.getPhone()));
        req.setAttribute("displayName",     nameInput  != null ? nameInput  : customer.getFullName());
        req.setAttribute("displayPhone",    phoneInput != null ? phoneInput : customer.getPhone());
        req.setAttribute("displayNewEmail", newEmailInput != null ? newEmailInput : "");
        req.setAttribute("displayInitial",  avatarInitial(customer.getFullName()));

        if (errorMsg != null) {
            req.setAttribute("error", errorMsg);
            req.setAttribute("errorSection", errorSection);
        }

        req.getRequestDispatcher("/WEB-INF/views/customer/profile/index.jsp").forward(req, resp);
    }

    private void refreshSessionCustomer(HttpServletRequest req, int customerId) throws SQLException {
        Customer fresh = customerDAO.findById(customerId);
        if (fresh != null) req.getSession().setAttribute("customer", fresh);
    }

    private void setCommonAttrs(HttpServletRequest req, Customer customer) throws Exception {
        req.setAttribute("navCategories", serviceDAO.findAllCategoriesWithServices());
        req.setAttribute("unreadCount", notifDAO.countUnread(customer.getCustomerID()));
    }

    private Customer requireLogin(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession sess = req.getSession(false);
        Customer c = sess != null ? (Customer) sess.getAttribute("customer") : null;
        if (c == null) {
            req.getSession(true).setAttribute("redirectAfterLogin",
                    req.getRequestURI() + (req.getQueryString() != null ? "?" + req.getQueryString() : ""));
            resp.sendRedirect(req.getContextPath() + "/auth/login");
        }
        return c;
    }

    private boolean isEmpty(String s) { return s == null || s.isBlank(); }
    private String  trim(String s)    { return s != null ? s.trim() : ""; }

    /** Chữ cái đầu tên hiển thị trên avatar sidebar của trang profile. */
    private String avatarInitial(String fullName) {
        if (fullName == null || fullName.isBlank()) return "?";
        return fullName.trim().substring(0, 1).toUpperCase();
    }
}
