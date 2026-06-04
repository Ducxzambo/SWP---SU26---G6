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

    /**
     * Authenticate by email or phone.
     *
     * @return Customer on success, null on failure.
     */
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

    /**
     * Validate data, check uniqueness, send OTPs to email (and phone if provided).
     */
    public SendOtpResult initiateRegistration(String fullName, String email,
                                              String phone, String rawPassword)
            throws SQLException {

        if (!PasswordUtil.isStrongPassword(rawPassword)) {
            throw new IllegalArgumentException("Password does not meet strength requirements.");
        }
        if (customerDAO.existsByEmail(email)) return SendOtpResult.EMAIL_TAKEN;
        if (phone != null && !phone.isBlank() && customerDAO.existsByPhone(phone))
            return SendOtpResult.PHONE_TAKEN;

        // Send email OTP
        try {
            String emailOtp = OtpUtil.generateOtp();
            OtpStore.save(otpKey(email, "reg-email"), emailOtp);
            OtpUtil.sendOtpEmail(email, emailOtp, "register");
        } catch (Exception e) {
            e.printStackTrace();
            return SendOtpResult.SEND_FAILED;
        }

        // Send phone OTP (non-blocking – phone is optional)
        if (phone != null && !phone.isBlank()) {
            try {
                String phoneOtp = OtpUtil.generateOtp();
                OtpStore.save(otpKey(phone, "reg-phone"), phoneOtp);
                OtpUtil.sendOtpSms(phone, phoneOtp, "register");
            } catch (Exception e) {
                e.printStackTrace();
                // SMS failure is non-fatal for now; frontend will show partial warning
            }
        }

        return SendOtpResult.OK;
    }

    public RegisterResult completeRegistration(String fullName, String email,
                                               String phone, String rawPassword,
                                               String emailOtp, String phoneOtp)
            throws SQLException {

        // Double-check uniqueness (edge case: registered while user was filling OTPs)
        if (customerDAO.existsByEmail(email)) return RegisterResult.EMAIL_TAKEN;

        // Verify email OTP (mandatory)
        if (!OtpStore.verify(otpKey(email, "reg-email"), emailOtp))
            return RegisterResult.WRONG_EMAIL_OTP;

        // Verify phone OTP only if phone was provided
        if (phone != null && !phone.isBlank()) {
            if (customerDAO.existsByPhone(phone)) return RegisterResult.PHONE_TAKEN;
            if (!OtpStore.verify(otpKey(phone, "reg-phone"), phoneOtp))
                return RegisterResult.WRONG_PHONE_OTP;
        }

        // Persist
        Customer customer = new Customer(fullName, email,
                phone != null && phone.isBlank() ? null : phone,
                PasswordUtil.hashPassword(rawPassword));
        customerDAO.insert(customer);
        return RegisterResult.SUCCESS;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  REGISTER  (Step 2 – verify OTPs and create account)
    // ══════════════════════════════════════════════════════════════════════════

    public ForgotResult initiateForgotPassword(String identifier) throws SQLException {
        Customer customer = identifier.contains("@")
                ? customerDAO.findByEmail(identifier)
                : customerDAO.findByPhone(identifier);

        if (customer == null) return ForgotResult.USER_NOT_FOUND;

        String otp = OtpUtil.generateOtp();
        try {
            if (identifier.contains("@")) {
                OtpStore.save(otpKey(identifier, "forgot"), otp);
                OtpUtil.sendOtpEmail(identifier, otp, "forgot");
            } else {
                OtpStore.save(otpKey(identifier, "forgot"), otp);
                OtpUtil.sendOtpSms(identifier, otp, "forgot");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ForgotResult.SEND_FAILED;
        }
        return ForgotResult.OK;
    }

    public boolean verifyForgotOtp(String identifier, String inputOtp) {
        return OtpStore.verify(otpKey(identifier, "forgot"), inputOtp);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FORGOT PASSWORD  (Step 1 – send OTP)
    // ══════════════════════════════════════════════════════════════════════════

    public ResetResult resetPassword(String identifier, String rawPassword,
                                     boolean isVerified) throws SQLException {
        if (!isVerified) return ResetResult.NOT_VERIFIED;
        if (!PasswordUtil.isStrongPassword(rawPassword)) return ResetResult.WEAK_PASSWORD;

        Customer customer = identifier.contains("@")
                ? customerDAO.findByEmail(identifier)
                : customerDAO.findByPhone(identifier);
        if (customer == null) return ResetResult.USER_NOT_FOUND;

        customerDAO.updatePassword(customer.getCustomerID(),
                PasswordUtil.hashPassword(rawPassword));
        return ResetResult.SUCCESS;
    }

    private String otpKey(String identifier, String purpose) {
        return identifier.trim().toLowerCase() + ":" + purpose;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FORGOT PASSWORD  (Step 2 – verify OTP)
    // ══════════════════════════════════════════════════════════════════════════

    public enum SendOtpResult {OK, EMAIL_TAKEN, PHONE_TAKEN, SEND_FAILED}

    // ══════════════════════════════════════════════════════════════════════════
    //  FORGOT PASSWORD  (Step 3 – reset password)
    // ══════════════════════════════════════════════════════════════════════════

    public enum RegisterResult {SUCCESS, WRONG_EMAIL_OTP, WRONG_PHONE_OTP, EMAIL_TAKEN, PHONE_TAKEN}

    public enum ForgotResult {OK, USER_NOT_FOUND, SEND_FAILED}

    // ── Helpers ──────────────────────────────────────────────────────────────

    public enum ResetResult {SUCCESS, USER_NOT_FOUND, WEAK_PASSWORD, NOT_VERIFIED}
}