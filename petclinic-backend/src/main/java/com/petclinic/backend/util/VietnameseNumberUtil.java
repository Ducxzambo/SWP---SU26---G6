package com.petclinic.backend.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Đọc số tiền (VNĐ) thành chữ tiếng Việt, dùng cho dòng "Bằng chữ" trên biên lai. */
public final class VietnameseNumberUtil {

    private static final String[] DIGITS = {
            "không", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín"
    };
    private static final String[] UNITS = { "", "nghìn", "triệu", "tỷ" };

    private VietnameseNumberUtil() {}

    public static String readMoney(BigDecimal amount) {
        if (amount == null) amount = BigDecimal.ZERO;
        long value = amount.setScale(0, RoundingMode.HALF_UP).longValueExact();
        if (value == 0) return "Không đồng chẵn./.";
        boolean negative = value < 0;
        if (negative) value = -value;

        String words = capitalizeFirst(readInteger(value).trim());
        return (negative ? "Âm " : "") + words + " đồng chẵn./.";
    }

    private static String readInteger(long value) {
        if (value == 0) return DIGITS[0];
        List<Long> groups = new ArrayList<>();
        long v = value;
        while (v > 0) { groups.add(v % 1000); v /= 1000; }

        StringBuilder sb = new StringBuilder();
        for (int i = groups.size() - 1; i >= 0; i--) {
            long group = groups.get(i);
            if (group == 0) continue;
            sb.append(readGroup(group, i < groups.size() - 1));
            if (i > 0) sb.append(" ").append(UNITS[i]);
            sb.append(" ");
        }
        return sb.toString().trim().replaceAll("\\s+", " ");
    }

    private static String readGroup(long group, boolean padHundred) {
        long hundreds = group / 100;
        long remainder = group % 100;
        long tens = remainder / 10;
        long units = remainder % 10;

        StringBuilder sb = new StringBuilder();
        if (hundreds > 0 || padHundred) {
            sb.append(DIGITS[(int) hundreds]).append(" trăm ");
        }
        if (tens == 0) {
            if (units > 0) sb.append(hundreds > 0 || padHundred ? "lẻ " : "").append(DIGITS[(int) units]);
        } else if (tens == 1) {
            sb.append("mười ");
            if (units == 5) sb.append("lăm");
            else if (units > 0) sb.append(DIGITS[(int) units]);
        } else {
            sb.append(DIGITS[(int) tens]).append(" mươi ");
            if (units == 1) sb.append("mốt");
            else if (units == 5) sb.append("lăm");
            else if (units > 0) sb.append(DIGITS[(int) units]);
        }
        return sb.toString().trim();
    }

    private static String capitalizeFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}