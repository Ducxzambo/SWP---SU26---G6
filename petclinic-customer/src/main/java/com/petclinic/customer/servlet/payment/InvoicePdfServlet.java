// petclinic-customer/src/main/java/com/petclinic/customer/servlet/invoice/InvoicePdfServlet.java
package com.petclinic.customer.servlet.payment;

import com.petclinic.backend.dao.AppointmentDAO;
import com.petclinic.backend.dao.InvoiceDAO;
import com.petclinic.backend.dto.ReceiptData;
import com.petclinic.backend.model.Appointment;
import com.petclinic.backend.model.Customer;
import com.petclinic.backend.model.Invoice;
import com.petclinic.backend.model.Payment;
import com.petclinic.backend.service.ReceiptService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

@WebServlet("/invoices/pdf")
public class InvoicePdfServlet extends HttpServlet {

    private final InvoiceDAO     invoiceDAO     = new InvoiceDAO();
    private final AppointmentDAO appointmentDAO = new AppointmentDAO();
    private final ReceiptService receiptService = new ReceiptService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        Customer customer = session != null ? (Customer) session.getAttribute("customer") : null;
        if (customer == null) { resp.sendRedirect(req.getContextPath() + "/auth/login"); return; }

        int invoiceId = parseId(req.getParameter("invoiceId"));
        int paymentId = parseId(req.getParameter("paymentId"));
        if (invoiceId <= 0) { resp.sendError(400); return; }

        try {
            Invoice invoice = invoiceDAO.findById(invoiceId);
            if (invoice == null || invoice.getCustomerID() != customer.getCustomerID()) {
                resp.sendError(404, "Không tìm thấy hóa đơn."); return;
            }
            Appointment appt = appointmentDAO.findById(invoice.getAppointmentID());

            ReceiptData doc;
            if (paymentId > 0) {
                Payment target = invoice.getPayments().stream()
                        .filter(p -> p.getPaymentID() == paymentId)
                        .findFirst().orElse(null);
                if (target == null) { resp.sendError(404, "Không tìm thấy giao dịch thanh toán."); return; }
                doc = receiptService.buildReceiptForPayment(invoice, appt, customer, target);
            } else {
                boolean isSettlementStage = appt != null && "Done".equals(appt.getStatus());
                doc = isSettlementStage
                        ? receiptService.buildSettlementInvoice(invoice, appt, customer)
                        : receiptService.buildPreInvoicePreview(invoice, appt, customer);
            }

            byte[] pdf = receiptService.renderPdf(doc);
            resp.setContentType("application/pdf");
            resp.setHeader("Content-Disposition", "inline; filename=\"" + doc.getInvoiceCode() + ".pdf\"");
            resp.setContentLength(pdf.length);
            resp.getOutputStream().write(pdf);
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendError(500, "Lỗi khi tạo file PDF: " + e.getMessage());
        }
    }

    private int parseId(String s) {
        if (s == null || s.isBlank()) return -1;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return -1; }
    }
}