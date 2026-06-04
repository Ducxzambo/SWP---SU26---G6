package com.petclinic.util;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.util.Properties;

/**
 * OTP Generator + Email sender (JavaMail) + SMS stub (Twilio).
 * <p>
 * Maven dependencies required:
 * <dependency>
 * <groupId>com.sun.mail</groupId>
 * <artifactId>jakarta.mail</artifactId>
 * <version>2.0.1</version>
 * </dependency>
 * <p>
 * For Twilio SMS:
 * <dependency>
 * <groupId>com.twilio.sdk</groupId>
 * <artifactId>twilio</artifactId>
 * <version>9.14.0</version>
 * </dependency>
 */
public class OtpUtil {

    public static final int OTP_EXPIRE_MINUTES = 10;
    // ── Email config (replace with real SMTP credentials) ──────────────────
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int SMTP_PORT = 587;
    private static final String EMAIL_FROM = "noreply@petclinic.com";
    private static final String EMAIL_USER = System.getenv("SMTP_USER");    // set env var
    private static final String EMAIL_PASS = System.getenv("SMTP_PASS");
    // ── Twilio config ───────────────────────────────────────────────────────
    private static final String TWILIO_ACCOUNT_SID = System.getenv("TWILIO_SID");
    private static final String TWILIO_AUTH_TOKEN = System.getenv("TWILIO_TOKEN");
    private static final String TWILIO_FROM_PHONE = System.getenv("TWILIO_PHONE");  // e.g. +1234567890
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generate a 6-digit numeric OTP.
     */
    public static String generateOtp() {
        int otp = 100_000 + RANDOM.nextInt(900_000);
        return String.valueOf(otp);
    }

    /**
     * Send OTP via email using JavaMail.
     */
    public static void sendOtpEmail(String toEmail, String otp, String purpose) throws MessagingException, UnsupportedEncodingException {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", String.valueOf(SMTP_PORT));

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EMAIL_USER, EMAIL_PASS);
            }
        });

        String subject = buildEmailSubject(purpose);
        String body = buildEmailBody(otp, purpose);

        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(EMAIL_FROM, "PetClinic"));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        msg.setSubject(subject);
        msg.setContent(body, "text/html; charset=UTF-8");

        Transport.send(msg);
    }

    /**
     * Send OTP via SMS using Twilio.
     * Uncomment and add Twilio dependency to enable.
     */
    public static void sendOtpSms(String toPhone, String otp, String purpose) {
        // ── Twilio integration (uncomment when dependency is added) ──────────
        // com.twilio.Twilio.init(TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN);
        // String messageBody = buildSmsBody(otp, purpose);
        // com.twilio.rest.api.v2010.account.Message.creator(
        //     new com.twilio.type.PhoneNumber(toPhone),
        //     new com.twilio.type.PhoneNumber(TWILIO_FROM_PHONE),
        //     messageBody
        // ).create();

        // ── Stub: log to console until Twilio is configured ─────────────────
        System.out.printf("[SMS STUB] To: %s | OTP: %s | Purpose: %s%n", toPhone, otp, purpose);
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private static String buildEmailSubject(String purpose) {
        switch (purpose) {
            case "register":
                return "[PetClinic] Email Verification Code";
            case "forgot":
                return "[PetClinic] Password Reset Code";
            default:
                return "[PetClinic] Verification Code";
        }
    }

    private static String buildEmailBody(String otp, String purpose) {
        String action = purpose.equals("forgot") ? "reset your password" : "verify your email";
        return "<div style='font-family:sans-serif;max-width:480px;margin:auto;padding:32px;'>"
                + "<h2 style='color:#2d7a4f;'>🐾 PetClinic</h2>"
                + "<p>Your verification code to <strong>" + action + "</strong>:</p>"
                + "<div style='font-size:36px;font-weight:bold;letter-spacing:8px;"
                + "background:#f0f9f4;border:2px solid #2d7a4f;border-radius:8px;"
                + "padding:16px;text-align:center;color:#2d7a4f;'>" + otp + "</div>"
                + "<p style='color:#888;font-size:13px;margin-top:16px;'>"
                + "This code expires in <strong>" + OTP_EXPIRE_MINUTES + " minutes</strong>. "
                + "Do not share it with anyone.</p>"
                + "</div>";
    }

    private static String buildSmsBody(String otp, String purpose) {
        return "[PetClinic] Your verification code is: " + otp
                + ". Valid for " + OTP_EXPIRE_MINUTES + " minutes.";
    }
}
