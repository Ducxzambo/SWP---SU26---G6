package com.petclinic.servlet.inpatient;

import com.petclinic.model.Staff;
import com.petclinic.service.InpatientService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

/**
 * BP-04 Daily loop — Vet submits daily assessment.
 *
 * GET  /inpatient/assessment?admissionId=X
 *      → show form + previous assessments
 *
 * POST /inpatient/assessment
 *      → save DailyAssessments row, notify customer
 *      → if markCritical=on → update Status + urgent notification
 *      → redirect back to same page with success param
 */
//@WebServlet("/inpatient/assessment")
public class DailyAssessmentServlet extends HttpServlet {

    private final InpatientService svc = new InpatientService();

    // ── GET ───────────────────────────────────────────────────────────────────
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Staff staff = requireVet(req, resp);
        if (staff == null) return;

        String idStr = req.getParameter("admissionId");
        if (blank(idStr)) {
            resp.sendRedirect(req.getContextPath() + "/inpatient/list");
            return;
        }

        try {
            int admissionId = Integer.parseInt(idStr.trim());
            req.setAttribute("admission",   svc.getAdmission(admissionId));
            req.setAttribute("assessments", svc.getAssessments(admissionId));
            req.setAttribute("todayDone",   svc.hasTodayAssessment(admissionId));
            req.setAttribute("staff",       staff);
            forward(req, resp, "/WEB-INF/views/vet/inpatient-assessment.jsp");
        } catch (Exception e) {
            req.setAttribute("error", "Error loading page: " + e.getMessage());
            forward(req, resp, "/WEB-INF/views/vet/inpatient-assessment.jsp");
        }
    }

    // ── POST ──────────────────────────────────────────────────────────────────
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Staff staff = requireVet(req, resp);
        if (staff == null) return;

        req.setCharacterEncoding("UTF-8");

        String idStr       = req.getParameter("admissionId");
        String condition   = req.getParameter("condition");
        String treatment   = req.getParameter("treatmentToday");
        boolean critical   = "on".equals(req.getParameter("markCritical"));

        if (blank(idStr) || blank(condition)) {
            req.setAttribute("error", "Condition is required.");
            doGet(req, resp);
            return;
        }

        try {
            int admissionId = Integer.parseInt(idStr.trim());

            // save assessment (throws if today already submitted)
            svc.saveDailyAssessment(admissionId, staff.getStaffID(),
                condition, treatment);

            // optional critical flag
            if (critical) {
                svc.markCritical(admissionId);
            }

            resp.sendRedirect(req.getContextPath()
                + "/inpatient/assessment?admissionId=" + admissionId
                + "&success=saved");

        } catch (IllegalStateException ex) {
            req.setAttribute("error", ex.getMessage());
            doGet(req, resp);
        } catch (Exception ex) {
            req.setAttribute("error", "Save failed: " + ex.getMessage());
            doGet(req, resp);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Returns vet Staff from session, or redirects to login and returns null. */
    private Staff requireVet(HttpServletRequest req,
                              HttpServletResponse resp) throws IOException {
        HttpSession s = req.getSession(false);
        if (s != null) {
            Object o = s.getAttribute("staff");
            if (o instanceof Staff st
                    && "Veterinarian".equalsIgnoreCase(st.getRoleName())) {
                return st;
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
