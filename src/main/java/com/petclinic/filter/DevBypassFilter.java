package com.petclinic.filter;

import com.petclinic.model.Customer;
import com.petclinic.model.Staff;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.LocalDate;

/**
 * ══════════════════════════════════════════════════════════
 *  DEV ONLY — inject fake session to bypass login for test.
 *
 *  HOW TO USE:
 *   • Set DEV_MODE = true  → all requests get fake session
 *   • Set DEV_MODE = false → filter does nothing (production)
 *
 *  Switch role via URL param (DEV_MODE must be true):
 *   ?devRole=vet            → Veterinarian
 *   ?devRole=receptionist   → Receptionist
 *   ?devRole=customer       → Customer
 *   ?devRole=groomer        → Groomer
 *   ?devRole=manager        → Manager
 *
 *  Or change DEFAULT_ROLE below to test a specific page permanently.
 *
 *  ⚠️  SET DEV_MODE = false BEFORE DEMO / SUBMISSION
 * ══════════════════════════════════════════════════════════
 */
@WebFilter("/*")
public class DevBypassFilter implements Filter {

    // ── Toggle ────────────────────────────────────────────────────────────────
    private static final boolean DEV_MODE = true;

    // ── Đổi role này để test trang tương ứng ─────────────────────────────────
    // "vet"          → /inpatient/assessment, /inpatient/list (vet view)
    // "receptionist" → /inpatient/admit, /inpatient/list (recep), /inpatient/discharge
    // "customer"     → /inpatient/list (customer view), /inpatient/detail
    private static final String DEFAULT_ROLE = "receptionist";

    // ── Fake IDs — phải tồn tại trong DB ─────────────────────────────────────
    private static final int FAKE_VET_ID      = 1;
    private static final int FAKE_RECEP_ID    = 2;
    private static final int FAKE_GROOMER_ID  = 3;
    private static final int FAKE_MANAGER_ID  = 4;
    private static final int FAKE_CUSTOMER_ID = 1;

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp,
                         FilterChain chain) throws IOException, ServletException {

        if (!DEV_MODE) { chain.doFilter(req, resp); return; }

        HttpServletRequest  request = (HttpServletRequest) req;
        HttpSession         session = request.getSession(true);

        // ?devRole param → re-inject immediately (lets you switch role during test)
        String paramRole   = request.getParameter("devRole");
        boolean forceInject = paramRole != null && !paramRole.isBlank();

        boolean alreadySet = session.getAttribute("staff")    != null
                || session.getAttribute("customer") != null;

        if (!alreadySet || forceInject) {
            String role = forceInject ? paramRole.toLowerCase() : DEFAULT_ROLE;
            inject(session, role);
        }

        chain.doFilter(req, resp);
    }

    // ── Inject fake session by role ───────────────────────────────────────────
    private void inject(HttpSession session, String role) {
        session.removeAttribute("staff");
        session.removeAttribute("customer");

        switch (role) {
            case "vet", "veterinarian" ->
                    session.setAttribute("staff",
                            fakeStaff(FAKE_VET_ID, "Dr. Nguyen Van A",
                                    "vet@clinic.local", "Veterinarian", 2));

            case "recep", "receptionist" ->
                    session.setAttribute("staff",
                            fakeStaff(FAKE_RECEP_ID, "Le Thi B",
                                    "recep@clinic.local", "Receptionist", 3));

            case "groomer" ->
                    session.setAttribute("staff",
                            fakeStaff(FAKE_GROOMER_ID, "Tran Van C",
                                    "groomer@clinic.local", "Groomer", 4));

            case "manager" ->
                    session.setAttribute("staff",
                            fakeStaff(FAKE_MANAGER_ID, "Pham Thi D",
                                    "manager@clinic.local", "Manager", 5));

            default ->     // "customer"
                    session.setAttribute("customer", fakeCustomer());
        }
    }

    // ── Fake object builders ──────────────────────────────────────────────────

    /**
     * Build a fake Staff object using only methods confirmed to exist
     * in the project's Staff.class (extracted from compiled .class file).
     *
     * Confirmed methods: setStaffID, setFullName, setEmail, setPhone,
     * setRoleName, setRoleID, setSpecialization, setLicenseNumber,
     * setPasswordHash, setActive
     *
     * New schema has HireDate column in DB but Staff model may or may
     * not have setHireDate() — we add it safely inside try/catch via
     * reflection to avoid compile error on older model versions.
     */
    private Staff fakeStaff(int id, String name, String email,
                            String roleName, int roleID) {
        Staff s = new Staff();
        s.setStaffID(id);
        s.setFullName(name);
        s.setEmail(email);
        s.setRoleName(roleName);
        s.setRoleID(roleID);
        s.setActive(true);
        // HireDate: exists in new schema Staff table
        // Call via reflection to avoid compile error if model doesn't have it yet
        try {
            Staff.class.getMethod("setHireDate", LocalDate.class)
                    .invoke(s, LocalDate.of(2024, 1, 1));
        } catch (Exception ignored) {
            // setHireDate not in model yet — skip silently
        }
        return s;
    }

    private Customer fakeCustomer() {
        Customer c = new Customer();
        c.setCustomerID(FAKE_CUSTOMER_ID);
        c.setFullName("Test Customer");
        c.setEmail("customer@clinic.local");
        c.setPhone("0900000000");
        c.setActive(true);
        return c;
    }

    @Override public void init(FilterConfig cfg) {}
    @Override public void destroy() {}
}