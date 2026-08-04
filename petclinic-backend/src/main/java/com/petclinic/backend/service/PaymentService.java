package com.petclinic.backend.service;

import com.petclinic.backend.dao.InvoiceDAO;

import java.math.BigDecimal;

public class PaymentService {

    private final InvoiceDAO invoiceDAO = new InvoiceDAO();
    private final PayOSClient payosClient = new PayOSClient();

    public String createPaymentLink(int invoiceId, int appointmentId, long amountVnd, String description, boolean isFullPayment) throws Exception {
        return createPaymentLink(invoiceId, appointmentId, amountVnd, description, isFullPayment, "customer", null);
    }

    public String createPaymentLink(int invoiceId, int appointmentId, long amountVnd, String description,
                                    boolean isFullPayment, String source, Integer staffId) throws Exception {
        if (amountVnd <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than 0");
        }

        long orderCode = buildOrderCode(invoiceId, isFullPayment);
        return payosClient.createPaymentLink(orderCode, appointmentId, invoiceId, amountVnd, description,
                isFullPayment, source, staffId);
    }

    public boolean handleWebhook(String rawBody, String receivedSignature) throws Exception {
        PayOSClient.WebhookData data = payosClient.parseAndVerifyWebhook(rawBody, receivedSignature);
        if (data == null) return false;

        int invoiceId = decodeInvoiceId(data.orderCode());
        boolean isFullPayment = decodeIsFullPayment(data.orderCode());
        int apptId = invoiceDAO.confirmPaymentInTransaction(invoiceId, data.amount(), isFullPayment);
        return true;
    }


    public int createInvoice(int customerId, int appointmentId, BigDecimal totalAmount) throws Exception {
        return invoiceDAO.createInvoice(customerId, appointmentId, totalAmount, "Unpaid");
    }

    public void addInvoiceItem(int invoiceId, String itemType, String description, BigDecimal quantity, BigDecimal unitPrice) throws Exception {
        invoiceDAO.addInvoiceItem(invoiceId, itemType, description, quantity, unitPrice);
    }

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
