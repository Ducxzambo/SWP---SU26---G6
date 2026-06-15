package com.petclinic.servlet.home;

import com.petclinic.dao.NotificationDAO;
import com.petclinic.model.Customer;
import com.petclinic.model.Notification;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;

@WebServlet(urlPatterns = {"/notifications/api", "/notifications/mark-read", "/notifications"})
public class NotificationServlet extends HttpServlet {

    private final NotificationDAO dao = new NotificationDAO();
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Customer customer = getCustomer(req, resp);
        if (customer == null) return;

        String path = req.getServletPath();

        if ("/notifications/api".equals(path)) {
            // ── JSON API for inline fetch ──────────────────────────────────
            int limit = 10;
            try { limit = Integer.parseInt(req.getParameter("limit")); } catch (Exception ignored) {}

            List<Notification> list;
            try { list = dao.findByCustomer(customer.getCustomerID()); }
            catch (Exception e) { resp.sendError(500); return; }

            resp.setContentType("application/json;charset=UTF-8");
            PrintWriter out = resp.getWriter();
            out.print("[");
            int count = 0;
            for (Notification n : list) {
                if (count >= limit) break;
                if (count > 0) out.print(",");
                out.print("{");
                out.print("\"id\":"       + n.getNotificationID()                + ",");
                out.print("\"title\":"    + jsonStr(n.getTitle())                + ",");
                out.print("\"body\":"     + jsonStr(n.getBody())                 + ",");
                out.print("\"read\":"     + n.isRead()                           + ",");
                out.print("\"createdAt\":" + jsonStr(
                        n.getCreatedAt() != null ? n.getCreatedAt().format(ISO) : "") + "}");
                count++;
            }
            out.print("]");
            return;
        }

        // ── Full notifications page ────────────────────────────────────────
        try {
            List<Notification> list = dao.findByCustomer(customer.getCustomerID());
            req.setAttribute("notifications", list);
            req.setAttribute("navCategories",
                    new com.petclinic.dao.ServiceDAO().findAllCategoriesWithServices());
            req.setAttribute("unreadCount", dao.countUnread(customer.getCustomerID()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        req.getRequestDispatcher("/WEB-INF/views/customer/notifications.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Customer customer = getCustomer(req, resp);
        if (customer == null) return;

        try { dao.markAllRead(customer.getCustomerID()); } catch (Exception ignored) {}

        String path = req.getServletPath();
        if ("/notifications/mark-read".equals(path)) {
            // Called by fetch (AJAX) — return 204
            resp.setStatus(204);
        } else {
            resp.sendRedirect(req.getContextPath() + "/notifications");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Customer getCustomer(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession session = req.getSession(false);
        Customer customer   = session != null ? (Customer) session.getAttribute("customer") : null;
        if (customer == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return null;
        }
        return customer;
    }

    private String jsonStr(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n", "\\n")
                       .replace("\r", "\\r") + "\"";
    }
}
