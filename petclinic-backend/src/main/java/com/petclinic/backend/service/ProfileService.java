package com.petclinic.backend.service;

import com.petclinic.backend.dao.CustomerDAO;
import com.petclinic.backend.model.Customer;
import com.petclinic.backend.util.OtpStore;
import com.petclinic.backend.util.OtpUtil;
import com.petclinic.backend.util.PasswordUtil;

import java.sql.SQLException;

public class ProfileService {

    private final CustomerDAO customerDAO = new CustomerDAO();

    public void updateFullName(int customerId, String fullName) throws SQLException {
        customerDAO.updateFullName(customerId, fullName);
    }

    public enum PhoneUpdateResult { OK, PHONE_TAKEN }

    public PhoneUpdateResult updatePhone(int customerId, String phone) throws SQLException {
        if (customerDAO.existsByPhoneExcluding(phone, customerId)) return PhoneUpdateResult.PHONE_TAKEN;
        customerDAO.updatePhone(customerId, phone);
        return PhoneUpdateResult.OK;
    }

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


    public enum EmailChangeComplete { SUCCESS, WRONG_OTP, EMAIL_TAKEN, ALREADY_SET }

    public EmailChangeComplete completeEmailChange(int customerId, String newEmail, String otp)
            throws SQLException {

        Customer current = customerDAO.findById(customerId);
        if (current != null && current.getEmail() != null && !current.getEmail().isBlank()) {
            return EmailChangeComplete.ALREADY_SET;
        }

        if (customerDAO.existsByEmailExcluding(newEmail, customerId)) return EmailChangeComplete.EMAIL_TAKEN;

        if (!OtpStore.verify(otpKey(newEmail, "change-email"), otp)) return EmailChangeComplete.WRONG_OTP;

        customerDAO.updateEmail(customerId, newEmail);
        return EmailChangeComplete.SUCCESS;
    }

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


    public enum PasswordChangeResult { SUCCESS, WEAK_PASSWORD, SAME_AS_CURRENT }


    public PasswordChangeResult changePassword(Customer customer, String newPassword)
            throws SQLException {

        if (!PasswordUtil.isStrongPassword(newPassword))
            return PasswordChangeResult.WEAK_PASSWORD;
        if (PasswordUtil.verifyPassword(newPassword, customer.getPasswordHash()))
            return PasswordChangeResult.SAME_AS_CURRENT;

        customerDAO.updatePassword(customer.getCustomerID(), PasswordUtil.hashPassword(newPassword));
        return PasswordChangeResult.SUCCESS;
    }


    private String otpKey(String identifier, String purpose) {
        return identifier.trim().toLowerCase() + ":" + purpose;
    }
}
