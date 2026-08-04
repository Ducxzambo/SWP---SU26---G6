package com.petclinic.backend.util;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class VietQrUtil {

    private static final String BASE = "https://img.vietqr.io/image/";

    private VietQrUtil() {
    }

    public static String buildQuickLinkUrl(String bankCode, String accountNumber,
                                           String accountName, BigDecimal amount, String message) {
        if (bankCode == null || bankCode.isBlank() || accountNumber == null || accountNumber.isBlank()) {
            return null;
        }
        StringBuilder url = new StringBuilder(BASE)
                .append(enc(bankCode.trim())).append('-')
                .append(enc(accountNumber.trim())).append("-compact2.png")
                .append("?amount=").append(amount == null ? "0" : amount.toBigInteger().toString());
        if (message != null && !message.isBlank()) {
            url.append("&addInfo=").append(enc(message));
        }
        if (accountName != null && !accountName.isBlank()) {
            url.append("&accountName=").append(enc(accountName));
        }
        return url.toString();
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
