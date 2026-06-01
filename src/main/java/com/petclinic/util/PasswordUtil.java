package com.petclinic.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Tiện ích hash và xác minh password bằng BCrypt.
 * Dependency: org.mindrot:jbcrypt:0.4
 */
public class PasswordUtil {

    private static final int SALT_ROUNDS = 12;

    /** Hash plain-text password. */
    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(SALT_ROUNDS));
    }

    /** So sánh plain-text với hash đã lưu trong DB. */
    public static boolean verify(String plainPassword, String hashedPassword) {
        if (hashedPassword == null || !hashedPassword.startsWith("$2")) return false;
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }

    private PasswordUtil() {}
}
