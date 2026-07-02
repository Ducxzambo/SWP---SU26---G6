package com.petclinic.util;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory OTP store.
 * Key  = "email:purpose" or "phone:purpose"
 * Value = OtpEntry(code, expiresAt)
 *
 * For multi-instance / production: replace with Redis or DB-backed store.
 */
public class OtpStore {

    private static final Map<String, OtpEntry> STORE = new ConcurrentHashMap<>();

    public static void save(String key, String otp) {
        Instant expiry = Instant.now().plusSeconds(OtpUtil.OTP_EXPIRE_MINUTES * 60L);
        STORE.put(normalise(key), new OtpEntry(otp, expiry));
        evictExpired();
    }

    public static boolean verify(String key, String inputOtp) {
        OtpEntry entry = STORE.get(normalise(key));
        if (entry == null)                            return false;
        if (Instant.now().isAfter(entry.expiresAt))  { STORE.remove(normalise(key)); return false; }
        if (!entry.otp.equals(inputOtp.trim()))       return false;
        STORE.remove(normalise(key));  // one-time use
        return true;
    }

    public static boolean exists(String key) {
        OtpEntry entry = STORE.get(normalise(key));
        if (entry == null) return false;
        if (Instant.now().isAfter(entry.expiresAt)) { STORE.remove(normalise(key)); return false; }
        return true;
    }

    private static String normalise(String key) { return key.toLowerCase().trim(); }

    private static void evictExpired() {
        Instant now = Instant.now();
        Iterator<Map.Entry<String, OtpEntry>> it = STORE.entrySet().iterator();
        while (it.hasNext()) { if (now.isAfter(it.next().getValue().expiresAt)) it.remove(); }
    }

    private static class OtpEntry {
        final String  otp;
        final Instant expiresAt;
        OtpEntry(String otp, Instant expiresAt) { this.otp = otp; this.expiresAt = expiresAt; }
    }
}
