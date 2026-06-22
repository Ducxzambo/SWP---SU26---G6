package com.petclinic.servlet.inpatient;

import com.petclinic.model.Customer;
import com.petclinic.model.Staff;
import com.petclinic.service.InpatientService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

/**
 * BP-04 — View inpatient list and detail.
 *
 * GET /inpatient/list
 *   Staff (Vet / Receptionist) → all active admissions
 *   Customer                   → own pets only
 *
 * GET /inpatient/detail?id=X
 *   All actors → single admission + assessment history
 *   (each actor gets a different JSP view)
 */
//@WebServlet(urlPatterns = {"/inpatient/list", "/inpatient/detail"})
public class InpatientListServlet extends HttpServlet {

    private final InpatientService svc = new InpatientService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String path = req.getServletPath();

        if ("/inpatient/detail".equals(path)) {
            showDetail(req, resp);
        } else {
            showList(req, resp);
        }
    }

    // ── List ──────────────────────────────────────────────────────────────────

    private void showList(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        if (session == null) { redirectLogin(req, resp); return; }

        Staff    staff    = (Staff)    session.getAttribute("staff");
        Customer customer = (Customer) session.getAttribute("customer");

        try {
            if (staff != null) {
                req.setAttribute("admissions", svc.getActiveAdmissions());
                req.setAttribute("staff", staff);

                String view = "Veterinarian".equalsIgnoreCase(staff.getRoleName())
                    ? "/WEB-INF/views/vet/inpatient-list.jsp"
                    : "/WEB-INF/views/receptionist/inpatient-list.jsp";

                forward(req, resp, view);

            } else if (customer != null) {
                req.setAttribute("admissions",
                    svc.getAdmissionsForCustomer(customer.getCustomerID()));
                req.setAttribute("customer", customer);
                forward(req, resp, "/WEB-INF/views/customer/inpatient-status.jsp");

            } else {
                redirectLogin(req, resp);
            }

        } catch (Exception e) {
            req.setAttribute("error", "Error loading inpatient list: " + e.getMessage());
            forward(req, resp, "/WEB-INF/views/error/500.jsp");
        }
    }

    // ── Detail ────────────────────────────────────────────────────────────────

    private void showDetail(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        if (session == null) { redirectLogin(req, resp); return; }

        String idStr = req.getParameter("id");
        if (blank(idStr)) {
            resp.sendRedirect(req.getContextPath() + "/inpatient/list");
            return;
        }

        try {
            int id = Integer.parseInt(idStr.trim());
            req.setAttribute("admission",   svc.getAdmission(id));
            req.setAttribute("assessments", svc.getAssessments(id));
            req.setAttribute("todayDone",   svc.hasTodayAssessment(id));

            Staff    staff    = (Staff)    session.getAttribute("staff");
            Customer customer = (Customer) session.getAttribute("customer");

            String view;
            if (staff != null && "Veterinarian".equalsIgnoreCase(staff.getRoleName())) {
                req.setAttribute("staff", staff);
                view = "/WEB-INF/views/vet/inpatient-detail.jsp";
            } else if (staff != null) {
                req.setAttribute("staff", staff);
                view = "/WEB-INF/views/receptionist/inpatient-detail.jsp";
            } else {
                view = "/WEB-INF/views/customer/inpatient-detail.jsp";
            }

            forward(req, resp, view);

        } catch (Exception e) {
            req.setAttribute("error", "Error: " + e.getMessage());
            resp.sendRedirect(req.getContextPath() + "/inpatient/list");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void forward(HttpServletRequest req, HttpServletResponse resp,
                          String path) throws ServletException, IOException {
        req.getRequestDispatcher(path).forward(req, resp);
    }

    private void redirectLogin(HttpServletRequest req,
                                HttpServletResponse resp) throws IOException {
        resp.sendRedirect(req.getContextPath() + "/auth/login");
    }

    private boolean blank(String s) { return s == null || s.isBlank(); }
}
