// petclinic-backend/src/main/java/com/petclinic/backend/service/ReceiptService.java
package com.petclinic.backend.service;

import com.petclinic.backend.dao.AppointmentDAO;
import com.petclinic.backend.dao.InvoiceDAO;
import com.petclinic.backend.dto.ReceiptData;
import com.petclinic.backend.dto.ReceiptLineItem;
import com.petclinic.backend.model.*;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReceiptService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");

    private final InvoiceDAO     invoiceDAO     = new InvoiceDAO();
    private final AppointmentDAO appointmentDAO = new AppointmentDAO();

    //  1. Hóa đơn 1 - trước khi Invoice tồn tại, khách đang ở booking/confirm
    public ReceiptData buildPreBookingInvoice(String customerName,
                                              List<Service> services, List<Vaccine> vaccines) {
        ReceiptData r = new ReceiptData();
        r.setDocumentLabel("HÓA ĐƠN 1");
        r.setInvoiceCode("Chưa phát hành");
        r.setIssuedAtDisplay(java.time.LocalDateTime.now().format(DT_FMT));
        r.setCustomerName(customerName);
        r.setStaffName("Đặt lịch trực tuyến");

        BigDecimal qty = BigDecimal.ZERO, sub = BigDecimal.ZERO;
        if (services != null) {
            for (Service s : services) {
                ReceiptLineItem li = lineOf(s.getName(), BigDecimal.ONE, s.getPrice());
                r.getItems().add(li);
                qty = qty.add(BigDecimal.ONE);
                sub = sub.add(li.getLineTotal());
            }
        }
        if (vaccines != null) {
            for (Vaccine v : vaccines) {
                ReceiptLineItem li = lineOf(v.getName(), BigDecimal.ONE, v.getUnitPrice());
                r.getItems().add(li);
                qty = qty.add(BigDecimal.ONE);
                sub = sub.add(li.getLineTotal());
            }
        }
        r.setTotalQuantity(qty);
        r.setSubTotal(sub);
        r.setDiscountAmount(BigDecimal.ZERO);
        r.setTotalPayable(sub);
        return r;
    }

    // Biến thể cho lịch nội trú
    public ReceiptData buildPreBookingInvoiceInpatient(String customerName, BigDecimal depositAmount) {
        ReceiptData r = new ReceiptData();
        r.setDocumentLabel("HÓA ĐƠN 1");
        r.setInvoiceCode("Chưa phát hành");
        r.setIssuedAtDisplay(java.time.LocalDateTime.now().format(DT_FMT));
        r.setCustomerName(customerName);
        r.setStaffName("Đặt lịch trực tuyến");
        r.getItems().add(lineOf("Đặt cọc nội trú", BigDecimal.ONE, depositAmount));
        r.setTotalQuantity(BigDecimal.ONE);
        r.setSubTotal(depositAmount);
        r.setDiscountAmount(BigDecimal.ZERO);
        r.setTotalPayable(depositAmount);
        return r;
    }

    //  2. "BIÊN LAI LẦN 1" - vừa thanh toán xong lúc đặt lịch (full hoặc 50%)
    public ReceiptData buildPrepayReceipt(Invoice invoice, Appointment appt, Customer customer,
                                          boolean isFullPayment, BigDecimal paidAmountThisTx) {
        ReceiptData r = baseFromInvoice(invoice, appt, customer);
        r.setDocumentLabel("BIÊN LAI LẦN 1");
        r.setPaidStatusLabel("PrePaid");
        r.setPaidAmount(paidAmountThisTx);
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
            ReceiptData r = baseFromInvoice(invoice, appt, customer);
            r.setDocumentLabel("BIÊN LAI LẦN 1");
            r.setPaidStatusLabel("PrePaid");
            r.setPaidAmount(totalPaid);
            BigDecimal remaining = r.getTotalPayable().subtract(totalPaid);
            if (remaining.signum() < 0) remaining = BigDecimal.ZERO;
            r.setRemainingAmount(remaining);
            r.setRemainingDueDate(appt != null ? appt.getFormattedAppointmentDate() : "-");
            r.setNote(remaining.signum() == 0 ? "Trả trước toàn bộ." : "Đặt cọc 50%.");
            return r;
        }
        if ("Paid".equals(status) || "Refunded".equals(status) || "PartiallyRefunded".equals(status)) {
            ReceiptData r = baseFromInvoice(invoice, appt, customer);
            r.setDocumentLabel("BIÊN LAI LẦN 2");
            r.setPaidStatusLabel("Paid");
            r.setPaidAmount(totalPaid);
            r.setChangeAmount(BigDecimal.ZERO);
            r.setNote("Thanh toán thành công.");
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

    private ReceiptData baseFromInvoice(Invoice invoice, Appointment appt, Customer customer) {
        ReceiptData r = new ReceiptData();
        r.setInvoiceIdRaw(invoice.getInvoiceID());
        r.setInvoiceCode(invoice.getInvoiceCode() != null
                ? invoice.getInvoiceCode() : "INV" + String.format("%06d", invoice.getInvoiceID()));
        r.setCustomerName(customer != null ? customer.getFullName()
                : (appt != null && appt.getCustomerName() != null ? appt.getCustomerName() : "Khách vãng lai"));

        List<Payment> payments = invoice.getPayments(); // ORDER BY PaidAt DESC
        Payment last = payments.isEmpty() ? null : payments.get(0);
        r.setIssuedAtDisplay(last != null && last.getPaidAt() != null
                ? last.getPaidAt().format(DT_FMT)
                : java.time.LocalDateTime.now().format(DT_FMT));
        r.setStaffName(last != null && last.getProcessedByName() != null
                ? last.getProcessedByName() : "Hệ thống (tự động)");

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
        return r;
    }

    private BigDecimal sumPayments(Invoice invoice) {
        BigDecimal sum = BigDecimal.ZERO;
        if (invoice.getPayments() == null) return sum;
        for (Payment p : invoice.getPayments()) if (p.getAmount() != null) sum = sum.add(p.getAmount());
        return sum;
    }

    //  RENDER - HTML (dùng chung cho PDF.
    public String renderHtml(ReceiptData r) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><meta charset=\"UTF-8\"/><style>")
                .append("body{font-family:'ArialCustom',sans-serif;font-size:12px;color:#1a1714;margin:24px;}")
                .append("h1{text-align:center;font-size:18px;margin:0 0 4px;letter-spacing:1px;}")
                .append(".sub{text-align:center;font-size:11px;color:#555;margin-bottom:14px;}")
                .append("table{width:100%;}")
                .append(".meta td{padding:2px 0;font-size:12px;}")
                .append(".meta .lbl{color:#555;width:150px;}")
                .append("table.items{border-collapse:collapse;margin-top:12px;}")
                .append("table.items th{border-bottom:1.5px solid #333;padding:6px 4px;font-size:11px;text-align:left;}")
                .append("table.items td{border-bottom:1px solid #ddd;padding:6px 4px;font-size:11.5px;}")
                .append(".num{text-align:right;}")
                .append(".itemname{font-weight:600;padding-top:9px;border-bottom:none;}")
                .append(".totals td{padding:3px 0;font-size:12.5px;}")
                .append(".totals .val{text-align:right;font-weight:bold;}")
                .append(".grand{font-size:15px;color:#0f3d24;}")
                .append(".grand td{border-top:1.5px solid #333;padding-top:8px;}")
                .append(".note{margin-top:16px;font-size:11px;color:#555;text-align:center;font-style:italic;}")
                .append(".thanks{margin-top:6px;text-align:center;font-weight:bold;font-size:13px;color:#0f3d24;}")
                .append("</style></head><body>");

        sb.append("<h1>").append(esc(r.getDocumentLabel())).append("</h1>");
        sb.append("<div class=\"sub\">PetClinic – 123 Đường ABC, TP. Hà Nội – (028) 123 456 789</div>");

        sb.append("<table class=\"meta\">");
        sb.append(row("Mã đơn hàng:", esc(r.getInvoiceCode())));
        sb.append(row("Ngày/giờ:",    esc(r.getIssuedAtDisplay())));
        sb.append(row("Nhân viên:",   esc(r.getStaffName())));
        sb.append(row("Khách hàng:",  esc(r.getCustomerName())));
        sb.append("</table>");

        sb.append("<table class=\"items\">");
        sb.append("<tr><th>Sản phẩm / Dịch vụ</th><th class=\"num\">SL</th><th class=\"num\">Đ.Giá</th>")
                .append("<th class=\"num\">CK</th><th class=\"num\">T.Tiền</th></tr>");
        for (ReceiptLineItem li : r.getItems()) {
            sb.append("<tr><td colspan=\"5\" class=\"itemname\">").append(esc(li.getName())).append("</td></tr>");
            sb.append("<tr><td></td><td class=\"num\">").append(fmt(li.getQuantity()))
                    .append("</td><td class=\"num\">").append(fmtMoney(li.getUnitPrice()))
                    .append("</td><td class=\"num\">").append(fmtMoney(li.getDiscount()))
                    .append("</td><td class=\"num\">").append(fmtMoney(li.getLineTotal())).append("</td></tr>");
        }
        sb.append("</table>");

        sb.append("<table class=\"totals\" style=\"margin-top:10px;\">");
        sb.append(totalRow("Tổng số lượng", fmt(r.getTotalQuantity())));
        sb.append(totalRow("Tổng tiền hàng", fmtMoney(r.getSubTotal()) + "đ"));
        sb.append(totalRow("Chiết khấu", fmtMoney(r.getDiscountAmount()) + "đ"));
        sb.append(totalRowCls("Tổng phải trả", fmtMoney(r.getTotalPayable()) + "đ", "grand"));

        if ("HÓA ĐƠN".equals(r.getDocumentLabel()) && r.getPrepaidAmount() != null) {
            sb.append(totalRow("Đã trả trước", fmtMoney(r.getPrepaidAmount()) + "đ"));
            sb.append(totalRowCls("Khách còn phải trả", fmtMoney(r.getAmountDue()) + "đ", "grand"));
            sb.append(totalRow("Tiền trả lại", fmtMoney(r.getChangeAmount()) + "đ"));
        }

        if (r.getPaidStatusLabel() != null) {
            sb.append(totalRow("Đã trả (" + esc(r.getPaidStatusLabel()) + ")", fmtMoney(r.getPaidAmount()) + "đ"));
        }
        if (r.getRemainingAmount() != null) {
            sb.append(totalRow("Còn phải thanh toán vào ngày " + esc(r.getRemainingDueDate()),
                    fmtMoney(r.getRemainingAmount()) + "đ"));
        }
        if ("BIÊN LAI LẦN 2".equals(r.getDocumentLabel())) {
            sb.append(totalRow("Tiền trả lại", fmtMoney(r.getChangeAmount()) + "đ"));
        }
        sb.append("</table>");

        if (r.getNote() != null && !r.getNote().isBlank()) {
            sb.append("<div class=\"note\">Ghi chú: ").append(esc(r.getNote())).append("</div>");
        }
        sb.append("<div class=\"note\">Quý khách được phép khiếu nại hoàn tiền trong vòng 48h kể từ ngày thanh toán.</div>");
        sb.append("<div class=\"thanks\">CẢM ƠN QUÝ KHÁCH VÀ HẸN GẶP LẠI!</div>");
        sb.append("</body></html>");
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
}