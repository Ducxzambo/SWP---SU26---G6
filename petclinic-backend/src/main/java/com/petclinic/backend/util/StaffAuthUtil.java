package com.petclinic.backend.util;

import com.petclinic.backend.model.Staff;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.function.Predicate;

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
