package com.petclinic.admin.servlet.manager.inventory;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Legacy URL kept as a compatibility redirect after the Inventory screen
 * was split into /manager/inventory/* (see the com.petclinic.servlet.manager.inventory
 * package). Anything that still links to /manager/stock lands on the new
 * Inventory List screen instead of 404-ing.
 */
@WebServlet("/admin/stock")
public class StockManagementServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.sendRedirect(req.getContextPath() + "/admin/inventory");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.sendRedirect(req.getContextPath() + "/admin/inventory");
    }
}
