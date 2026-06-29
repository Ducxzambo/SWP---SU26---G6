package com.petclinic.servlet.home;

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
                json(resp, "{\"unread\":" + count + "}");
            } catch (Exception e) { json(resp, "{\"unread\":0}"); }
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
                json(resp, "{\"unread\":" + count + "}");
            } catch (Exception e) { json(resp, "{\"unread\":0}"); }
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
                json(resp, "{\"error\":\"unauthorized\"}");
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

    /** Serialize notification list to JSON (manual, no Gson dependency). */
    private String toJson(List<Notification> list, int limit) {
        StringBuilder sb = new StringBuilder("[");
        int count = 0;
        for (Notification n : list) {
            if (count >= limit) break;
            if (count > 0) sb.append(",");
            sb.append("{")
                    .append("\"id\":").append(n.getNotificationID()).append(",")
                    .append("\"title\":").append(jsonStr(n.getTitle())).append(",")
                    .append("\"body\":").append(jsonStr(n.getBody())).append(",")
                    .append("\"type\":").append(jsonStr(n.getType())).append(",")
                    .append("\"actionUrl\":").append(jsonStr(n.getActionUrl())).append(",")
                    .append("\"isRead\":").append(n.isRead()).append(",")
                    .append("\"relativeTime\":").append(jsonStr(n.getRelativeTime())).append(",")
                    .append("\"typeColor\":").append(jsonStr(n.getTypeColor()))
                    .append("}");
            count++;
        }
        return sb.append("]").toString();
    }

    private String jsonStr(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\","\\\\").replace("\"","\\\"")
                .replace("\n","\\n").replace("\r","") + "\"";
    }
}
