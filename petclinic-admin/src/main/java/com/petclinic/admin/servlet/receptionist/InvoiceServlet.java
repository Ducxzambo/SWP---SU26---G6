package com.petclinic.admin.servlet.receptionist;

import com.petclinic.backend.dao.*;
import com.petclinic.backend.dto.ReceiptData;
import com.petclinic.backend.model.*;
import com.petclinic.backend.service.AssignmentService;
import com.petclinic.backend.service.EmailService;
import com.petclinic.backend.service.PaymentService;
import com.petclinic.backend.service.ReceiptService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.math.BigDecimal;

@WebServlet("/receptionist/invoice")
public class InvoiceServlet extends HttpServlet {

    private final InvoiceDAO        invoiceDAO        = new InvoiceDAO();
    private final AppointmentDAO    appointmentDAO     = new AppointmentDAO();
    private final CustomerDAO       customerDAO        = new CustomerDAO();
    private final PaymentService    paymentService     = new PaymentService();
    private final AssignmentService assignmentService  = new AssignmentService();
    private final ReceiptService    receiptService     = new ReceiptService();
    private final EmailService      emailService       = new EmailService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Staff staff = getAuthenticatedReceptionist(req, resp);
        if (staff == null) return;

        int invoiceId = parseId(req.getParameter("invoiceId"));
        String from = normalizeFrom(req.getParameter("from"));
        if (invoiceId <= 0) { resp.sendRedirect(req.getContextPath() + "/receptionist/checkin"); return; }

        try {
            Invoice invoice = invoiceDAO.findById(invoiceId);
            if (invoice == null) {
                req.getSession().setAttribute("flashError", "Không tìm thấy hóa đơn #" + invoiceId + ".");
                resp.sendRedirect(req.getContextPath() + "/receptionist/" + from);
                return;
            }
            Appointment appt = appointmentDAO.findById(invoice.getAppointmentID());
            Customer customer = customerDAO.findById(invoice.getCustomerID());
            boolean isSettlementStage = appt != null && "Done".equals(appt.getStatus());

            BigDecimal amountPaid = invoiceDAO.getAmountPaid(invoiceId);
            BigDecimal amountDue  = invoice.getTotalAmount().subtract(amountPaid);
            if (amountDue.compareTo(BigDecimal.ZERO) < 0) amountDue = BigDecimal.ZERO;

            ReceiptData preview = isSettlementStage
                    ? receiptService.buildSettlementInvoice(invoice, appt, customer)
                    : receiptService.buildPreInvoicePreview(invoice, appt, customer);

            // Xuất PDF hóa đơn
            if ("pdf".equalsIgnoreCase(req.getParameter("format"))) {
                byte[] pdf = receiptService.renderPdf(preview);
                String filename = invoice.getInvoiceCode() != null
                        ? invoice.getInvoiceCode() : ("INV" + String.format("%06d", invoiceId));
                resp.setContentType("application/pdf");
                resp.setHeader("Content-Disposition", "inline; filename=\"" + filename + ".pdf\"");
                resp.setContentLength(pdf.length);
                resp.getOutputStream().write(pdf);
                return;
            }

            req.setAttribute("invoice",     invoice);
            req.setAttribute("appointment", appt);
            req.setAttribute("amountPaid",  amountPaid);
            req.setAttribute("amountDue",   amountDue);
            req.setAttribute("fullyPaid",   amountDue.compareTo(BigDecimal.ZERO) <= 0);
            req.setAttribute("isSettlementStage", isSettlementStage);
            req.setAttribute("invoicePreview", preview);
            req.setAttribute("from",  from);
            req.setAttribute("staff", staff);

            req.getRequestDispatcher("/WEB-INF/views/receptionist/invoice.jsp").forward(req, resp);

        } catch (Exception e) {
            e.printStackTrace();
            req.getSession().setAttribute("flashError", "Lỗi hệ thống khi tải hóa đơn: " + e.getMessage());
            resp.sendRedirect(req.getContextPath() + "/receptionist/" + from);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        Staff staff = getAuthenticatedReceptionist(req, resp);
        if (staff == null) return;

        int invoiceId = parseId(req.getParameter("invoiceId"));
        String from   = normalizeFrom(req.getParameter("from"));
        String action = req.getParameter("action");   // "cash" | "bank" | "mixed"
        HttpSession session = req.getSession();
        String redirectBack = req.getContextPath() + "/receptionist/invoice?invoiceId=" + invoiceId + "&from=" + from;

        if (invoiceId <= 0) {
            session.setAttribute("flashError", "Thiếu mã hóa đơn.");
            resp.sendRedirect(req.getContextPath() + "/receptionist/" + from);
            return;
        }

        try {
            Invoice invoice = invoiceDAO.findById(invoiceId);
            if (invoice == null) {
                session.setAttribute("flashError", "Không tìm thấy hóa đơn #" + invoiceId + ".");
                resp.sendRedirect(req.getContextPath() + "/receptionist/" + from);
                return;
            }
            Appointment appt = appointmentDAO.findById(invoice.getAppointmentID());
            boolean isSettlementStage = appt != null && "Done".equals(appt.getStatus());

            BigDecimal amountPaid = invoiceDAO.getAmountPaid(invoiceId);
            BigDecimal amountDue  = invoice.getTotalAmount().subtract(amountPaid);
            if (amountDue.compareTo(BigDecimal.ZERO) <= 0) {
                session.setAttribute("flashWarning", "Hóa đơn này đã được thanh toán đủ.");
                resp.sendRedirect(redirectBack);
                return;
            }

            switch (action == null ? "" : action) {
                case "cash"  -> handleCash(req, session, staff, invoice, amountDue, isSettlementStage, from, resp);
                case "bank"  -> handleBank(session, staff, invoice, amountDue, from, resp);
                case "mixed" -> handleMixed(req, session, staff, invoice, amountDue, from, resp);
                default -> resp.sendRedirect(redirectBack);
            }

        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("flashError", "Lỗi hệ thống: " + e.getMessage());
            resp.sendRedirect(redirectBack);
        }
    }

    // 1. Tiền mặt: thu đủ amountDue, validate + tính tiền thối
    private void handleCash(HttpServletRequest req, HttpSession session, Staff staff, Invoice invoice,
                            BigDecimal amountDue, boolean isSettlementStage, String from,
                            HttpServletResponse resp) throws Exception {
        int invoiceId = invoice.getInvoiceID();
        String redirectBack = req.getContextPath() + "/receptionist/invoice?invoiceId=" + invoiceId + "&from=" + from;

        BigDecimal tendered = parseAmount(req.getParameter("cashReceived"));
        if (tendered == null || tendered.compareTo(amountDue) < 0) {
            session.setAttribute("flashError",
                    "Số tiền khách đưa không đủ để thanh toán " + formatVnd(amountDue) + "đ.");
            resp.sendRedirect(redirectBack);
            return;
        }
        BigDecimal change = tendered.subtract(amountDue);

        invoiceDAO.insertPayment(invoiceId, amountDue, "Cash", staff.getStaffID());
        assignmentService.autoAssign(invoice.getAppointmentID());
        session.setAttribute("flashSuccess", "Đã ghi nhận thu tiền mặt " + formatVnd(amountDue) + "đ.");
        afterPaymentSuccess(session, invoiceId, isSettlementStage, true, change);
        resp.sendRedirect(req.getContextPath() + "/receptionist/invoice/receipt?invoiceId=" + invoiceId + "&from=" + from);
    }

    // 2. Chuyển khoản: toàn bộ amountDue qua QR PayOS
    private void handleBank(HttpSession session, Staff staff, Invoice invoice,
                            BigDecimal amountDue, String from, HttpServletResponse resp) throws Exception {
        int invoiceId = invoice.getInvoiceID();
        String description = "Invoice " + invoiceId;
        String source = "staff:" + from;
        String checkoutUrl = paymentService.createPaymentLink(
                invoiceId, invoice.getAppointmentID(), amountDue.longValue(),
                description, true, source, staff.getStaffID());
        if (checkoutUrl == null || checkoutUrl.isBlank()) {
            session.setAttribute("flashError", "Không thể tạo liên kết thanh toán PayOS. Vui lòng thử lại.");
            resp.sendRedirect("invoice?invoiceId=" + invoiceId + "&from=" + from);
            return;
        }
        session.setAttribute("pendingBankIsFull_" + invoiceId, true);
        resp.sendRedirect(checkoutUrl);
    }

    // 3. Tiền mặt + Chuyển khoản: 1 phần cash + QR cho phần còn lại
    private void handleMixed(HttpServletRequest req, HttpSession session, Staff staff, Invoice invoice,
                             BigDecimal amountDue, String from, HttpServletResponse resp) throws Exception {
        int invoiceId = invoice.getInvoiceID();
        String redirectBack = req.getContextPath() + "/receptionist/invoice?invoiceId=" + invoiceId + "&from=" + from;

        BigDecimal cashPart = parseAmount(req.getParameter("cashPart"));
        if (cashPart == null || cashPart.compareTo(BigDecimal.ZERO) <= 0
                || cashPart.compareTo(amountDue) >= 0) {
            session.setAttribute("flashError",
                    "Số tiền mặt phải lớn hơn 0 và nhỏ hơn tổng số tiền cần thu ("
                            + formatVnd(amountDue) + "đ).");
            resp.sendRedirect(redirectBack);
            return;
        }
        BigDecimal remaining = amountDue.subtract(cashPart);

        // Ghi nhận phần tiền mặt trước
        invoiceDAO.insertPayment(invoiceId, cashPart, "Cash", staff.getStaffID());
        assignmentService.autoAssign(invoice.getAppointmentID());

        // Tạo QR cho đúng phần còn lại
        String description = "Invoice " + invoiceId;
        String source = "staff:" + from;
        String checkoutUrl = paymentService.createPaymentLink(
                invoiceId, invoice.getAppointmentID(), remaining.longValue(),
                description, true, source, staff.getStaffID());
        if (checkoutUrl == null || checkoutUrl.isBlank()) {
            session.setAttribute("flashWarning",
                    "Đã ghi nhận " + formatVnd(cashPart) + "đ tiền mặt, nhưng không tạo được liên kết "
                            + "chuyển khoản cho phần còn lại (" + formatVnd(remaining) + "đ). Vui lòng thử lại.");
            resp.sendRedirect(redirectBack);
            return;
        }
        session.setAttribute("flashSuccess",
                "Đã ghi nhận " + formatVnd(cashPart) + "đ tiền mặt. Vui lòng hoàn tất phần còn lại ("
                        + formatVnd(remaining) + "đ) qua chuyển khoản.");
        session.setAttribute("pendingBankIsFull_" + invoiceId, true);
        resp.sendRedirect(checkoutUrl);
    }

    // Gửi email biên lai
    private void afterPaymentSuccess(HttpSession session, int invoiceId,
                                     boolean isSettlementStage, boolean isFullThisTx, BigDecimal change) {
        session.setAttribute("lastPayIsSettlement_" + invoiceId, isSettlementStage);
        session.setAttribute("lastPayIsFull_" + invoiceId, isFullThisTx);
        if (change.signum() > 0) {
            session.setAttribute("lastCashChangeInvoiceId", invoiceId);
            session.setAttribute("lastCashChange", change);
        }
        try {
            Invoice invoice = invoiceDAO.findById(invoiceId);
            Appointment appt = appointmentDAO.findById(invoice.getAppointmentID());
            Customer customer = customerDAO.findById(invoice.getCustomerID());
            if (customer == null || customer.getEmail() == null || customer.getEmail().isBlank()) return;

            ReceiptData receipt = receiptService.buildReceiptForDownload(invoice, appt, customer);
            if (receipt != null) {
                String pdfUrl = System.getenv().getOrDefault("APP_BASE_URL", "")
                        + "/invoices/pdf?invoiceId=" + invoiceId;
                emailService.sendInvoiceEmail(customer, receipt, pdfUrl);
            }
        } catch (Exception ignored) {
        }
    }

    private String normalizeFrom(String from) {
        return "history".equals(from) ? "history" : "checkin";
    }
    private int parseId(String s) {
        if (s == null || s.isBlank()) return -1;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    private BigDecimal parseAmount(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return new BigDecimal(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    private String formatVnd(BigDecimal amount) {
        return String.format("%,.0f", amount).replace(',', '.');
    }

    private Staff getAuthenticatedReceptionist(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null) { resp.sendRedirect(req.getContextPath() + "/auth/staff/login"); return null; }
        Staff staff = (Staff) session.getAttribute("staff");
        if (staff == null || !"Receptionist".equals(staff.getRoleName())) {
            resp.sendRedirect(req.getContextPath() + "/auth/staff/login"); return null;
        }
        return staff;
    }
}