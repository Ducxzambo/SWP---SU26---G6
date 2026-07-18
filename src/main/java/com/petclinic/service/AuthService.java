package com.petclinic.service;

import com.petclinic.dao.CustomerDAO;
import com.petclinic.model.Customer;
import com.petclinic.util.OtpStore;
import com.petclinic.util.OtpUtil;
import com.petclinic.util.PasswordUtil;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

public class AuthService {

    private final CustomerDAO customerDAO = new CustomerDAO();

    // ══════════════════════════════════════════════════════════════════════════
    //  LOGIN
    // ══════════════════════════════════════════════════════════════════════════

    public Customer login(String identifier, String rawPassword) throws SQLException {
        Customer customer = identifier.contains("@")
                ? customerDAO.findByEmail(identifier)
                : customerDAO.findByPhone(identifier);

        if (customer == null) return null;
        if (!PasswordUtil.verifyPassword(rawPassword, customer.getPasswordHash())) return null;
        return customer;
    }

    /**
     * Generate a new Remember-Me token, persist it into Customers table, return the token string.
     */
    public String createRememberMeToken(int customerId) throws SQLException {
        String token = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiredTime = LocalDateTime.now().plusDays(30);
        customerDAO.saveRememberMeToken(customerId, token, expiredTime);
        return token;
    }

    /**
     * Resolve a Remember-Me cookie token to a Customer.
     * Returns null if token not found or expired.
     */
    public Customer resolveRememberMeToken(String token) throws SQLException {
        Customer customer = customerDAO.findByRememberMeToken(token);
        if (customer == null) return null;
        if (customer.getTokenExpiredTime() == null ||
                LocalDateTime.now().isAfter(customer.getTokenExpiredTime())) {
            customerDAO.clearRememberMeToken(customer.getCustomerID());
            return null;
        }
        return customer;
    }

    /**
     * Clear Remember-Me token by the raw token string (on logout).
     */
    public void invalidateRememberMeToken(String token) throws SQLException {
        if (token == null) return;
        Customer customer = customerDAO.findByRememberMeToken(token);
        if (customer != null) customerDAO.clearRememberMeToken(customer.getCustomerID());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  REGISTER  (Step 1 – send OTPs)
    // ══════════════════════════════════════════════════════════════════════════

    public enum SendOtpResult { OK, EMAIL_TAKEN, PHONE_TAKEN, SEND_FAILED }

    /**
     * Validate data, check uniqueness, send OTP to email only.
     * Phone is required (format-only validation, no SMS sent).
     */
    public SendOtpResult initiateRegistration(String fullName, String email,
                                              String phone, String rawPassword)
            throws SQLException {

        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Phone number is required.");
        }
        if (!PasswordUtil.isStrongPassword(rawPassword)) {
            throw new IllegalArgumentException("Password does not meet strength requirements.");
        }
        if (customerDAO.existsByEmail(email)) return SendOtpResult.EMAIL_TAKEN;
        if (customerDAO.existsByPhone(phone)) return SendOtpResult.PHONE_TAKEN;

        // Send email OTP (only channel)
        try {
            String emailOtp = OtpUtil.generateOtp();
            OtpStore.save(otpKey(email, "reg-email"), emailOtp);
            OtpUtil.sendOtpEmail(email, emailOtp, "register");
        } catch (Exception e) {
            e.printStackTrace();
            return SendOtpResult.SEND_FAILED;
        }

        return SendOtpResult.OK;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  REGISTER  (Step 2 – verify email OTP and create account)
    // ══════════════════════════════════════════════════════════════════════════

    public enum RegisterResult { SUCCESS, WRONG_EMAIL_OTP, EMAIL_TAKEN, PHONE_TAKEN }

    public RegisterResult completeRegistration(String fullName, String email,
                                               String phone, String rawPassword,
                                               String emailOtp)
            throws SQLException {

        // Double-check uniqueness (edge case: registered while user was filling OTP)
        if (customerDAO.existsByEmail(email)) return RegisterResult.EMAIL_TAKEN;
        if (customerDAO.existsByPhone(phone)) return RegisterResult.PHONE_TAKEN;

        // Verify email OTP
        if (!OtpStore.verify(otpKey(email, "reg-email"), emailOtp))
            return RegisterResult.WRONG_EMAIL_OTP;

        // Persist
        Customer customer = new Customer(fullName, email, phone,
                PasswordUtil.hashPassword(rawPassword));
        customerDAO.insert(customer);
        return RegisterResult.SUCCESS;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FORGOT PASSWORD  (Step 1 – send OTP via email only)
    // ══════════════════════════════════════════════════════════════════════════

    public enum ForgotResult { OK, USER_NOT_FOUND, INVALID_FORMAT, SEND_FAILED }

    public ForgotResult initiateForgotPassword(String email) throws SQLException {
        Customer customer = customerDAO.findByEmail(email);
        if (customer == null) return ForgotResult.USER_NOT_FOUND;

        String otp = OtpUtil.generateOtp();
        try {
            OtpStore.save(otpKey(email, "forgot"), otp);
            OtpUtil.sendOtpEmail(email, otp, "forgot");
        } catch (Exception e) {
            e.printStackTrace();
            return ForgotResult.SEND_FAILED;
        }
        return ForgotResult.OK;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FORGOT PASSWORD  (Step 2 – verify OTP)
    // ══════════════════════════════════════════════════════════════════════════

    public boolean verifyForgotOtp(String email, String inputOtp) {
        return OtpStore.verify(otpKey(email, "forgot"), inputOtp);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FORGOT PASSWORD  (Step 3 – reset password)
    // ══════════════════════════════════════════════════════════════════════════

    public enum ResetResult { SUCCESS, USER_NOT_FOUND, WEAK_PASSWORD, NOT_VERIFIED }

    public ResetResult resetPassword(String email, String rawPassword,
                                     boolean isVerified) throws SQLException {
        if (!isVerified) return ResetResult.NOT_VERIFIED;
        if (!PasswordUtil.isStrongPassword(rawPassword)) return ResetResult.WEAK_PASSWORD;

        Customer customer = customerDAO.findByEmail(email);
        if (customer == null) return ResetResult.USER_NOT_FOUND;

        customerDAO.updatePassword(customer.getCustomerID(),
                PasswordUtil.hashPassword(rawPassword));
        return ResetResult.SUCCESS;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String otpKey(String identifier, String purpose) {
        return identifier.trim().toLowerCase() + ":" + purpose;
    }
}