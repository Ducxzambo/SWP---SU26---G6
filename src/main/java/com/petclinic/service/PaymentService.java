package com.petclinic.service;

import com.petclinic.dao.AppointmentDAO;
import com.petclinic.dao.InvoiceDAO;
import com.petclinic.model.Invoice;
import com.petclinic.util.DBConnection;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.math.BigDecimal;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * PayOS (by Casso) integration.
 *
 * Docs: https://payos.vn/docs/
 *
 * Required env vars:
 *   PAYOS_CLIENT_ID     – from PayOS dashboard
 *   PAYOS_API_KEY       – from PayOS dashboard
 *   PAYOS_CHECKSUM_KEY  – for HMAC-SHA256 signature
 *   APP_BASE_URL        – e.g. https://yourapp.com (for cancel/return URLs)
 */
public class PaymentService {

    private static final String PAYOS_BASE      = "https://api-merchant.payos.vn";
    private static final String CLIENT_ID        = System.getenv("PAYOS_CLIENT_ID");
    private static final String API_KEY          = System.getenv("PAYOS_API_KEY");
    private static final String CHECKSUM_KEY     = System.getenv("PAYOS_CHECKSUM_KEY");
    private static final String APP_BASE_URL     = System.getenv().getOrDefault("APP_BASE_URL", "http://localhost:8080/petclinic");

    private final InvoiceDAO     invoiceDAO     = new InvoiceDAO();
    private final AppointmentDAO appointmentDAO = new AppointmentDAO();

    // ── Create PayOS payment link ─────────────────────────────────────────────

    /**
     * Creates a PayOS payment and returns the checkout URL to redirect the customer.
     *
     * @param invoiceId      our Invoice ID (used as PayOS orderCode)
     * @param appointmentId  for return URL building
     * @param amountVnd      VND amount to charge
     * @param description    shown in bank transfer description (max 25 chars)
     * @param isFullPayment  true = Paid, false = PartiallyPaid
     */
    public String createPaymentLink(int invoiceId, int appointmentId,
                                    long amountVnd, String description,
                                    boolean isFullPayment) throws Exception {

        long orderCode = buildOrderCode(invoiceId, isFullPayment);

        // Truncate description to 25 chars (PayOS limit)
        if (description == null || description.isBlank()) description = "Thanh toan PetClinic";
        if (description.length() > 25) description = description.substring(0, 25);

        String returnUrl = APP_BASE_URL + "/payment/result?invoiceId=" + invoiceId
                + "&apptId=" + appointmentId + "&full=" + isFullPayment;
        String cancelUrl = APP_BASE_URL + "/appointments/detail?id=" + appointmentId;

        // Build signature string: amount|cancelUrl|description|orderCode|returnUrl
        String sigData = "amount=" + amountVnd
                + "&cancelUrl=" + cancelUrl
                + "&description=" + description
                + "&orderCode=" + orderCode
                + "&returnUrl=" + returnUrl;
        String signature = hmacSha256(sigData, CHECKSUM_KEY);

        // Build JSON body manually (no Gson dependency required)
        String body = "{"
                + "\"orderCode\":" + orderCode + ","
                + "\"amount\":" + amountVnd + ","
                + "\"description\":\"" + escJson(description) + "\","
                + "\"returnUrl\":\"" + escJson(returnUrl) + "\","
                + "\"cancelUrl\":\"" + escJson(cancelUrl) + "\","
                + "\"signature\":\"" + signature + "\","
                + "\"expiredAt\":" + (System.currentTimeMillis() / 1000 + 900) + ","  // 15 min
                + "\"items\":[{\"name\":\"PetClinic\",\"quantity\":1,\"price\":" + amountVnd + "}]"
                + "}";

        String response = post(PAYOS_BASE + "/v2/payment-requests", body);
        return extractJsonField(response, "checkoutUrl");
    }

    // ── Webhook handler ───────────────────────────────────────────────────────

    /**
     * Called by PayOS webhook (POST /payment/webhook).
     * Verifies HMAC signature, then updates Invoice + Appointment status.
     * Returns true if signature valid and invoice updated.
     */
    public boolean handleWebhook(String rawBody, String receivedSignature) throws Exception {
        // Verify signature
        String computed = hmacSha256(extractWebhookSignData(rawBody), CHECKSUM_KEY);
        if (!computed.equalsIgnoreCase(receivedSignature)) return false;

        // Parse orderCode and payment status
        String code   = extractJsonField(rawBody, "code");   // "00" = success
        if (!"00".equals(code)) return false;

        String orderCodeStr = extractJsonField(rawBody, "orderCode");
        long   orderCode    = Long.parseLong(orderCodeStr);
        long   amount       = Long.parseLong(extractJsonField(rawBody, "amount"));

        // Decode: invoiceId from orderCode (see buildOrderCode)
        int     invoiceId    = (int)(orderCode / 10);
        boolean isFullPayment= (orderCode % 10) == 1;

        confirmPayment(invoiceId, amount, isFullPayment);
        return true;
    }

    /**
     * Mark invoice as Paid/PartiallyPaid, appointment as Confirmed,
     * insert a Payment record.
     */
    public void confirmPayment(int invoiceId, long amountVnd, boolean isFullPayment)
            throws Exception {

        String invoiceStatus = isFullPayment ? "Paid" : "PartiallyPaid";
        int apptId = -1;

        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                // Update invoice status
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE Invoices SET Status=? WHERE InvoiceID=?")) {
                    ps.setString(1, invoiceStatus);
                    ps.setInt(2, invoiceId);
                    ps.executeUpdate();
                }

                // Get appointmentId from invoice
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT AppointmentID FROM Invoices WHERE InvoiceID=?")) {
                    ps.setInt(1, invoiceId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) apptId = rs.getInt(1);
                    }
                }

                if (apptId > 0) {
                    // Update appointment status to Confirmed
                    try (PreparedStatement ps = c.prepareStatement(
                            "UPDATE Appointments SET Status='Confirmed' WHERE AppointmentID=?")) {
                        ps.setInt(1, apptId);
                        ps.executeUpdate();
                    }
                }

                // Insert Payment record (ProcessedByID = 1 = system user; adjust as needed)
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO Payments (InvoiceID, Amount, Method, PaidAt, ProcessedByID) "
                                + "VALUES (?, ?, 'BankTransfer', GETDATE(), 1)")) {
                    ps.setInt(1, invoiceId);
                    ps.setLong(2, amountVnd);
                    ps.executeUpdate();
                }

                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }

        // Auto-assign Vet/Groomer NGOÀI transaction trên (cần connection riêng,
        // và không nên rollback việc thanh toán chỉ vì auto-assign lỗi).
        if (apptId > 0) {
            new AssignmentService().autoAssign(apptId);
        }
    }

    // ── Invoice creation ──────────────────────────────────────────────────────

    /**
     * Create an Invoice for a list of appointments (one per pet×service combo).
     * Returns the InvoiceID.
     */
    public int createInvoice(int customerId, int appointmentId,
                             BigDecimal totalAmount) throws Exception {
        String sql = "INSERT INTO Invoices (AppointmentID, CustomerID, TotalAmount, Status) "
                + "VALUES (?, ?, ?, 'Unpaid')";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, appointmentId);
            ps.setInt(2, customerId);
            ps.setBigDecimal(3, totalAmount);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    /** Add a line item to an Invoice. */
    public void addInvoiceItem(int invoiceId, String itemType, String description,
                               BigDecimal quantity, BigDecimal unitPrice) throws Exception {
        String sql = "INSERT INTO InvoiceItems (InvoiceID, ItemType, Description, Quantity, UnitPrice) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            ps.setString(2, itemType);
            ps.setString(3, description);
            ps.setBigDecimal(4, quantity);
            ps.setBigDecimal(5, unitPrice);
            ps.executeUpdate();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** orderCode encodes invoiceId and payment type: invoiceId*10 + (full?1:0) */
    private long buildOrderCode(int invoiceId, boolean isFullPayment) {
        return (long) invoiceId * 10 + (isFullPayment ? 1 : 0);
    }

    /** Build signature data string per PayOS spec for webhook verification. */
    private String extractWebhookSignData(String json) {
        // PayOS webhook sig data: amount|cancelUrl|description|orderCode|returnUrl
        // For webhook, we verify the "data" sub-object fields
        // Simplified: extract key fields in alphabetical order
        String amount      = extractJsonField(json, "amount");
        String desc        = extractJsonField(json, "description");
        String orderCode   = extractJsonField(json, "orderCode");
        String reference   = extractJsonField(json, "reference");
        String transDateTime = extractJsonField(json, "transactionDateTime");
        return "amount=" + amount
                + "&description=" + desc
                + "&orderCode=" + orderCode
                + "&reference=" + reference
                + "&transactionDateTime=" + transDateTime;
    }

    private String hmacSha256(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private String post(String url, String body) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type",  "application/json");
        conn.setRequestProperty("x-client-id",  CLIENT_ID);
        conn.setRequestProperty("x-api-key",    API_KEY);
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(15_000);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    /** Naive JSON field extractor (no dependency on Gson). */
    private String extractJsonField(String json, String field) {
        String key = "\"" + field + "\"";
        int idx = json.indexOf(key);
        if (idx < 0) return "";
        int colon = json.indexOf(':', idx + key.length());
        if (colon < 0) return "";
        int start = colon + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;
        if (start >= json.length()) return "";
        char first = json.charAt(start);
        if (first == '"') {
            int end = json.indexOf('"', start + 1);
            return end < 0 ? "" : json.substring(start + 1, end);
        } else {
            int end = start;
            while (end < json.length() && ",}]".indexOf(json.charAt(end)) < 0) end++;
            return json.substring(start, end).trim();
        }
    }

    private String escJson(String s) {
        return s == null ? "" : s.replace("\\","\\\\").replace("\"","\\\"");
    }
}