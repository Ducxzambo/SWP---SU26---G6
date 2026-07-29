package com.petclinic.backend.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.petclinic.backend.util.HttpUtils;
import com.petclinic.backend.util.SecurityUtils;

import java.util.Map;

public class PayOSClient {

    private static final String PAYOS_BASE = "https://api-merchant.payos.vn";
    private static final String CLIENT_ID = System.getenv("PAYOS_CLIENT_ID");
    private static final String API_KEY = System.getenv("PAYOS_API_KEY");
    private static final String CHECKSUM_KEY = System.getenv("PAYOS_CHECKSUM_KEY");
    private static final String APP_BASE_URL = System.getenv().getOrDefault("APP_BASE_URL", "http://localhost:8080/petclinic");

    private final Gson gson = new Gson();

    /**
     * Tạo link thanh toán PayOS — luồng KHÁCH HÀNG (booking online, mặc định).
     */
    public String createPaymentLink(long orderCode, int appointmentId, int invoiceId, long amountVnd, String description, boolean isFullPayment) throws Exception {
        return createPaymentLink(orderCode, appointmentId, invoiceId, amountVnd, description, isFullPayment, "customer", null);
    }

    /**
     * Tạo link thanh toán PayOS - dùng chung cho CẢ khách hàng (booking online)
     * VÀ lễ tân (thu tiền hóa đơn tại quầy).
     *
     * @param source  "customer" (mặc định) hoặc "staff:checkin"/"staff:history"
     *                (định danh lễ tân đang ở tab nào để quay về đúng chỗ).
     * @param staffId ID nhân viên đang thực hiện thu tiền (null nếu luồng khách hàng).
     */
    public String createPaymentLink(long orderCode, int appointmentId, int invoiceId, long amountVnd,
                                    String description, boolean isFullPayment,
                                    String source, Integer staffId) throws Exception {
        // Chuẩn hóa mô tả
        if (description == null || description.isBlank()) description = "Thanh toan PetClinic";
        description = description.replaceAll("[^A-Za-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
        if (description.length() > 25) description = description.substring(0, 25);

        if (source == null || source.isBlank()) source = "customer";

        String returnUrl = APP_BASE_URL + "/payment/result?invoiceId=" + invoiceId + "&apptId=" + appointmentId
                + "&full=" + isFullPayment + "&amount=" + amountVnd + "&source=" + source
                + (staffId != null ? "&staffId=" + staffId : "");

        String cancelUrl;
        if (source.startsWith("staff")) {
            int idx = source.indexOf(':');
            String from = idx >= 0 ? source.substring(idx + 1) : "checkin";
            cancelUrl = APP_BASE_URL + "/receptionist/invoice?invoiceId=" + invoiceId + "&from=" + from;
        } else {
            cancelUrl = APP_BASE_URL + "/appointments/detail?id=" + appointmentId;
        }

        // Tạo chữ ký
        String sigData = "amount=" + amountVnd + "&cancelUrl=" + cancelUrl + "&description=" + description + "&orderCode=" + orderCode + "&returnUrl=" + returnUrl;
        String signature = SecurityUtils.hmacSha256(sigData, CHECKSUM_KEY);

        // Build JSON bằng Gson
        JsonObject body = new JsonObject();
        body.addProperty("orderCode", orderCode);
        body.addProperty("amount", amountVnd);
        body.addProperty("description", description);
        body.addProperty("returnUrl", returnUrl);
        body.addProperty("cancelUrl", cancelUrl);
        body.addProperty("signature", signature);
        body.addProperty("expiredAt", (System.currentTimeMillis() / 1000) + 900);

        JsonArray items = new JsonArray();
        JsonObject item = new JsonObject();
        item.addProperty("name", "PetClinic Service");
        item.addProperty("quantity", 1);
        item.addProperty("price", amountVnd);
        items.add(item);
        body.add("items", items);

        // Gửi HTTP Request
        Map<String, String> headers = Map.of(
                "x-client-id", CLIENT_ID,
                "x-api-key", API_KEY
        );

        String responseStr = HttpUtils.post(PAYOS_BASE + "/v2/payment-requests", gson.toJson(body), headers);

        // Trích xuất checkoutUrl
        JsonObject responseJson = JsonParser.parseString(responseStr).getAsJsonObject();
        return responseJson.getAsJsonObject("data").get("checkoutUrl").getAsString();
    }

    /**
     * Parse và verify Webhook
     */
    public WebhookData parseAndVerifyWebhook(String rawBody, String receivedSignature) throws Exception {
        JsonObject payload = JsonParser.parseString(rawBody).getAsJsonObject();
        String code = payload.get("code").getAsString();

        if (!"00".equals(code)) return null;

        JsonObject data = payload.getAsJsonObject("data");

        // Verify chữ ký
        String signData = "amount=" + data.get("amount").getAsString()
                + "&description=" + data.get("description").getAsString()
                + "&orderCode=" + data.get("orderCode").getAsString()
                + "&reference=" + data.get("reference").getAsString()
                + "&transactionDateTime=" + data.get("transactionDateTime").getAsString();

        String computed = SecurityUtils.hmacSha256(signData, CHECKSUM_KEY);
        if (!computed.equalsIgnoreCase(receivedSignature)) return null;

        return new WebhookData(data.get("orderCode").getAsLong(), data.get("amount").getAsLong());
    }

    public record WebhookData(long orderCode, long amount) {}
}