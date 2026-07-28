package com.petclinic.admin.servlet.manager.inventory;

import com.petclinic.backend.model.Staff;
import com.petclinic.backend.dto.StockTransaction;
import com.petclinic.backend.service.StockService;
import com.petclinic.backend.util.StaffAuthUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * Inventory &gt; Lịch sử giao dịch kho (transaction history screen).
 * Raw chronological log of every stock-in/stock-out movement, filterable
 * by date range and item type. For the aggregated per-item view (with CSV
 * export), see StockMovementReportServlet instead - the two are kept
 * separate because "browse recent movements" and "export a summary report"
 * are different jobs even though they read the same underlying table.
 */
@WebServlet("/manager/inventory/transactions")
public class StockTransactionServlet extends HttpServlet {

    private final StockService stockService = new StockService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Staff manager = StaffAuthUtil.requireManager(req, resp);
        if (manager == null) return;

        try {
            LocalDate fromDate = parseDate(req.getParameter("fromDate"));
            LocalDate toDate = parseDate(req.getParameter("toDate"));
            String itemType = req.getParameter("itemType");

            List<StockTransaction> transactions =
                    stockService.getRecentTransactions(fromDate, toDate, itemType);

            req.setAttribute("transactions", transactions);
            req.setAttribute("fromDate", req.getParameter("fromDate"));
            req.setAttribute("toDate", req.getParameter("toDate"));
            req.setAttribute("itemType", itemType);

            req.getRequestDispatcher("/WEB-INF/views/manager/inventory/transactions.jsp").forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Không tải được lịch sử giao dịch kho: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/manager/inventory/transactions.jsp").forward(req, resp);
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        return LocalDate.parse(value);
    }
}
