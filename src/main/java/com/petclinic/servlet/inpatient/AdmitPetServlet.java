package com.petclinic.servlet.inpatient;

import com.petclinic.model.Staff;
import com.petclinic.service.InpatientService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;

/**
 * BP-04 Step 1 — Receptionist admits a pet.
 *
 * GET  /inpatient/admit?recordId=X&petId=Y&appointmentId=Z
 *      → show cage-selection form
 *
 * POST /inpatient/admit
 *      → validate cage, create InpatientAdmissions row
 *      → redirect to /inpatient/detail?id={newId}&success=admitted
 */
//@WebServlet("/inpatient/admit")
public class AdmitPetServlet extends HttpServlet {

    private final InpatientService svc = new InpatientService();

    // ── GET — show admit form ─────────────────────────────────────────────────
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Staff staff = requireRole(req, resp, "Receptionist");
        if (staff == null) return;

        try {
            List<String> occupied = svc.getOccupiedCages();
            req.setAttribute("occupiedCages",  occupied);
            req.setAttribute("recordId",        req.getParameter("recordId"));
            req.setAttribute("petId",           req.getParameter("petId"));
            req.setAttribute("appointmentId",   req.getParameter("appointmentId"));
            forward(req, resp, "/WEB-INF/views/receptionist/inpatient-admit.jsp");
        } catch (Exception e) {
            req.setAttribute("error", "Cannot load cage list: " + e.getMessage());
            forward(req, resp, "/WEB-INF/views/receptionist/inpatient-admit.jsp");
        }
    }

    // ── POST — save admission ─────────────────────────────────────────────────
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Staff staff = requireRole(req, resp, "Receptionist");
        if (staff == null) return;

        req.setCharacterEncoding("UTF-8");

        String recordIdStr = req.getParameter("recordId");
        String petIdStr    = req.getParameter("petId");
        String cageNumber  = req.getParameter("cageNumber");

        // basic validation
        if (blank(recordIdStr) || blank(petIdStr) || blank(cageNumber)) {
            req.setAttribute("error", "All fields are required.");
            doGet(req, resp);
            return;
        }

        try {
            int admissionId = svc.admitPet(
                Integer.parseInt(recordIdStr.trim()),
                Integer.parseInt(petIdStr.trim()),
                cageNumber.trim()
            );
            resp.sendRedirect(req.getContextPath()
                + "/inpatient/detail?id=" + admissionId + "&success=admitted");

        } catch (IllegalStateException ex) {
            // cage already occupied
            req.setAttribute("error", ex.getMessage());
            doGet(req, resp);
        } catch (Exception ex) {
            req.setAttribute("error", "Admission failed: " + ex.getMessage());
            doGet(req, resp);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Staff requireRole(HttpServletRequest req, HttpServletResponse resp,
                               String role) throws IOException {
        HttpSession s = req.getSession(false);
        if (s != null) {
            Object o = s.getAttribute("staff");
            if (o instanceof Staff st && role.equalsIgnoreCase(st.getRoleName())) {
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
