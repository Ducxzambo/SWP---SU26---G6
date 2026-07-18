package com.petclinic.servlet.home;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.petclinic.dao.NotificationDAO;
import com.petclinic.dao.ServiceDAO;
import com.petclinic.model.Customer;
import com.petclinic.model.Notification;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;

/**
 * URL map:
 *   GET  /notifications            → trang full notification center
 *   GET  /notifications/api        → JSON list (dùng bởi header bell dropdown)
 *   GET  /notifications/count      → JSON { "unread": N }
 *   POST /notifications/mark-read  → đánh dấu đã đọc (1 hoặc tất cả)
 */
@WebServlet(urlPatterns = {
        "/notifications",
        "/notifications/api",
        "/notifications/count",
        "/notifications/mark-read"
})
public class NotificationServlet extends HttpServlet {

    private final NotificationDAO dao        = new NotificationDAO();
    private final ServiceDAO      serviceDAO = new ServiceDAO();

    // ── GET ───────────────────────────────────────────────────────────────────
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Customer customer = getCustomer(req, resp);
        if (customer == null) return;

        String path = req.getServletPath();

        // ── JSON: unread count (for SSE-style polling in header) ──────────────
        if ("/notifications/count".equals(path)) {
            try {
                int count = dao.countUnread(customer.getCustomerID());
                json(resp, unreadJson(count));
            } catch (Exception e) { json(resp, unreadJson(0)); }
            return;
        }

        // ── JSON: notification list (for header dropdown) ─────────────────────
        if ("/notifications/api".equals(path)) {
            int limit = 12;
            try { limit = Integer.parseInt(req.getParameter("limit")); } catch (Exception ignored) {}
            try {
                List<Notification> list = dao.findByCustomer(customer.getCustomerID());
                json(resp, toJson(list, limit));
            } catch (Exception e) { json(resp, "[]"); }
            return;
        }

        // ── Full page ─────────────────────────────────────────────────────────
        String tab = req.getParameter("tab"); // null | REMINDER | PAYMENT | EXAM_RESULT | CARE_TIP | SUPPORT
        try {
            List<Notification> all = dao.findByCustomer(customer.getCustomerID());

            // Filter by tab type-group if requested
            List<Notification> filtered = all;
            if (tab != null && !tab.isBlank()) {
                filtered = new java.util.ArrayList<>();
                for (Notification n : all) {
                    if (matchesTab(n.getType(), tab)) filtered.add(n);
                }
                // Mark tab notifications as read automatically on view
                dao.markTypeRead(customer.getCustomerID(), tab);
            }

            // Count per tab group for badges
            int cntReminder = 0, cntPayment = 0, cntExam = 0, cntCare = 0, cntAll = 0;
            for (Notification n : all) {
                if (!n.isRead()) {
                    cntAll++;
                    if (matchesTab(n.getType(), "REMINDER")) cntReminder++;
                    if (matchesTab(n.getType(), "PAYMENT"))  cntPayment++;
                    if (matchesTab(n.getType(), "EXAM_RESULT")) cntExam++;
                    if (matchesTab(n.getType(), "CARE_TIP")) cntCare++;
                }
            }

            req.setAttribute("notifications",   filtered);
            req.setAttribute("allCount",         all.size());
            req.setAttribute("unreadCount",      cntAll);
            req.setAttribute("cntReminder",      cntReminder);
            req.setAttribute("cntPayment",       cntPayment);
            req.setAttribute("cntExam",          cntExam);
            req.setAttribute("cntCare",          cntCare);
            req.setAttribute("activeTab",        tab != null ? tab : "ALL");
            req.setAttribute("navCategories",    serviceDAO.findAllCategoriesWithServices());

            req.getRequestDispatcher("/WEB-INF/views/customer/notifications.jsp")
                    .forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException(e);
        }
    }

    // ── POST: mark-read ───────────────────────────────────────────────────────
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Customer customer = getCustomer(req, resp);
        if (customer == null) return;

        String idParam = req.getParameter("id");   // specific ID or null = mark all
        try {
            if (idParam != null && !idParam.isBlank()) {
                dao.markRead(Integer.parseInt(idParam));
            } else {
                dao.markAllRead(customer.getCustomerID());
            }
        } catch (Exception e) { /* best-effort */ }

        // Respond: if AJAX (Accept:json), return updated count; else redirect
        String accept = req.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                int count = dao.countUnread(customer.getCustomerID());
                json(resp, unreadJson(count));
            } catch (Exception e) { json(resp, unreadJson(0)); }
        } else {
            String ref = req.getHeader("Referer");
            resp.sendRedirect(ref != null ? ref : req.getContextPath() + "/notifications");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean matchesTab(String type, String tab) {
        if (type == null) return "ALL".equals(tab);
        switch (tab) {
            case "REMINDER":    return type.startsWith("REMINDER");
            case "PAYMENT":     return type.startsWith("PAYMENT") || type.equals("BOOKING_CONFIRMED") || type.equals("BOOKING_CANCELLED");
            case "EXAM_RESULT": return type.equals("EXAM_RESULT") || type.equals("VACCINE_DUE");
            case "CARE_TIP":    return type.equals("CARE_TIP") || type.equals("SUPPORT");
            default:            return true;
        }
    }

    private Customer getCustomer(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession sess = req.getSession(false);
        Customer c = sess != null ? (Customer) sess.getAttribute("customer") : null;
        if (c == null) {
            // API endpoints: return 401 JSON
            String path = req.getServletPath();
            if (path.contains("/api") || path.contains("/count")) {
                json(resp, errorJson("unauthorized"));
            } else {
                resp.sendRedirect(req.getContextPath() + "/auth/login");
            }
        }
        return c;
    }

    private void json(HttpServletResponse resp, String body) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(body);
    }

    private String unreadJson(int count) {
        JsonObject o = new JsonObject();
        o.addProperty("unread", count);
        return o.toString();
    }

    private String errorJson(String message) {
        JsonObject o = new JsonObject();
        o.addProperty("error", message);
        return o.toString();
    }

    private String toJson(List<Notification> list, int limit) {
        JsonArray arr = new JsonArray();
        int count = 0;
        for (Notification n : list) {
            if (count >= limit) break;
            JsonObject o = new JsonObject();
            o.addProperty("id", n.getNotificationID());
            o.addProperty("title", n.getTitle());
            o.addProperty("body", n.getBody());
            o.addProperty("type", n.getType());
            o.addProperty("actionUrl", n.getActionUrl());
            o.addProperty("isRead", n.isRead());
            o.addProperty("relativeTime", n.getRelativeTime());
            o.addProperty("typeColor", n.getTypeColor());
            arr.add(o);
            count++;
        }
        return arr.toString();
    }
}
