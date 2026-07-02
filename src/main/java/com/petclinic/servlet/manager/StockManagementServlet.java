package com.petclinic.servlet.manager;

import com.petclinic.model.Staff;
import com.petclinic.model.StockMovementReport;
import com.petclinic.service.StockService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@WebServlet("/manager/stock")
public class StockManagementServlet extends HttpServlet {

    private final StockService stockService = new StockService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Staff manager = getAuthenticatedManager(req, resp);
        if (manager == null) return;

        if ("export".equals(req.getParameter("action"))) {
            exportMovementReport(req, resp);
            return;
        }

        loadDashboard(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");

        Staff manager = getAuthenticatedManager(req, resp);
        if (manager == null) return;

        try {
            String action = req.getParameter("action");
            if ("stockIn".equals(action)) {
                handleStockIn(req, manager);
                req.getSession().setAttribute("flashSuccess", "Đã lưu giao dịch nhập kho.");
            } else if ("threshold".equals(action)) {
                handleThreshold(req);
                req.getSession().setAttribute("flashSuccess", "Đã cập nhật ngưỡng cảnh báo.");
            } else {
                req.getSession().setAttribute("flashWarning", "Thao tác kho không hợp lệ.");
            }
        } catch (IllegalArgumentException e) {
            req.getSession().setAttribute("flashError", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            req.getSession().setAttribute("flashError", "Lỗi hệ thống khi xử lý kho: " + e.getMessage());
        }

        resp.sendRedirect(req.getContextPath() + "/manager/stock");
    }

    private void loadDashboard(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            String keyword = req.getParameter("q");
            String itemType = req.getParameter("itemType");
            String stockLevel = req.getParameter("stockLevel");
            LocalDate fromDate = parseDate(req.getParameter("fromDate"));
            LocalDate toDate = parseDate(req.getParameter("toDate"));

            req.setAttribute("inventory",
                    stockService.searchInventory(keyword, itemType, stockLevel));
            req.setAttribute("allItems", stockService.getAllInventoryItems());
            req.setAttribute("lowStock", stockService.getLowStockAlerts());
            req.setAttribute("movementReport", stockService.getMovementReport(fromDate, toDate));
            req.setAttribute("transactions", stockService.getRecentTransactions(fromDate, toDate));

            req.setAttribute("keyword", keyword);
            req.setAttribute("itemType", itemType);
            req.setAttribute("stockLevel", stockLevel);
            req.setAttribute("fromDate", req.getParameter("fromDate"));
            req.setAttribute("toDate", req.getParameter("toDate"));

            req.getRequestDispatcher("/WEB-INF/views/manager/stock.jsp").forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Không tải được dữ liệu quản lý kho: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/manager/stock.jsp").forward(req, resp);
        }
    }

    private void handleStockIn(HttpServletRequest req, Staff manager) throws Exception {
        String itemType = req.getParameter("itemType");
        Integer itemID = null;

        String stockItemKey = req.getParameter("stockItemKey");
        if (stockItemKey != null && !stockItemKey.isBlank()) {
            String[] parts = stockItemKey.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Item tồn kho không hợp lệ.");
            }
            itemType = parts[0];
            itemID = parseNullableInt(parts[1]);
        }

        String itemName = req.getParameter("itemName");
        String unit = req.getParameter("unit");
        BigDecimal unitPrice = parseNullableDecimal(req.getParameter("unitPrice"));
        int quantity = parseRequiredInt(req.getParameter("quantity"), "Số lượng nhập kho không hợp lệ.");
        Integer minStockLevel = parseNullableInt(req.getParameter("minStockLevel"));

        stockService.recordStockIn(itemType, itemID, itemName, unit, unitPrice,
                quantity, minStockLevel, manager.getStaffID());
    }

    private void handleThreshold(HttpServletRequest req) throws Exception {
        int medicineID = parseRequiredInt(req.getParameter("medicineID"), "Thuốc không hợp lệ.");
        int minStockLevel = parseRequiredInt(req.getParameter("minStockLevel"),
                "Ngưỡng cảnh báo không hợp lệ.");
        stockService.updateThreshold(medicineID, minStockLevel);
    }

    private void exportMovementReport(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        try {
            LocalDate fromDate = parseDate(req.getParameter("fromDate"));
            LocalDate toDate = parseDate(req.getParameter("toDate"));
            List<StockMovementReport> rows = stockService.getMovementReport(fromDate, toDate);
            resp.setContentType("text/csv;charset=UTF-8");
            resp.setHeader("Content-Disposition", "attachment; filename=\"stock-movement-report.csv\"");

            try (PrintWriter out = resp.getWriter()) {
                out.write("\uFEFF");
                out.println("Item Type,Item Name,Stock In,Stock Out,Net Change");
                for (StockMovementReport row : rows) {
                    out.println(csv(row.getItemType()) + ","
                            + csv(row.getItemName()) + ","
                            + row.getTotalStockIn() + ","
                            + row.getTotalStockOut() + ","
                            + row.getNetChange());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Không xuất được báo cáo kho.");
        }
    }

    private Staff getAuthenticatedManager(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession session = req.getSession(false);
        Staff staff = (session != null) ? (Staff) session.getAttribute("staff") : null;
        if (staff == null || !isManagerRole(staff.getRoleName())) {
            resp.sendRedirect(req.getContextPath() + "/auth/staff/login");
            return null;
        }
        return staff;
    }

    private boolean isManagerRole(String roleName) {
        return "Admin".equals(roleName) || "Manager".equals(roleName);
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        return LocalDate.parse(value);
    }

    private Integer parseNullableInt(String value) {
        if (value == null || value.isBlank()) return null;
        return Integer.parseInt(value);
    }

    private int parseRequiredInt(String value, String message) {
        try {
            if (value == null || value.isBlank()) throw new NumberFormatException();
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(message);
        }
    }

    private BigDecimal parseNullableDecimal(String value) {
        if (value == null || value.isBlank()) return null;
        return new BigDecimal(value);
    }

    private String csv(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
