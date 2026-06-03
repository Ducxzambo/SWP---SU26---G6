package com.petclinic.util;

import java.security.SecureRandom;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Simple BCrypt-style password utility.
 * In production, replace with org.mindrot:jbcrypt library for true BCrypt.
 * Add dependency: <dependency>
 *   <groupId>org.mindrot</groupId>
 *   <artifactId>jbcrypt</artifactId>
 *   <version>0.4</version>
 * </dependency>
 */
public class PasswordUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    /** Hash password using SHA-256 + random salt (swap for BCrypt in production). */
    public static String hashPassword(String rawPassword) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hash    = sha256(saltB64 + rawPassword);
        return saltB64 + ":" + hash;
    }

    /** Verify raw password against stored hash. */
    public static boolean verifyPassword(String rawPassword, String storedHash) {
        if (storedHash == null || !storedHash.contains(":")) return false;
        String[] parts = storedHash.split(":", 2);
        String saltB64 = parts[0];
        String expected = parts[1];
        String actual   = sha256(saltB64 + rawPassword);
        return expected.equals(actual);
    }

    /** Validate password complexity: ≥6 chars, uppercase, lowercase, digit, special char. */
    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 6) return false;
        boolean hasUpper   = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower   = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit   = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(c ->
                "!@#$%^&*()_+-=[]{}|;':\",./<>?".indexOf(c) >= 0);
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
