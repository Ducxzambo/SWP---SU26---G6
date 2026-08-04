package com.petclinic.admin.servlet.manager.inventory;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.petclinic.backend.dao.StockImportReceiptDAO;
import com.petclinic.backend.model.*;
import com.petclinic.backend.util.StaffAuthUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.awt.Color;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@WebServlet({
    "/admin/inventory/receipts",
    "/admin/inventory/receipts/detail",
    "/admin/inventory/receipts/pdf"
})
public class StockReceiptServlet extends HttpServlet {

    private final StockImportReceiptDAO receiptDAO = new StockImportReceiptDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if (StaffAuthUtil.requireManager(req, resp) == null) {
            return;
        }

        try {
            String path = req.getServletPath();

            if (path.endsWith("/pdf")) {
                writePdf(receiptDAO.findById(parseId(req)), resp);
                return;
            }

            if (path.endsWith("/detail")) {
                StockImportReceipt receipt = receiptDAO.findById(parseId(req));

                if (receipt == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }

                req.setAttribute("receipt", receipt);
                req.getRequestDispatcher(
                        "/WEB-INF/views/manager/inventory/receipt-detail.jsp")
                        .forward(req, resp);
                return;
            }

            req.setAttribute("receipts", receiptDAO.findRecent());
            req.getRequestDispatcher(
                    "/WEB-INF/views/manager/inventory/receipts.jsp")
                    .forward(req, resp);

        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private int parseId(HttpServletRequest req) {
        return Integer.parseInt(req.getParameter("id"));
    }

    private void writePdf(StockImportReceipt receipt,
                          HttpServletResponse resp) throws Exception {

        if (receipt == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        resp.setContentType("application/pdf");
        resp.setHeader(
                "Content-Disposition",
                "attachment; filename=" + receipt.getReceiptCode() + ".pdf"
        );

        Document document = new Document(PageSize.A4, 36, 36, 42, 42);
        PdfWriter.getInstance(document, resp.getOutputStream());
        document.open();

        BaseFont base = createFont();

        Font title = new Font(
                base,
                18,
                Font.BOLD,
                new Color(5, 72, 63)
        );

        Font normal = new Font(base, 10);
        Font bold = new Font(base, 10, Font.BOLD);

        Paragraph heading = new Paragraph(
                "PETCLINIC - PHIẾU NHẬP KHO",
                title
        );

        heading.setAlignment(Element.ALIGN_CENTER);
        heading.setSpacingAfter(16);

        document.add(heading);

        document.add(new Paragraph(
                "Mã phiếu: " + receipt.getReceiptCode(),
                bold
        ));

        document.add(new Paragraph(
                "Nhà cung cấp: " + receipt.getProviderName(),
                normal
        ));

        document.add(new Paragraph(
                "Nhân viên: " + receipt.getPerformedByName(),
                normal
        ));

        document.add(new Paragraph(
                "Ngày nhập: " + receipt.getImportedAt().format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                ),
                normal
        ));

        PdfPTable table = new PdfPTable(
                new float[]{1, 2, 5, 2, 2.5f, 3}
        );

        table.setWidthPercentage(100);
        table.setSpacingBefore(16);

        String[] headers = {
                "#",
                "Loại",
                "Item",
                "SL",
                "Đơn giá",
                "Thành tiền"
        };

        for (String h : headers) {
            addCell(
                    table,
                    h,
                    bold,
                    new Color(12, 123, 107),
                    Color.WHITE,
                    Element.ALIGN_CENTER
            );
        }

        int index = 1;

        NumberFormat money = NumberFormat.getNumberInstance(
                new Locale("vi", "VN")
        );

        for (StockImportReceiptDetail d : receipt.getDetails()) {

            addCell(
                    table,
                    String.valueOf(index++),
                    normal,
                    Color.WHITE,
                    Color.BLACK,
                    Element.ALIGN_CENTER
            );

            addCell(
                    table,
                    d.getItemType(),
                    normal,
                    Color.WHITE,
                    Color.BLACK,
                    Element.ALIGN_LEFT
            );

            addCell(
                    table,
                    d.getItemName(),
                    normal,
                    Color.WHITE,
                    Color.BLACK,
                    Element.ALIGN_LEFT
            );

            addCell(
                    table,
                    String.valueOf(d.getQuantity()),
                    normal,
                    Color.WHITE,
                    Color.BLACK,
                    Element.ALIGN_RIGHT
            );

            addCell(
                    table,
                    money.format(d.getUnitPrice()),
                    normal,
                    Color.WHITE,
                    Color.BLACK,
                    Element.ALIGN_RIGHT
            );

            addCell(
                    table,
                    money.format(d.getLineTotal()),
                    normal,
                    Color.WHITE,
                    Color.BLACK,
                    Element.ALIGN_RIGHT
            );
        }

        document.add(table);

        Paragraph total = new Paragraph(
                "TỔNG TIỀN: "
                        + money.format(receipt.getTotalAmount())
                        + " VNĐ",
                bold
        );

        total.setAlignment(Element.ALIGN_RIGHT);
        total.setSpacingBefore(14);

        document.add(total);

        document.close();
    }

    private BaseFont createFont() throws Exception {

        String path = "C:/Windows/Fonts/arial.ttf";

        try {
            return BaseFont.createFont(
                    path,
                    BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED
            );
        } catch (Exception e) {
            return BaseFont.createFont(
                    BaseFont.HELVETICA,
                    BaseFont.WINANSI,
                    false
            );
        }
    }

    private void addCell(PdfPTable table,
                         String text,
                         Font font,
                         Color bg,
                         Color fg,
                         int align) {

        Font cellFont = new Font(
                font.getBaseFont(),
                font.getSize(),
                font.getStyle(),
                fg
        );

        PdfPCell cell = new PdfPCell(
                new Phrase(text, cellFont)
        );

        cell.setBackgroundColor(bg);
        cell.setPadding(7);
        cell.setHorizontalAlignment(align);
        cell.setBorderColor(new Color(210, 225, 222));

        table.addCell(cell);
    }
}