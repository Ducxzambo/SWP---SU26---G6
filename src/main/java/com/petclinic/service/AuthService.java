package com.petclinic.service;

import com.petclinic.dao.UserDAO;
import com.petclinic.model.User;
import com.petclinic.util.EmailUtil;
import com.petclinic.util.PasswordUtil;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Business logic cho Authentication.
 * Servlet gọi Service, Service gọi DAO — không có JDBC trong Servlet.
 */
public class AuthService {

    private final UserDAO userDAO = new UserDAO();

    // ── Login ─────────────────────────────────────────────────────────────────

    /**
     * Xác thực đăng nhập.
     * @return User nếu hợp lệ, empty nếu sai email/password hoặc bị khóa.
     */
    public Optional<User> login(String email, String plainPassword) {
        Optional<User> opt = userDAO.findByEmail(email.trim().toLowerCase());
        if (opt.isEmpty()) return Optional.empty();

        User user = opt.get();
        if (!user.isActive()) return Optional.empty();                     // tài khoản bị khóa
        if (!PasswordUtil.verify(plainPassword, user.getPasswordHash()))
            return Optional.empty();

        return Optional.of(user);
    }

    // ── Register ──────────────────────────────────────────────────────────────

    public enum RegisterResult { SUCCESS, EMAIL_EXISTS, INVALID_INPUT }

    /**
     * Đăng ký tài khoản Customer mới.
     */
    public RegisterResult register(String fullName, String email,
                                   String phone,    String plainPassword) {
        if (fullName == null || fullName.isBlank()
         || email    == null || email.isBlank()
         || plainPassword == null || plainPassword.length() < 6) {
            return RegisterResult.INVALID_INPUT;
        }

        String normalizedEmail = email.trim().toLowerCase();
        if (userDAO.existsByEmail(normalizedEmail)) {
            return RegisterResult.EMAIL_EXISTS;
        }

        User user = new User();
        user.setFullName(fullName.trim());
        user.setEmail(normalizedEmail);
        user.setPhone(phone != null ? phone.trim() : null);
        user.setPasswordHash(PasswordUtil.hash(plainPassword));
        user.setRoleId(5); // Customer — khớp với seed data

        userDAO.insert(user);
        return RegisterResult.SUCCESS;
    }

    // ── Forgot Password ───────────────────────────────────────────────────────

    /**
     * Tạo reset token và gửi email.
     * Luôn trả về true để tránh user enumeration attack
     * (không để lộ email nào tồn tại trong hệ thống).
     */
    public boolean requestPasswordReset(String email, String baseUrl) {
        Optional<User> opt = userDAO.findByEmail(email.trim().toLowerCase());
        if (opt.isEmpty()) return true; // silent fail

        User user = opt.get();
        String token  = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(30);

        userDAO.saveResetToken(user.getUserId(), token, expiry);

        String resetLink = baseUrl + "/reset-password?token=" + token;
        try {
            EmailUtil.sendResetPasswordEmail(user.getEmail(), resetLink);
        } catch (Exception e) {
            // Log lỗi nhưng không ném ra ngoài — UX vẫn thấy "email đã gửi"
            System.err.println("[AuthService] Failed to send reset email: " + e.getMessage());
        }
        return true;
    }

    // ── Reset Password ────────────────────────────────────────────────────────

    public enum ResetResult { SUCCESS, INVALID_TOKEN, EXPIRED_TOKEN, WEAK_PASSWORD }

    /**
     * Đặt lại mật khẩu bằng token.
     */
    public ResetResult resetPassword(String token, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            return ResetResult.WEAK_PASSWORD;
        }

        Optional<User> opt = userDAO.findByValidResetToken(token);
        if (opt.isEmpty()) return ResetResult.INVALID_TOKEN;  // không tồn tại hoặc hết hạn

        User user = opt.get();
        userDAO.updatePassword(user.getUserId(), PasswordUtil.hash(newPassword));
        return ResetResult.SUCCESS;
    }
}
