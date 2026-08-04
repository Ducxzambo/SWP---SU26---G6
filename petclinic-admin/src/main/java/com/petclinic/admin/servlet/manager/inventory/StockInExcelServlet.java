package com.petclinic.admin.servlet.manager.inventory;

import com.petclinic.backend.dao.StockImportReceiptDAO;
import com.petclinic.backend.model.InventoryItem;
import com.petclinic.backend.model.Provider;
import com.petclinic.backend.model.Staff;
import com.petclinic.backend.service.ProviderService;
import com.petclinic.backend.service.StockService;
import com.petclinic.backend.util.StaffAuthUtil;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@WebServlet({
        "/admin/inventory/stock-in/template",
        "/admin/inventory/stock-in/import"
})
@MultipartConfig(maxFileSize = 5 * 1024 * 1024)
public class StockInExcelServlet extends HttpServlet {

    private final StockService stockService = new StockService();
    private final ProviderService providerService = new ProviderService();
    private final StockImportReceiptDAO receiptDAO = new StockImportReceiptDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        if (StaffAuthUtil.requireManager(req, resp) == null) {
            return;
        }

        List<Provider> providers;
        List<InventoryItem> items;

        try {
            providers = providerService.getActive();
            items = stockService.getAllInventoryItems();
        } catch (Exception e) {
            resp.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Không thể tải danh mục cho file mẫu."
            );
            return;
        }

        resp.setContentType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        );
        resp.setHeader(
                "Content-Disposition",
                "attachment; filename=stock-in-template.xlsx"
        );

        try (
                Workbook workbook = new XSSFWorkbook();
                OutputStream out = resp.getOutputStream()
        ) {

            Sheet sheet = workbook.createSheet("Nhap kho");

            String[] headers = {
                    "Nhà cung cấp",
                    "Loại item",
                    "Tên item",
                    "Đơn vị",
                    "Đơn giá",
                    "Số lượng",
                    "Ngưỡng cảnh báo"
            };

            CellStyle header = createHeaderStyle(workbook);
            CellStyle data = createDataStyle(workbook);

            Row headerRow = sheet.createRow(0);

            for (int col = 0; col < headers.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(headers[col]);
                cell.setCellStyle(header);

                sheet.setColumnWidth(
                        col,
                        col == 2 ? 9000 : 4800
                );
            }

            for (int rowIndex = 1; rowIndex <= 100; rowIndex++) {
                Row row = sheet.createRow(rowIndex);

                for (int col = 0; col < headers.length; col++) {
                    row.createCell(col).setCellStyle(data);
                }
            }

            sheet.createFreezePane(0, 1);

            addLookupLists(
                    workbook,
                    sheet,
                    providers,
                    items
            );

            workbook.write(out);
        }
    }

    private void addLookupLists(
            Workbook workbook,
            Sheet inputSheet,
            List<Provider> providers,
            List<InventoryItem> items
    ) {

        Sheet lookup = workbook.createSheet("DanhMuc");

        lookup.createRow(0).createCell(0).setCellValue("Nhà cung cấp");
        lookup.getRow(0).createCell(1).setCellValue("Loại item");
        lookup.getRow(0).createCell(2).setCellValue("Tên item có sẵn");

        for (int i = 0; i < providers.size(); i++) {
            row(lookup, i + 1)
                    .createCell(0)
                    .setCellValue(providers.get(i).getName());
        }

        row(lookup, 1).createCell(1).setCellValue("Medicine");
        row(lookup, 2).createCell(1).setCellValue("Vaccine");

        for (int i = 0; i < items.size(); i++) {
            row(lookup, i + 1)
                    .createCell(2)
                    .setCellValue(items.get(i).getDisplayName());
        }

        createNamedRange(workbook, "ProviderNames", providers.size(), "A");
        createNamedRange(workbook, "ItemTypes", 2, "B");
        createNamedRange(workbook, "ExistingItems", items.size(), "C");

        addDropdown(inputSheet, 0, "ProviderNames");
        addDropdown(inputSheet, 1, "ItemTypes");
        addDropdown(inputSheet, 2, "ExistingItems");

        workbook.setSheetHidden(
                workbook.getSheetIndex(lookup),
                true
        );
    }

    private Row row(Sheet sheet, int index) {
        Row row = sheet.getRow(index);
        return row == null ? sheet.createRow(index) : row;
    }

    private void createNamedRange(
            Workbook workbook,
            String name,
            int size,
            String column
    ) {

        Name range = workbook.createName();
        range.setNameName(name);

        range.setRefersToFormula(
                "'DanhMuc'!$" + column + "$2:$" + column + "$" + Math.max(2, size + 1)
        );
    }

    private void addDropdown(
            Sheet sheet,
            int column,
            String rangeName
    ) {

        DataValidationHelper helper = sheet.getDataValidationHelper();

        DataValidationConstraint constraint =
                helper.createFormulaListConstraint(rangeName);

        CellRangeAddressList addresses =
                new CellRangeAddressList(1, 100, column, column);

        DataValidation validation =
                helper.createValidation(constraint, addresses);

        validation.setShowErrorBox(false);
        validation.setSuppressDropDownArrow(true);

        sheet.addValidationData(validation);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        Staff staff = StaffAuthUtil.requireManager(req, resp);

        if (staff == null) {
            return;
        }

        try {
            int receiptID = importWorkbook(
                    req.getPart("excelFile"),
                    staff
            );

            req.getSession().setAttribute(
                    "flashSuccess",
                    "Import thành công và đã tạo phiếu nhập."
            );

            resp.sendRedirect(
                    req.getContextPath()
                            + "/admin/inventory/receipts/detail?id="
                            + receiptID
            );

            return;

        } catch (Exception e) {

            req.getSession().setAttribute(
                    "flashError",
                    "Import thất bại: " + e.getMessage()
            );
        }

        resp.sendRedirect(
                req.getContextPath() + "/admin/inventory/stock-in"
        );
    }

    private int importWorkbook(Part file, Staff staff) throws Exception {

        if (file == null || file.getSize() == 0) {
            throw new IllegalArgumentException(
                    "Vui lòng chọn file Excel."
            );
        }

        List<ImportRow> rows = new ArrayList<>();
        Provider receiptProvider = null;

        try (
                Workbook workbook =
                        new XSSFWorkbook(file.getInputStream())
        ) {

            Sheet sheet = workbook.getSheetAt(0);

            for (int rowIndex = 1;
                 rowIndex <= sheet.getLastRowNum();
                 rowIndex++) {

                Row row = sheet.getRow(rowIndex);

                if (row == null || value(row, 0).isBlank()) {
                    continue;
                }

                Provider provider =
                        providerService.findActiveByName(
                                value(row, 0)
                        );

                if (provider == null) {
                    throw new IllegalArgumentException(
                            "Dòng " + (rowIndex + 1)
                                    + ": không tìm thấy nhà cung cấp."
                    );
                }

                if (receiptProvider == null) {
                    receiptProvider = provider;
                }

                if (receiptProvider.getProviderID()
                        != provider.getProviderID()) {

                    throw new IllegalArgumentException(
                            "Một file chỉ được chứa một nhà cung cấp."
                    );
                }

                String type =
                        normalizeItemType(value(row, 1));

                if (type == null) {
                    throw new IllegalArgumentException(
                            "Dòng " + (rowIndex + 1)
                                    + ": Loại Item không hợp lệ."
                    );
                }

                String name = value(row, 2);
                int quantity = Integer.parseInt(value(row, 5));

                BigDecimal price = decimal(row, 4);

                rows.add(
                        new ImportRow(
                                type,
                                findExistingItem(type, name),
                                name,
                                value(row, 3),
                                price == null
                                        ? BigDecimal.ZERO
                                        : price,
                                quantity,
                                integer(row, 6)
                        )
                );
            }
        }

        if (rows.isEmpty() || receiptProvider == null) {
            throw new IllegalArgumentException(
                    "File không có dòng dữ liệu."
            );
        }

        String code = "PN-"
                + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern(
                        "yyyyMMdd-HHmmss-SSS"
                )
        );

        int receiptID = receiptDAO.create(
                receiptProvider.getProviderID(),
                staff.getStaffID(),
                code
        );

        for (ImportRow row : rows) {

            int savedID = stockService.recordStockIn(
                    row.type,
                    row.itemID,
                    row.name,
                    row.unit,
                    row.price,
                    row.quantity,
                    row.threshold,
                    staff.getStaffID(),
                    receiptProvider.getProviderID()
            );

            receiptDAO.addDetail(
                    receiptID,
                    row.type,
                    savedID,
                    row.name,
                    row.quantity,
                    row.price
            );

            receiptDAO.linkLatestTransaction(
                    receiptID,
                    row.type,
                    savedID,
                    staff.getStaffID()
            );
        }

        receiptDAO.updateTotal(receiptID);

        return receiptID;
    }

    private Integer findExistingItem(
            String type,
            String name
    ) throws Exception {

        for (InventoryItem item : stockService.getAllInventoryItems()) {
            if (item.getItemType().equals(type)
                    && item.getDisplayName().equalsIgnoreCase(name)) {
                return item.getItemID();
            }
        }

        return null;
    }

    private String normalizeItemType(String type) {

        String value = type == null ? "" : type.trim();

        if (value.equalsIgnoreCase("Medicine")) {
            return "Medicine";
        }

        if (value.equalsIgnoreCase("Vaccine")) {
            return "Vaccine";
        }

        return null;
    }

    private String value(Row row, int col) {
        return new DataFormatter()
                .formatCellValue(
                        row.getCell(
                                col,
                                Row.MissingCellPolicy.CREATE_NULL_AS_BLANK
                        )
                )
                .trim();
    }

    private Integer integer(Row row, int col) {
        String value = value(row, col);
        return value.isBlank() ? null : Integer.valueOf(value);
    }

    private BigDecimal decimal(Row row, int col) {
        String value = value(row, col);
        return value.isBlank()
                ? null
                : new BigDecimal(value.replace(",", ""));
    }

    private CellStyle createHeaderStyle(Workbook workbook) {

        CellStyle style = workbook.createCellStyle();

        style.setFillForegroundColor(
                IndexedColors.TEAL.getIndex()
        );
        style.setFillPattern(
                FillPatternType.SOLID_FOREGROUND
        );

        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());

        style.setFont(font);

        setBorders(style);

        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {

        CellStyle style = workbook.createCellStyle();

        setBorders(style);

        return style;
    }

    private void setBorders(CellStyle style) {

        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private static class ImportRow {

        final String type;
        final String name;
        final String unit;
        final Integer itemID;
        final Integer threshold;
        final BigDecimal price;
        final int quantity;

        ImportRow(
                String type,
                Integer itemID,
                String name,
                String unit,
                BigDecimal price,
                int quantity,
                Integer threshold
        ) {
            this.type = type;
            this.itemID = itemID;
            this.name = name;
            this.unit = unit;
            this.price = price;
            this.quantity = quantity;
            this.threshold = threshold;
        }
    }
}