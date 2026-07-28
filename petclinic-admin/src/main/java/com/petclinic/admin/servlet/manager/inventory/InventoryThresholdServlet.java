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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Inventory &gt; Ngưỡng cảnh báo &amp; tồn thấp (thresholds screen).
 * Combines two closely related concerns into one screen: which items are
 * currently low/out of stock (read), and what their alert threshold should
 * be (edit) - for both Medicine and Vaccine. Sorting already puts low/out
 * of stock items first (see MedicineDAO.searchInventory), so the "alert"
 * view falls out naturally from the same list instead of needing a
 * separate page.
 */
@WebServlet("/manager/inventory/thresholds")
public class InventoryThresholdServlet extends HttpServlet {

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

            req.setAttribute("inventory", inventory);
            req.setAttribute("keyword", keyword);
            req.setAttribute("itemType", itemType);
            req.setAttribute("stockLevel", stockLevel);

            req.getRequestDispatcher("/WEB-INF/views/manager/inventory/thresholds.jsp").forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Không tải được danh sách ngưỡng cảnh báo: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/manager/inventory/thresholds.jsp").forward(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");

        Staff manager = StaffAuthUtil.requireManager(req, resp);
        if (manager == null) return;

        try {
            String itemType = req.getParameter("itemType");
            int itemID = parseRequiredInt(req.getParameter("itemID"), "Item không hợp lệ.");
            int minStockLevel = parseRequiredInt(req.getParameter("minStockLevel"),
                    "Ngưỡng cảnh báo không hợp lệ.");
            stockService.updateThreshold(itemType, itemID, minStockLevel);
            req.getSession().setAttribute("flashSuccess", "Đã cập nhật ngưỡng cảnh báo.");
        } catch (IllegalArgumentException e) {
            req.getSession().setAttribute("flashError", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            req.getSession().setAttribute("flashError", "Lỗi hệ thống khi cập nhật ngưỡng: " + e.getMessage());
        }

        resp.sendRedirect(req.getContextPath() + "/manager/inventory/thresholds" + buildFilterQuery(req));
    }

    /**
     * Preserves the list's current q/itemType/stockLevel filters across the
     * inline-edit POST, using hidden fields the JSP echoes back per row
     * (see thresholds.jsp). "itemType" is reserved on this request for the
     * *edited row's* item type (Medicine/Vaccine), so the filter value is
     * carried separately as "filterItemType" to avoid colliding with it.
     */
    private String buildFilterQuery(HttpServletRequest req) throws UnsupportedEncodingException {
        StringBuilder qs = new StringBuilder();
        appendParam(qs, "q", req.getParameter("q"));
        appendParam(qs, "itemType", req.getParameter("filterItemType"));
        appendParam(qs, "stockLevel", req.getParameter("stockLevel"));
        return qs.length() == 0 ? "" : "?" + qs;
    }

    private void appendParam(StringBuilder qs, String name, String value)
            throws UnsupportedEncodingException {
        if (value == null || value.isBlank()) return;
        if (qs.length() > 0) qs.append("&");
        qs.append(name).append("=").append(URLEncoder.encode(value, StandardCharsets.UTF_8.name()));
    }

    private int parseRequiredInt(String value, String message) {
        try {
            if (value == null || value.isBlank()) throw new NumberFormatException();
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(message);
        }
    }
}
