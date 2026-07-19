package com.petclinic.service;

import com.petclinic.dao.CustomerDAO;
import com.petclinic.model.Customer;
import com.petclinic.util.OtpStore;
import com.petclinic.util.OtpUtil;
import com.petclinic.util.PasswordUtil;

import java.sql.SQLException;

/**
 * Business logic for the customer account/profile page:
 *   - update full name
 *   - update phone number
 *   - set email ONCE (2-step: send OTP to the NEW email → verify → apply) —
 *     locked permanently after the first successful set, cannot be changed
 *   - change password (no current-password check; user is already
 *     authenticated via session)
 */
public class ProfileService {

    private final CustomerDAO customerDAO = new CustomerDAO();

    // ══════════════════════════════════════════════════════════════════════════
    //  FULL NAME
    // ══════════════════════════════════════════════════════════════════════════

    public void updateFullName(int customerId, String fullName) throws SQLException {
        customerDAO.updateFullName(customerId, fullName);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PHONE
    // ══════════════════════════════════════════════════════════════════════════

    public enum PhoneUpdateResult { OK, PHONE_TAKEN }

    public PhoneUpdateResult updatePhone(int customerId, String phone) throws SQLException {
        if (customerDAO.existsByPhoneExcluding(phone, customerId)) return PhoneUpdateResult.PHONE_TAKEN;
        customerDAO.updatePhone(customerId, phone);
        return PhoneUpdateResult.OK;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  EMAIL  (Step 1 – send OTP to the new email)
    //  Email bị "fix cứng": chỉ cho phép thiết lập MỘT LẦN khi tài khoản chưa
    //  có email. Một khi đã có email, không cho đổi sang email khác nữa.
    // ══════════════════════════════════════════════════════════════════════════

    public enum EmailChangeStart { OK, ALREADY_SET, SAME_AS_CURRENT, EMAIL_TAKEN, SEND_FAILED }

    public EmailChangeStart initiateEmailChange(int customerId, String currentEmail, String newEmail)
            throws SQLException {

        if (currentEmail != null && !currentEmail.isBlank()) return EmailChangeStart.ALREADY_SET;
        if (newEmail.equalsIgnoreCase(currentEmail)) return EmailChangeStart.SAME_AS_CURRENT;
        if (customerDAO.existsByEmailExcluding(newEmail, customerId)) return EmailChangeStart.EMAIL_TAKEN;

        try {
            String otp = OtpUtil.generateOtp();
            OtpStore.save(otpKey(newEmail, "change-email"), otp);
            OtpUtil.sendOtpEmail(newEmail, otp, "change-email");
        } catch (Exception e) {
            e.printStackTrace();
            return EmailChangeStart.SEND_FAILED;
        }
        return EmailChangeStart.OK;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  EMAIL  (Step 2 – verify OTP and apply)
    // ══════════════════════════════════════════════════════════════════════════

    public enum EmailChangeComplete { SUCCESS, WRONG_OTP, EMAIL_TAKEN, ALREADY_SET }

    public EmailChangeComplete completeEmailChange(int customerId, String newEmail, String otp)
            throws SQLException {

        // Double-check: email vẫn phải đang trống tại thời điểm xác nhận (edge
        // case: email được thiết lập ở một phiên khác trong lúc OTP đang chờ)
        Customer current = customerDAO.findById(customerId);
        if (current != null && current.getEmail() != null && !current.getEmail().isBlank()) {
            return EmailChangeComplete.ALREADY_SET;
        }

        // Double-check uniqueness (edge case: email taken while OTP was pending)
        if (customerDAO.existsByEmailExcluding(newEmail, customerId)) return EmailChangeComplete.EMAIL_TAKEN;

        if (!OtpStore.verify(otpKey(newEmail, "change-email"), otp)) return EmailChangeComplete.WRONG_OTP;

        customerDAO.updateEmail(customerId, newEmail);
        return EmailChangeComplete.SUCCESS;
    }

    /** Re-send the OTP for a pending email change. */
    public boolean resendEmailChangeOtp(String newEmail) {
        try {
            String otp = OtpUtil.generateOtp();
            OtpStore.save(otpKey(newEmail, "change-email"), otp);
            OtpUtil.sendOtpEmail(newEmail, otp, "change-email");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PASSWORD
    // ══════════════════════════════════════════════════════════════════════════

    public enum PasswordChangeResult { SUCCESS, WEAK_PASSWORD, SAME_AS_CURRENT }

    /**
     * Đổi mật khẩu cho khách hàng đã đăng nhập. Không yêu cầu nhập lại mật
     * khẩu hiện tại — người dùng đã được xác thực qua session ở thời điểm này.
     */
    public PasswordChangeResult changePassword(Customer customer, String newPassword)
            throws SQLException {

        if (!PasswordUtil.isStrongPassword(newPassword))
            return PasswordChangeResult.WEAK_PASSWORD;
        if (PasswordUtil.verifyPassword(newPassword, customer.getPasswordHash()))
            return PasswordChangeResult.SAME_AS_CURRENT;

        customerDAO.updatePassword(customer.getCustomerID(), PasswordUtil.hashPassword(newPassword));
        return PasswordChangeResult.SUCCESS;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String otpKey(String identifier, String purpose) {
        return identifier.trim().toLowerCase() + ":" + purpose;
    }
}
