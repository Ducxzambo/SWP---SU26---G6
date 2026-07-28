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
import java.util.List;

/**
 * Inventory &gt; Danh sách tồn kho (list/search screen).
 * Read-only browse of all Medicine + Vaccine stock, with keyword/type/level
 * filters and quick health metrics. This is the hub screen managers land on
 * for the Inventory module; the other Inventory screens (Stock In,
 * Thresholds, Transactions, Report) are reached through the sub-nav.
 */
@WebServlet("/manager/inventory")
public class InventoryListServlet extends HttpServlet {

    private final StockService stockService = new StockService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Staff manager = StaffAuthUtil.requireManager(req, resp);
        if (manager == null) return;

        try {
            String keyword = req.getParameter("q");
            String itemType = req.getParameter("itemType");
            String stockLevel = req.getParameter("stockLevel");

            List<InventoryItem> inventory = stockService.searchInventory(keyword, itemType, stockLevel);
            List<InventoryItem> allItems = stockService.getAllInventoryItems();

            long lowStockCount = allItems.stream().filter(InventoryItem::isLowStock).count();
            long outOfStockCount = allItems.stream().filter(InventoryItem::isOutOfStock).count();

            req.setAttribute("inventory", inventory);
            req.setAttribute("totalItems", allItems.size());
            req.setAttribute("lowStockCount", lowStockCount);
            req.setAttribute("outOfStockCount", outOfStockCount);

            req.setAttribute("keyword", keyword);
            req.setAttribute("itemType", itemType);
            req.setAttribute("stockLevel", stockLevel);

            req.getRequestDispatcher("/WEB-INF/views/manager/inventory/list.jsp").forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Không tải được danh sách tồn kho: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/manager/inventory/list.jsp").forward(req, resp);
        }
    }
}
