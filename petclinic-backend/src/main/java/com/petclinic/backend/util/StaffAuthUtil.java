package com.petclinic.backend.util;

import com.petclinic.backend.model.Staff;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.function.Predicate;

/**
 * Shared session-based role checks for staff-only (manager/vet) screens.
 * <p>
 * Every screen used to repeat its own copy of "read staff from session,
 * check role, redirect to login if missing". Centralizing it here means
 * new screens (e.g. the split-out Inventory servlets) get consistent
 * behavior for free and any future change (session key, login URL) only
 * needs to happen in one place.
 * <p>
 * Usage:
 * <pre>
 *   Staff manager = StaffAuthUtil.requireManager(req, resp);
 *   if (manager == null) return; // response already redirected
 * </pre>
 */
public final class StaffAuthUtil {

    private static final String SESSION_KEY = "staff";
    private static final String LOGIN_URL = "/auth/staff/login";

    private StaffAuthUtil() {
    }

    public static Staff requireManager(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        return requireRole(req, resp, StaffAuthUtil::isManagerRole);
    }

    public static Staff requireVet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        return requireRole(req, resp, "Veterinarian"::equals);
    }

    private static Staff requireRole(HttpServletRequest req, HttpServletResponse resp,
                                     Predicate<String> roleCheck) throws IOException {
        HttpSession session = req.getSession(false);
        Staff staff = (session != null) ? (Staff) session.getAttribute(SESSION_KEY) : null;
        if (staff == null || !roleCheck.test(staff.getRoleName())) {
            resp.sendRedirect(req.getContextPath() + LOGIN_URL);
            return null;
        }
        return staff;
    }

    private static boolean isManagerRole(String roleName) {
        return "Admin".equals(roleName) || "Manager".equals(roleName);
    }
}
