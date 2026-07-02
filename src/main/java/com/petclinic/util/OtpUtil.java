package com.petclinic.util;

import jakarta.mail.*;
import jakarta.mail.internet.*;

import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.util.Properties;

public class OtpUtil {

    // ── Email config (replace with real SMTP credentials) ──────────────────
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int SMTP_PORT = 587;

    private static final String EMAIL_FROM = System.getenv("EMAIL_FROM");
    private static final String EMAIL_USER = System.getenv("SMTP_USER");
    private static final String EMAIL_PASS = System.getenv("SMTP_PASS");


    private static final SecureRandom RANDOM = new SecureRandom();
    public  static final int OTP_EXPIRE_MINUTES = 10;

    /** Generate a 6-digit numeric OTP. */
    public static String generateOtp() {
        int otp = 100_000 + RANDOM.nextInt(900_000);
        return String.valueOf(otp);
    }

    /** Send OTP via email using JavaMail. */
    public static void sendOtpEmail(String toEmail, String otp, String purpose) throws MessagingException, UnsupportedEncodingException {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            SMTP_HOST);
        props.put("mail.smtp.port",            String.valueOf(SMTP_PORT));

        Session session = Session.getInstance(props, new Authenticator() {
            @Override protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EMAIL_USER, EMAIL_PASS);
            }
        });

        String subject = buildEmailSubject(purpose);
        String body    = buildEmailBody(otp, purpose);

        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(EMAIL_FROM, "PetClinic"));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        msg.setSubject(subject);
        msg.setContent(body, "text/html; charset=UTF-8");

        Transport.send(msg);
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private static String buildEmailSubject(String purpose) {
        switch (purpose) {
            case "register":       return "[PetClinic] Email Verification Code";
            case "forgot":         return "[PetClinic] Password Reset Code";
            default:               return "[PetClinic] Verification Code";
        }
    }

    private static String buildEmailBody(String otp, String purpose) {
        String action = purpose.equals("forgot") ? "reset your password" : "verify your email";
        return "<div style='font-family:sans-serif;max-width:480px;margin:auto;padding:32px;'>"
             + "<h2 style='color:#2d7a4f;'>🐾 PetClinic</h2>"
             + "<p>Your verification code to <strong>" + action + "</strong>:</p>"
             + "<div style='font-size:36px;font-weight:bold;letter-spacing:8px;"
             +      "background:#f0f9f4;border:2px solid #2d7a4f;border-radius:8px;"
             +      "padding:16px;text-align:center;color:#2d7a4f;'>" + otp + "</div>"
             + "<p style='color:#888;font-size:13px;margin-top:16px;'>"
             +    "This code expires in <strong>" + OTP_EXPIRE_MINUTES + " minutes</strong>. "
             +    "Do not share it with anyone.</p>"
             + "</div>";
    }

}
