package com.petclinic.backend.util;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Xây URL ảnh VietQR (dịch vụ quicklink công khai, không cần API key) để
 * staff quét bằng app ngân hàng của họ và tự chuyển khoản thủ công. Đây
 * KHÔNG phải một lệnh gọi API tạo giao dịch - chỉ là 1 URL ảnh
 * (<img src="...">), trình duyệt tự tải, không cần gọi từ phía server.
 *
 * Tham khảo: https://www.vietqr.io/danh-sach-api/link-tao-ma-qr
 */
public final class VietQrUtil {

    private static final String BASE = "https://img.vietqr.io/image/";

    private VietQrUtil() {
    }

    /**
     * @param bankCode      mã ngân hàng theo chuẩn VietQR (vd "VCB", "MB"...)
     * @param accountNumber số tài khoản người nhận
     * @param accountName   tên chủ tài khoản (hiển thị trên ảnh QR, không bắt buộc)
     * @param amount        số tiền cần chuyển
     * @param message       nội dung chuyển khoản gợi ý
     */
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
