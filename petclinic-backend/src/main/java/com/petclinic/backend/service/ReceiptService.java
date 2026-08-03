// petclinic-backend/src/main/java/com/petclinic/backend/service/ReceiptService.java
package com.petclinic.backend.service;

import com.petclinic.backend.dao.AppointmentDAO;
import com.petclinic.backend.dao.InvoiceDAO;
import com.petclinic.backend.dao.RefundDAO;
import com.petclinic.backend.dao.StaffDAO;
import com.petclinic.backend.dto.ReceiptData;
import com.petclinic.backend.dto.ReceiptLineItem;
import com.petclinic.backend.model.*;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import com.petclinic.backend.util.VietnameseNumberUtil;

public class ReceiptService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");

    private final InvoiceDAO     invoiceDAO     = new InvoiceDAO();
    private final AppointmentDAO appointmentDAO = new AppointmentDAO();
    private final RefundDAO      refundDAO      = new RefundDAO();
    private final StaffDAO staffDAO       = new StaffDAO();

    private static final String BIZ_NAME    = "PetClinic";
    private static final String BIZ_ADDRESS = "123 Đường ABC, TP. Hà Nội";
    private static final String BIZ_HOTLINE = "(028) 123 456 789";
    private static final String BIZ_EMAIL   = "petclinicweb123@gmail.com";

    public ReceiptData buildReceiptForPayment(Invoice invoice, Appointment appt, Customer customer, Payment payment) {
        ReceiptData r = baseFromInvoice(invoice, appt, customer);

        List<Payment> payments = invoice.getPayments(); // DESC by PaidAt
        boolean isEarliest = !payments.isEmpty()
                && payment.getPaymentID() == payments.get(payments.size() - 1).getPaymentID();

        BigDecimal cumulativeThroughThis = BigDecimal.ZERO;
        for (int i = payments.size() - 1; i >= 0; i--) {
            Payment p = payments.get(i);
            cumulativeThroughThis = cumulativeThroughThis.add(p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO);
            if (p.getPaymentID() == payment.getPaymentID()) break;
        }
        boolean completesTotal = invoice.getTotalAmount() != null
                && cumulativeThroughThis.compareTo(invoice.getTotalAmount()) >= 0;

        r.setDocumentLabel(completesTotal ? "BIÊN LAI LẦN 2" : "BIÊN LAI LẦN 1");
        r.setPaidStatusLabel(completesTotal ? "Paid" : "PrePaid");
        r.setPaymentCode(payment.getPaymentCode());
        r.setIssuedAtDisplay(payment.getPaidAt() != null ? payment.getPaidAt().format(DT_FMT) : r.getIssuedAtDisplay());
        r.setStaffName(payment.getProcessedByName() != null ? payment.getProcessedByName() : "Hệ thống (tự động)");
        r.setPaymentMethodDisplay(mapPaymentMethod(payment.getMethod()));
        r.setPaidAmount(payment.getAmount());
        r.setAmountInWords(VietnameseNumberUtil.readMoney(payment.getAmount()));

        if (!completesTotal) {
            BigDecimal remaining = invoice.getTotalAmount() != null
                    ? invoice.getTotalAmount().subtract(cumulativeThroughThis) : BigDecimal.ZERO;
            if (remaining.signum() < 0) remaining = BigDecimal.ZERO;
            r.setRemainingAmount(remaining);
            r.setRemainingDueDate(appt != null ? appt.getFormattedAppointmentDate() : "-");
        }
        r.setNote(completesTotal ? "Thanh toán hoàn tất."
                : (isEarliest ? "Đặt cọc / trả trước." : "Thanh toán một phần."));
        return r;
    }

    //  2. "BIÊN LAI LẦN 1" - vừa thanh toán xong lúc đặt lịch (full hoặc 50%)
    public ReceiptData buildPrepayReceipt(Invoice invoice, Appointment appt, Customer customer,
                                          boolean isFullPayment, BigDecimal paidAmountThisTx) {
        ReceiptData r = baseFromInvoice(invoice, appt, customer);
        r.setDocumentLabel("BIÊN LAI LẦN 1");
        r.setPaidStatusLabel("PrePaid");
        r.setPaidAmount(paidAmountThisTx);
        r.setAmountInWords(VietnameseNumberUtil.readMoney(paidAmountThisTx));
        BigDecimal remaining = r.getTotalPayable().subtract(paidAmountThisTx);
        if (remaining.signum() < 0) remaining = BigDecimal.ZERO;
        r.setRemainingAmount(remaining);
        r.setRemainingDueDate(appt != null ? appt.getFormattedAppointmentDate() : "-");
        r.setNote(isFullPayment ? "Trả trước toàn bộ." : "Đặt cọc 50%.");
        return r;
    }

    //  3. "HÓA ĐƠN" — appointment đã Done, chờ thu phần còn lại
    public ReceiptData buildSettlementInvoice(Invoice invoice, Appointment appt, Customer customer) {
        ReceiptData r = baseFromInvoice(invoice, appt, customer);
        r.setDocumentLabel("HÓA ĐƠN");
        BigDecimal paidSoFar = sumPayments(invoice);
        r.setPrepaidAmount(paidSoFar);
        BigDecimal due = r.getTotalPayable().subtract(paidSoFar);
        if (due.signum() < 0) due = BigDecimal.ZERO;
        r.setAmountDue(due);
        r.setChangeAmount(BigDecimal.ZERO);
        return r;
    }

    //  4. "BIÊN LAI LẦN 2" — vừa thu xong phần còn lại (thanh toán hoàn tất)
    public ReceiptData buildSettlementReceipt(Invoice invoice, Appointment appt, Customer customer,
                                              BigDecimal justPaidAmount, BigDecimal changeAmount) {
        ReceiptData r = baseFromInvoice(invoice, appt, customer);
        r.setDocumentLabel("BIÊN LAI LẦN 2");
        r.setPaidStatusLabel("Paid");
        r.setPaidAmount(justPaidAmount);
        r.setAmountInWords(VietnameseNumberUtil.readMoney(justPaidAmount));
        r.setChangeAmount(changeAmount == null ? BigDecimal.ZERO : changeAmount);
        r.setNote("Thanh toán thành công.");
        return r;
    }

    //  5. "Xem lại / tải PDF" bất kỳ lúc nào từ 1 Invoice đã lưu
    public ReceiptData buildReceiptForDownload(int invoiceId) throws SQLException {
        Invoice invoice = invoiceDAO.findById(invoiceId);
        if (invoice == null) return null;
        Appointment appt = appointmentDAO.findById(invoice.getAppointmentID());
        return buildReceiptForDownload(invoice, appt, null);
    }

    public ReceiptData buildReceiptForDownload(Invoice invoice, Appointment appt, Customer customer) {
        String status = invoice.getStatus();
        BigDecimal totalPaid = sumPayments(invoice);

        if ("PrePaid".equals(status)) {
            ReceiptData r = buildPrepayReceipt(invoice, appt, customer, false, totalPaid);
            return r;
        }
        if ("Paid".equals(status) || "Refunded".equals(status) || "PartiallyRefunded".equals(status)) {
            ReceiptData r = buildSettlementReceipt(invoice, appt, customer, totalPaid, BigDecimal.ZERO);
            return r;
        }
        return null;
    }

    // "HÓA ĐƠN 1" xem trước - dùng ở màn hình lễ tân khi Invoice ĐÃ tồn tại nhưng chưa thu tiền.
    public ReceiptData buildPreInvoicePreview(Invoice invoice, Appointment appt, Customer customer) {
        ReceiptData r = baseFromInvoice(invoice, appt, customer);
        r.setDocumentLabel("HÓA ĐƠN 1");
        return r;
    }


    private ReceiptLineItem lineOf(String name, BigDecimal qty, BigDecimal price) {
        ReceiptLineItem li = new ReceiptLineItem();
        li.setName(name);
        li.setQuantity(qty);
        li.setUnitPrice(price == null ? BigDecimal.ZERO : price);
        li.setDiscount(BigDecimal.ZERO);
        li.setLineTotal(qty.multiply(price == null ? BigDecimal.ZERO : price));
        return li;
    }

    public ReceiptData buildRefundReceipt(Refund refund) throws SQLException {
        ReceiptData r = new ReceiptData();
        r.setDocumentLabel("BIÊN LAI HOÀN TIỀN");
        r.setInvoiceIdRaw(refund.getRefundID());
        r.setInvoiceCode("REF-" + String.format("%06d", refund.getRefundID()));

        Invoice linkedInvoice = invoiceDAO.findByAppointment(refund.getAppointmentID());
        String linkedInvoiceCode = (linkedInvoice != null && linkedInvoice.getInvoiceCode() != null)
                ? linkedInvoice.getInvoiceCode()
                : ("INV" + String.format("%06d", refund.getAppointmentID()));
        int refundSeq = refundDAO.countRefundsUpToForAppointment(refund.getAppointmentID(), refund.getRefundID());
        String refundSuffix = refundSeq >= 2 ? "-02" : "-01";
        r.setPaymentCode("REF-" + linkedInvoiceCode + refundSuffix);

        r.setIssuedAtDisplay(refund.getRefundedAt() != null
                ? refund.getRefundedAt().format(DT_FMT) : java.time.LocalDateTime.now().format(DT_FMT));
        r.setCustomerName(refund.getCustomerName());
        r.setCustomerPhone(refund.getCustomerPhone());
        r.setPetName(refund.getPetName());
        r.setStaffName(staffDAO.findById(refund.getProcessedByID()).getFullName());
        r.setPaymentMethodDisplay("CK");
        r.setPaidAmount(refund.getPaidAmount());
        r.setAmountInWords(VietnameseNumberUtil.readMoney(refund.getPaidAmount()));
        r.setPurposeText("Hoàn tiền lịch hẹn #" + refund.getAppointmentID());
        r.setNote(refund.getReason() != null && !refund.getReason().isBlank()
                ? "Lý do: " + refund.getReason() : null);
        return r;
    }

    private ReceiptData baseFromInvoice(Invoice invoice, Appointment appt, Customer customer) {
        ReceiptData r = new ReceiptData();
        r.setInvoiceIdRaw(invoice.getInvoiceID());
        r.setInvoiceCode(invoice.getInvoiceCode() != null
                ? invoice.getInvoiceCode() : "INV" + String.format("%06d", invoice.getInvoiceID()));
        r.setCustomerName(customer != null ? customer.getFullName()
                : (appt != null && appt.getCustomerName() != null ? appt.getCustomerName() : "Khách vãng lai"));
        r.setCustomerPhone(customer != null ? customer.getPhone() : null);
        r.setPetName(appt != null ? appt.getPetName() : null);

        List<Payment> payments = invoice.getPayments(); // ORDER BY PaidAt DESC
        Payment last = payments.isEmpty() ? null : payments.get(0);
        r.setIssuedAtDisplay(last != null && last.getPaidAt() != null
                ? last.getPaidAt().format(DT_FMT)
                : java.time.LocalDateTime.now().format(DT_FMT));
        r.setStaffName(last != null && last.getProcessedByName() != null
                ? last.getProcessedByName() : "Hệ thống (tự động)");
        r.setPaymentCode(last != null ? last.getPaymentCode() : null);
        r.setPaymentMethodDisplay(mapPaymentMethod(last != null ? last.getMethod() : null));

        BigDecimal qty = BigDecimal.ZERO, itemsSum = BigDecimal.ZERO;
        for (InvoiceItem it : invoice.getItems()) {
            ReceiptLineItem li = new ReceiptLineItem();
            li.setName(it.getDescription());
            li.setQuantity(it.getQuantity());
            li.setUnitPrice(it.getUnitPrice());
            li.setDiscount(BigDecimal.ZERO);
            li.setLineTotal(it.getLineTotal());
            r.getItems().add(li);
            if (it.getQuantity() != null)  qty = qty.add(it.getQuantity());
            if (it.getLineTotal() != null) itemsSum = itemsSum.add(it.getLineTotal());
        }

        BigDecimal total = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : itemsSum;
        BigDecimal extra = total.subtract(itemsSum);
        if (extra.signum() > 0 && invoice.getOtherFees() != null && !invoice.getOtherFees().isBlank()) {
            ReceiptLineItem fee = new ReceiptLineItem();
            fee.setName("Phụ phí (" + invoice.getOtherFees() + ")");
            fee.setQuantity(BigDecimal.ONE);
            fee.setUnitPrice(extra);
            fee.setLineTotal(extra);
            r.getItems().add(fee);
            qty = qty.add(BigDecimal.ONE);
        }

        r.setTotalQuantity(qty);
        r.setSubTotal(total);
        r.setDiscountAmount(BigDecimal.ZERO);
        r.setTotalPayable(total);
        r.setPurposeText(buildPurposeText(appt));
        return r;
    }

    private BigDecimal sumPayments(Invoice invoice) {
        BigDecimal sum = BigDecimal.ZERO;
        if (invoice.getPayments() == null) return sum;
        for (Payment p : invoice.getPayments()) if (p.getAmount() != null) sum = sum.add(p.getAmount());
        return sum;
    }

    public String renderHtml(ReceiptData r) {
        boolean isReceipt = r.getDocumentLabel() != null && r.getDocumentLabel().startsWith("BIÊN LAI");
        boolean showClosingNote = "BIÊN LAI LẦN 1".equals(r.getDocumentLabel())
                || "BIÊN LAI LẦN 2".equals(r.getDocumentLabel());

        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><meta charset=\"UTF-8\"/><style>").append(commonCss()).append("</style></head><body>");
        sb.append(buildHeaderBlock(r));
        sb.append(isReceipt ? buildReceiptBody(r) : buildInvoiceBody(r));
        if (showClosingNote) {
            sb.append("<div class=\"thanks\">CẢM ƠN QUÝ KHÁCH VÀ HẸN GẶP LẠI!</div>");
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    private String commonCss() {
        return "body{font-family:'ArialCustom',sans-serif;font-size:12px;color:#1a1714;margin:24px;}"
                + ".biz-name{text-align:center;font-size:16px;font-weight:bold;color:#0f3d24;}"
                + ".biz-sub{text-align:center;font-size:10.5px;color:#555;margin-bottom:10px;}"
                + "h1{text-align:center;font-size:17px;margin:8px 0 2px;letter-spacing:1px;}"
                + ".sub{text-align:center;font-size:11px;color:#555;margin-bottom:14px;font-weight:bold;}"
                + ".section-title{font-size:11.5px;font-weight:bold;color:#0f3d24;margin:14px 0 4px;"
                +   "border-bottom:1px solid #ccc;padding-bottom:3px;}"
                + "table{width:100%;}"
                + ".meta td{padding:2px 0;font-size:12px;}"
                + ".meta .lbl{color:#555;width:160px;}"
                + "table.items{border-collapse:collapse;margin-top:6px;}"
                + "table.items th{border-bottom:1.5px solid #333;padding:6px 4px;font-size:11px;text-align:left;}"
                + "table.items td{border-bottom:1px solid #ddd;padding:6px 4px;font-size:11.5px;}"
                + ".num{text-align:right;}"
                + ".totals .val{text-align:right;font-weight:bold;}"
                + ".totals td{padding:3px 0;font-size:12.5px;}"
                + ".grand{font-size:14px;color:#0f3d24;}"
                + ".grand td{border-top:1.5px solid #333;padding-top:8px;}"
                + ".amount-words{margin-top:8px;font-size:12px;font-style:italic;}"
                + ".sign-table{margin-top:28px;}"
                + ".sign-col{width:50%;text-align:center;vertical-align:top;font-size:12px;}"
                + ".sign-title{font-weight:bold;}"
                + ".sign-hint{font-size:10.5px;color:#666;margin-top:2px;}"
                + ".sign-space{height:56px;}"
                + ".sign-name{font-weight:bold;}"
                + ".note{margin-top:14px;font-size:11px;color:#555;text-align:center;font-style:italic;}"
                + ".thanks{margin-top:6px;text-align:center;font-weight:bold;font-size:13px;color:#0f3d24;}";
    }

    private String buildHeaderBlock(ReceiptData r) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"biz-name\">").append(BIZ_NAME).append("</div>");
        sb.append("<div class=\"biz-sub\">").append(esc(BIZ_ADDRESS))
                .append(" &#160;|&#160; Hotline: ").append(esc(BIZ_HOTLINE))
                .append(" &#160;|&#160; Email: ").append(esc(BIZ_EMAIL)).append("</div>");
        sb.append("<h1>").append(esc(r.getDocumentLabel())).append("</h1>");
        if (r.getDocumentLabel() != null && r.getDocumentLabel().startsWith("BIÊN LAI")) {
            sb.append("<div class=\"sub\">").append(receiptSubtitle(r.getDocumentLabel())).append("</div>");
        }
        return sb.toString();
    }

    private String receiptSubtitle(String label) {
        if ("BIÊN LAI LẦN 1".equals(label)) return "THU TIỀN ĐẶT CỌC / TRẢ TRƯỚC";
        if ("BIÊN LAI LẦN 2".equals(label)) return "QUYẾT TOÁN HOÀN THÀNH DỊCH VỤ";
        if ("BIÊN LAI HOÀN TIỀN".equals(label)) return "XÁC NHẬN CHUYỂN KHOẢN HOÀN TIỀN CHO KHÁCH HÀNG";
        return "";
    }

    private String buildInvoiceBody(ReceiptData r) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table class=\"meta\">");
        sb.append(row("Mã hóa đơn:", esc(r.getInvoiceCode())));
        sb.append(row("Ngày/giờ:",   esc(r.getIssuedAtDisplay())));
        sb.append(row("Nhân viên:",  esc(r.getStaffName())));
        sb.append("</table>");

        sb.append("<div class=\"section-title\">THÔNG TIN KHÁCH HÀNG</div><table class=\"meta\">");
        sb.append(row("Khách hàng:", esc(r.getCustomerName())));
        sb.append(row("Điện thoại:", r.getCustomerPhone() != null ? esc(r.getCustomerPhone()) : "-"));
        sb.append("</table>");

        if (r.getPetName() != null && !r.getPetName().isBlank()) {
            sb.append("<div class=\"section-title\">THÔNG TIN THÚ CƯNG</div><table class=\"meta\">");
            sb.append(row("Tên Pet:", esc(r.getPetName())));
            sb.append("</table>");
        }

        sb.append("<div class=\"section-title\">CHI TIẾT DỊCH VỤ &amp; SẢN PHẨM</div>");
        sb.append("<table class=\"items\">");
        sb.append("<tr><th>STT</th><th>Tên Dịch vụ / Sản phẩm</th><th class=\"num\">SL</th>")
                .append("<th class=\"num\">Đơn giá (đ)</th><th class=\"num\">Thành tiền (đ)</th></tr>");
        int idx = 1;
        for (ReceiptLineItem li : r.getItems()) {
            sb.append("<tr><td>").append(idx++).append("</td><td>").append(esc(li.getName())).append("</td>")
                    .append("<td class=\"num\">").append(fmt(li.getQuantity())).append("</td>")
                    .append("<td class=\"num\">").append(fmtMoney(li.getUnitPrice())).append("</td>")
                    .append("<td class=\"num\">").append(fmtMoney(li.getLineTotal())).append("</td></tr>");
        }
        sb.append("</table>");

        sb.append("<table class=\"totals\" style=\"margin-top:10px;\">");
        sb.append(totalRow("Tạm tính:", fmtMoney(r.getSubTotal()) + " đ"));
        sb.append(totalRow("Chiết khấu:", fmtMoney(r.getDiscountAmount()) + " đ"));
        sb.append(totalRowCls("TỔNG GIÁ TRỊ HĐ:", fmtMoney(r.getTotalPayable()) + " đ", "grand"));
        if (r.getPrepaidAmount() != null) {
            sb.append(totalRow("Đã trả trước:", fmtMoney(r.getPrepaidAmount()) + " đ"));
            sb.append(totalRowCls("CÒN LẠI PHẢI THANH TOÁN:", fmtMoney(r.getAmountDue()) + " đ", "grand"));
        }
        sb.append("</table>");

        if (r.getNote() != null && !r.getNote().isBlank()) {
            sb.append("<div class=\"note\">* Ghi chú: ").append(esc(r.getNote())).append("</div>");
        }
        return sb.toString();
    }

    private String buildReceiptBody(ReceiptData r) {
        StringBuilder sb = new StringBuilder();
        boolean isRefund = "BIÊN LAI HOÀN TIỀN".equals(r.getDocumentLabel());
        String suffix = "BIÊN LAI LẦN 2".equals(r.getDocumentLabel()) ? "-02" : "-01";
        String receiptCode = (r.getPaymentCode() != null && !r.getPaymentCode().isBlank())
                ? r.getPaymentCode() : ("BL-" + r.getInvoiceIdRaw() + suffix);

        sb.append("<table class=\"meta\">");
        sb.append(row(isRefund ? "Mã yêu cầu hoàn tiền:" : "Liên kết Hóa đơn tổng:", esc(r.getInvoiceCode())));
        sb.append(row("Mã biên lai:", esc(receiptCode)));
        sb.append(row("Thời gian:", esc(r.getIssuedAtDisplay())));
        sb.append(row(isRefund ? "Khách nhận tiền hoàn:" : "Người nộp tiền:",
                esc(r.getCustomerName()) + (r.getCustomerPhone() != null ? " (" + esc(r.getCustomerPhone()) + ")" : "")));
        sb.append(row(isRefund ? "Nội dung hoàn tiền:" : "Nội dung thu:", esc(r.getPurposeText())));
        sb.append(row(isRefund ? "Hình thức hoàn tiền:" : "Hình thức thanh toán:", esc(r.getPaymentMethodDisplay())));
        sb.append("</table>");

        sb.append("<table class=\"totals\" style=\"margin-top:10px;\">");
        sb.append(totalRowCls(isRefund ? "Số tiền đã hoàn:" : "Số tiền thực thu:", fmtMoney(r.getPaidAmount()) + " đ", "grand"));
        sb.append("</table>");
        sb.append("<div class=\"amount-words\">Bằng chữ: <em>").append(esc(r.getAmountInWords())).append("</em></div>");

        if (r.getRemainingAmount() != null && r.getRemainingAmount().signum() > 0) {
            sb.append("<div class=\"note\">Còn lại phải thanh toán vào ngày ")
                    .append(esc(r.getRemainingDueDate())).append(": ")
                    .append(fmtMoney(r.getRemainingAmount())).append(" đ</div>");
        }

        String payerLabel = isRefund ? "Người chi tiền (PetClinic)" : "Người nộp tiền";
        String payerName  = isRefund ? r.getStaffName() : r.getCustomerName();
        String payeeLabel = isRefund ? "Người nhận tiền hoàn" : "Người thu tiền / Thủ quỹ";
        String payeeName  = isRefund ? r.getCustomerName() : r.getStaffName();

        sb.append("<table class=\"sign-table\"><tr>")
                .append("<td class=\"sign-col\"><div class=\"sign-title\">").append(payerLabel).append("</div>")
                .append("<div class=\"sign-hint\">(Ký, ghi rõ họ tên nếu cần)</div>")
                .append("<div class=\"sign-space\"></div><div class=\"sign-name\">").append(esc(payerName)).append("</div></td>")
                .append("<td class=\"sign-col\"><div class=\"sign-title\">").append(payeeLabel).append("</div>")
                .append("<div class=\"sign-hint\">(Hệ thống xác thực điện tử)</div>")
                .append("<div class=\"sign-space\"></div><div class=\"sign-name\">").append(esc(payeeName)).append("</div></td>")
                .append("</tr></table>");
        return sb.toString();
    }

    public byte[] renderPdf(ReceiptData r) throws Exception {
        String html = renderHtml(r);
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            if (getClass().getResource("/fonts/arial.ttf") != null) {
                builder.useFont(() -> getClass().getResourceAsStream("/fonts/arial.ttf"),
                        "ArialCustom", 400, PdfRendererBuilder.FontStyle.NORMAL, true);
            }

            if (getClass().getResource("/fonts/arialbd.ttf") != null) {
                builder.useFont(() -> getClass().getResourceAsStream("/fonts/arialbd.ttf"),
                        "ArialCustom", 700, PdfRendererBuilder.FontStyle.NORMAL, true);
            } else {
                // Mẹo: Nếu không có file Arial-Bold.ttf, bạn có thể ép dùng tạm file Arial.ttf cho cả chữ đậm
                builder.useFont(() -> getClass().getResourceAsStream("/fonts/arial.ttf"),
                        "ArialCustom", 700, PdfRendererBuilder.FontStyle.NORMAL, true);
            }
            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        }
    }

    private String row(String label, String value) {
        return "<tr><td class=\"lbl\">" + label + "</td><td>" + value + "</td></tr>";
    }
    private String totalRow(String label, String value) {
        return "<tr><td class=\"lbl\">" + label + "</td><td class=\"val\">" + value + "</td></tr>";
    }
    private String totalRowCls(String label, String value, String cls) {
        return "<tr class=\"" + cls + "\"><td class=\"lbl\">" + label + "</td><td class=\"val\">" + value + "</td></tr>";
    }
    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }
    private String fmt(BigDecimal v) {
        return v == null ? "0" : v.stripTrailingZeros().toPlainString();
    }
    private String fmtMoney(BigDecimal v) {
        return v == null ? "0" : String.format("%,.0f", v.doubleValue());
    }

    // Helper mới
    private String mapPaymentMethod(String method) {
        if (method == null) return "-";
        return switch (method) {
            case "Cash" -> "TM";
            case "BankTransfer" -> "CK";
            case "E-Wallet" -> "CK";
            default -> method;
        };
    }

    private String buildPurposeText(Appointment appt) {
        if (appt == null) return "Thanh toán dịch vụ tại PetClinic";
        StringBuilder sb = new StringBuilder("Thanh toán dịch vụ");
        if (appt.getServiceName() != null && !appt.getServiceName().isBlank())
            sb.append(" ").append(appt.getServiceName());
        if (appt.getPetName() != null && !appt.getPetName().isBlank())
            sb.append(" cho thú cưng ").append(appt.getPetName());
        sb.append(" - Lịch hẹn #").append(appt.getAppointmentID());
        return sb.toString();
    }
}