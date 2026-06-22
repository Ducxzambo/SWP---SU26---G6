package com.petclinic.servlet.inpatient;

import com.petclinic.model.InpatientAdmission;
import com.petclinic.model.Staff;
import com.petclinic.service.InpatientService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

/**
 * BP-04 Final step — Receptionist discharges a pet.
 *
 * GET  /inpatient/discharge?admissionId=X
 *      → show discharge confirmation page with summary
 *
 * POST /inpatient/discharge
 *      → discharge pet, generate invoice
 *      → redirect to /payment?invoiceId={id}  (BP-05)
 */
//@WebServlet("/inpatient/discharge")
public class DischargeServlet extends HttpServlet {

    private final InpatientService svc = new InpatientService();

    // ── GET ───────────────────────────────────────────────────────────────────
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Staff staff = requireRole(req, resp, "Receptionist");
        if (staff == null) return;

        String idStr = req.getParameter("admissionId");
        if (blank(idStr)) {
            resp.sendRedirect(req.getContextPath() + "/inpatient/list");
            return;
        }

        try {
            int admissionId = Integer.parseInt(idStr.trim());
            InpatientAdmission a = svc.getAdmission(admissionId);

            if (a == null || "Discharged".equals(a.getStatus())) {
                resp.sendRedirect(req.getContextPath() + "/inpatient/list");
                return;
            }

            req.setAttribute("admission",   a);
            req.setAttribute("assessments", svc.getAssessments(admissionId));
            forward(req, resp, "/WEB-INF/views/receptionist/inpatient-discharge.jsp");

        } catch (Exception e) {
            req.setAttribute("error", "Error loading discharge page: " + e.getMessage());
            forward(req, resp, "/WEB-INF/views/receptionist/inpatient-discharge.jsp");
        }
    }

    // ── POST ──────────────────────────────────────────────────────────────────
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Staff staff = requireRole(req, resp, "Receptionist");
        if (staff == null) return;

        req.setCharacterEncoding("UTF-8");
        String idStr = req.getParameter("admissionId");

        if (blank(idStr)) {
            resp.sendRedirect(req.getContextPath() + "/inpatient/list");
            return;
        }

        try {
            int admissionId = Integer.parseInt(idStr.trim());
            int invoiceId   = svc.dischargePet(admissionId);

            // hand off to BP-05 Payment
            resp.sendRedirect(req.getContextPath()
                + "/payment?invoiceId=" + invoiceId + "&source=inpatient");

        } catch (Exception e) {
            req.setAttribute("error", "Discharge failed: " + e.getMessage());
            doGet(req, resp);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Staff requireRole(HttpServletRequest req, HttpServletResponse resp,
                               String role) throws IOException {
        HttpSession s = req.getSession(false);
        if (s != null) {
            Object o = s.getAttribute("staff");
            if (o instanceof Staff st && role.equalsIgnoreCase(st.getRoleName()))
                return st;
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
