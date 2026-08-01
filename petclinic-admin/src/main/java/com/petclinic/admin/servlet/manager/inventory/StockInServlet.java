package com.petclinic.admin.servlet.manager.inventory;

import com.petclinic.backend.model.InventoryItem;
import com.petclinic.backend.model.Staff;
import com.petclinic.backend.service.StockService;
import com.petclinic.backend.util.StaffAuthUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@WebServlet("/manager/inventory/stock-in")
public class StockInServlet extends HttpServlet {

    private final StockService stockService = new StockService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Staff manager = StaffAuthUtil.requireManager(req, resp);
        if (manager == null) return;

        try {
            List<InventoryItem> allItems = stockService.getAllInventoryItems();
            req.setAttribute("allItems", allItems);
            req.getRequestDispatcher("/WEB-INF/views/manager/inventory/stock-in.jsp").forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Không tải được danh sách item: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/manager/inventory/stock-in.jsp").forward(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");

        Staff manager = StaffAuthUtil.requireManager(req, resp);
        if (manager == null) return;

        try {
            handleStockIn(req, manager);
            req.getSession().setAttribute("flashSuccess", "Đã lưu giao dịch nhập kho.");
        } catch (IllegalArgumentException e) {
            req.getSession().setAttribute("flashError", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            req.getSession().setAttribute("flashError", "Lỗi hệ thống khi nhập kho: " + e.getMessage());
        }

        resp.sendRedirect(req.getContextPath() + "/manager/inventory/stock-in");
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
}
