package com.petclinic.util;

import jakarta.mail.*;
import jakarta.mail.internet.*;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

/**
 * Gửi email qua SMTP (ví dụ: Gmail, SendGrid, Mailtrap).
 * Cấu hình qua biến môi trường.
 */
public class EmailUtil {

    private static final String SMTP_HOST = System.getenv().getOrDefault("SMTP_HOST", "smtp.gmail.com");
    private static final String SMTP_PORT = System.getenv().getOrDefault("SMTP_PORT", "587");
    private static final String FROM_EMAIL =System.getenv().getOrDefault("SMTP_USER","haminhtx@gmail.com");
    private static final String FROM_PASS  = System.getenv().getOrDefault("SMTP_PASS", "vbcs tiih jhxb zeyi");

    public static void sendResetPasswordEmail(String toEmail, String resetLink) throws MessagingException, UnsupportedEncodingException {
        Session session = buildSession();

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(FROM_EMAIL, "Pet Clinic", "UTF-8"));

        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject("Đặt lại mật khẩu - Pet Clinic");

        String html = """
            <div style="font-family:Arial,sans-serif;max-width:500px">
              <h2>Yêu cầu đặt lại mật khẩu</h2>
              <p>Nhấn vào nút bên dưới để đặt lại mật khẩu. Link có hiệu lực trong <strong>30 phút</strong>.</p>
              <a href="%s"
                 style="display:inline-block;padding:12px 24px;background:#4f46e5;
                        color:#fff;text-decoration:none;border-radius:6px">
                Đặt lại mật khẩu
              </a>
              <p style="color:#888;font-size:12px;margin-top:16px">
                Nếu bạn không yêu cầu, hãy bỏ qua email này.
              </p>
            </div>
            """.formatted(resetLink);

        message.setContent(html, "text/html; charset=utf-8");
        Transport.send(message);
        System.out.println("Reset password email sent to: " + toEmail);
    }

    private static Session buildSession() {
        Properties props = new Properties();

        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props,
                new jakarta.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(
                                FROM_EMAIL,
                                FROM_PASS
                        );
                    }
                });

        session.setDebug(true);
        return session;
    }

    private EmailUtil() {}
}
