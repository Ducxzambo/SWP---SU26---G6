package com.petclinic.service;

import com.petclinic.dao.InvoiceDAO;

import java.math.BigDecimal;

public class PaymentService {

    private final InvoiceDAO invoiceDAO = new InvoiceDAO();
    private final PayOSClient payosClient = new PayOSClient();
    private final AssignmentService assignmentSvc = new AssignmentService();

    public String createPaymentLink(int invoiceId, int appointmentId, long amountVnd, String description, boolean isFullPayment) throws Exception {
        if (amountVnd <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than 0");
        }

        long orderCode = buildOrderCode(invoiceId, isFullPayment);
        return payosClient.createPaymentLink(orderCode, appointmentId, invoiceId, amountVnd, description, isFullPayment);
    }

    public boolean handleWebhook(String rawBody, String receivedSignature) throws Exception {
        PayOSClient.WebhookData data = payosClient.parseAndVerifyWebhook(rawBody, receivedSignature);
        if (data == null) return false;

        int invoiceId = decodeInvoiceId(data.orderCode());
        boolean isFullPayment = decodeIsFullPayment(data.orderCode());

        // Gọi DAO để update database
        int apptId = invoiceDAO.confirmPaymentInTransaction(invoiceId, data.amount(), isFullPayment);

        // Tự động assign bác sĩ nếu thành công
        if (apptId > 0) {
            assignmentSvc.autoAssign(apptId);
        }
        return true;
    }

    // Các hàm Invoice giữ nguyên (hoặc bạn có thể chuyển hẳn sang InvoiceService)
    /**
     * Invoice luôn được tạo ở trạng thái 'Unpaid' — được tạo NGAY SAU khi
     * tạo appointment, TRƯỚC khi khách thanh toán. Sẽ tự chuyển 'PrePaid'
     * ngay khi thanh toán 100% được xác nhận (xem
     * InvoiceDAO.confirmPaymentInTransaction/insertPayment).
     */
    public int createInvoice(int customerId, int appointmentId, BigDecimal totalAmount) throws Exception {
        return invoiceDAO.createInvoice(customerId, appointmentId, totalAmount, "Unpaid");
    }

    public void addInvoiceItem(int invoiceId, String itemType, String description, BigDecimal quantity, BigDecimal unitPrice) throws Exception {
        invoiceDAO.addInvoiceItem(invoiceId, itemType, description, quantity, unitPrice);
    }

    // Luồng sinh mã orderCode
    private long buildOrderCode(int invoiceId, boolean isFullPayment) {
        long retrySuffix = (System.currentTimeMillis() / 1000) % 10_000;
        return (long) invoiceId * 100_000 + retrySuffix * 10 + (isFullPayment ? 1 : 0);
    }

    public int decodeInvoiceId(long orderCode) {
        if (orderCode >= 100_000) return (int) (orderCode / 100_000);
        return (int) (orderCode / 10);
    }

    public boolean decodeIsFullPayment(long orderCode) {
        return (orderCode % 10) == 1;
    }
}