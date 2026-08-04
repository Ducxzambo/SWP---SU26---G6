package com.petclinic.admin.servlet.manager.inventory;

import com.petclinic.backend.dto.StockMovementReport;
import com.petclinic.backend.model.Staff;
import com.petclinic.backend.service.StockService;
import com.petclinic.backend.util.StaffAuthUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.List;

/**
 * Inventory &gt; Báo cáo biến động kho (movement report screen).
 * Aggregated stock-in / stock-out / net totals per item over a date range,
 * with a CSV export action. See StockTransactionServlet for the raw,
 * per-transaction log this report is built from.
 */
@WebServlet("/admin/inventory/report")
public class StockMovementReportServlet extends HttpServlet {

    private final StockService stockService = new StockService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Staff manager = StaffAuthUtil.requireManager(req, resp);
        if (manager == null) return;

        if ("export".equals(req.getParameter("action"))) {
            exportMovementReport(req, resp);
            return;
        }

        try {
            LocalDate fromDate = parseDate(req.getParameter("fromDate"));
            LocalDate toDate = parseDate(req.getParameter("toDate"));

            List<StockMovementReport> movementReport = stockService.getMovementReport(fromDate, toDate);

            req.setAttribute("movementReport", movementReport);
            req.setAttribute("fromDate", req.getParameter("fromDate"));
            req.setAttribute("toDate", req.getParameter("toDate"));

            req.getRequestDispatcher("/WEB-INF/views/manager/inventory/report.jsp").forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Không tải được báo cáo biến động kho: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/manager/inventory/report.jsp").forward(req, resp);
        }
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

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        return LocalDate.parse(value);
    }

    private String csv(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
