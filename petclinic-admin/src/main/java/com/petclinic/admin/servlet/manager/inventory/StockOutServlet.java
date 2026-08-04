package com.petclinic.admin.servlet.manager.inventory;

import com.petclinic.backend.model.InventoryItem;
import com.petclinic.backend.model.Staff;
import com.petclinic.backend.service.StockService;
import com.petclinic.backend.util.StaffAuthUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/admin/inventory/stock-out")
public class StockOutServlet extends HttpServlet {
    private final StockService stockService = new StockService();
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (StaffAuthUtil.requireManager(req, resp) == null) return;
        try { List<InventoryItem> items = stockService.getAllInventoryItems(); req.setAttribute("allItems", items); req.getRequestDispatcher("/WEB-INF/views/manager/inventory/stock-out.jsp").forward(req, resp); }
        catch (Exception e) { req.setAttribute("error", e.getMessage()); req.getRequestDispatcher("/WEB-INF/views/manager/inventory/stock-out.jsp").forward(req, resp); }
    }
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8"); Staff manager = StaffAuthUtil.requireManager(req, resp); if (manager == null) return;
        try { String[] key=req.getParameter("stockItemKey").split(":"); if(key.length!=2) throw new IllegalArgumentException("Invalid inventory item."); int qty=Integer.parseInt(req.getParameter("quantity")); stockService.recordStockOut(key[0],Integer.parseInt(key[1]),qty,req.getParameter("reason"),manager.getStaffID()); req.getSession().setAttribute("flashSuccess","Stock-out transaction saved."); }
        catch (Exception e) { req.getSession().setAttribute("flashError","Could not stock out: "+e.getMessage()); }
        resp.sendRedirect(req.getContextPath()+"/admin/inventory/stock-out");
    }
}
