package com.petclinic.servlet.inpatient;

import com.petclinic.model.Staff;
import com.petclinic.service.InpatientService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Cage Management — Receptionist only.
 *
 * GET  /inpatient/cages          → view all cages + status
 * POST /inpatient/cages?action=add       → add new cage
 * POST /inpatient/cages?action=toggle    → toggle maintenance mode
 *
 * Mapped in web.xml only (no @WebServlet annotation)
 */
public class CageServlet extends HttpServlet {

    private final InpatientService svc = new InpatientService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Staff staff = requireRole(req, resp, "Receptionist");
        if (staff == null) return;

        try {
            req.setAttribute("cages", svc.getAllCages());
            forward(req, resp,
                    "/WEB-INF/views/receptionist/cage-list.jsp");
        } catch (Exception e) {
            req.setAttribute("error",
                    "Cannot load cages: " + e.getMessage());
            forward(req, resp,
                    "/WEB-INF/views/receptionist/cage-list.jsp");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Staff staff = requireRole(req, resp, "Receptionist");
        if (staff == null) return;

        req.setCharacterEncoding("UTF-8");
        String action = req.getParameter("action");

        try {
            if ("add".equals(action)) {
                handleAdd(req);
            } else if ("toggle".equals(action)) {
                handleToggle(req);
            }
            resp.sendRedirect(req.getContextPath()
                    + "/inpatient/cages?success=true");

        } catch (IllegalStateException | IllegalArgumentException ex) {
            req.setAttribute("error", ex.getMessage());
            doGet(req, resp);
        } catch (Exception ex) {
            req.setAttribute("error", "Operation failed: " + ex.getMessage());
            doGet(req, resp);
        }
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    private void handleAdd(HttpServletRequest req) throws Exception {
        String cageNumber = req.getParameter("cageNumber");
        String cageType   = req.getParameter("cageType");
        String notes      = req.getParameter("notes");

        if (blank(cageNumber))
            throw new IllegalArgumentException("Cage number is required.");

        svc.addCage(cageNumber.trim().toUpperCase(), cageType, notes);
    }

    private void handleToggle(HttpServletRequest req) throws Exception {
        String cageIdStr  = req.getParameter("cageID");
        String activeStr  = req.getParameter("isActive");

        if (blank(cageIdStr))
            throw new IllegalArgumentException("Cage ID is required.");

        svc.toggleCageMaintenance(
                Integer.parseInt(cageIdStr.trim()),
                "1".equals(activeStr) || "true".equals(activeStr)
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Staff requireRole(HttpServletRequest req, HttpServletResponse resp,
                              String role) throws IOException {
        HttpSession s = req.getSession(false);
        if (s != null) {
            Object o = s.getAttribute("staff");
            if (o instanceof Staff st) {
                if (role.equalsIgnoreCase(st.getRoleName())) return st;
                resp.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Required role: " + role
                                + " | Tip: add ?devRole=receptionist");
                return null;
            }
        }
        resp.sendRedirect(req.getContextPath() + "/auth/login");
        return null;
    }

    private void forward(HttpServletRequest req, HttpServletResponse resp,
                         String path) throws ServletException, IOException {
        req.getRequestDispatcher(path).forward(req, resp);
    }

    private boolean blank(String s) { return s == null || s.isBlank(); }
}