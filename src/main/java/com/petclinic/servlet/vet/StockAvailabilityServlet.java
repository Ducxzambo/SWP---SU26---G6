package com.petclinic.servlet.vet;

import com.petclinic.model.Staff;
import com.petclinic.service.StockService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/vet/stock")
public class StockAvailabilityServlet extends HttpServlet {

    private final StockService stockService = new StockService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Staff vet = getAuthenticatedVet(req, resp);
        if (vet == null) return;

        try {
            String keyword = req.getParameter("q");
            String itemType = req.getParameter("itemType");
            String stockLevel = req.getParameter("stockLevel");

            req.setAttribute("inventory",
                    stockService.searchInventory(keyword, itemType, stockLevel));
            req.setAttribute("keyword", keyword);
            req.setAttribute("itemType", itemType);
            req.setAttribute("stockLevel", stockLevel);

            req.getRequestDispatcher("/WEB-INF/views/vet/stock-readonly.jsp").forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Không tải được tồn kho.");
            req.getRequestDispatcher("/WEB-INF/views/vet/stock-readonly.jsp").forward(req, resp);
        }
    }

    private Staff getAuthenticatedVet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession session = req.getSession(false);
        Staff staff = (session != null) ? (Staff) session.getAttribute("staff") : null;
        if (staff == null || !"Veterinarian".equals(staff.getRoleName())) {
            resp.sendRedirect(req.getContextPath() + "/auth/staff/login");
            return null;
        }
        return staff;
    }
}
