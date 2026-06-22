package com.petclinic.servlet.auth;

import com.petclinic.dao.StaffDAO;
import com.petclinic.model.Staff;
import com.petclinic.util.PasswordUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Staff login / logout.
 * <p>
 * GET  /auth/staff/login  → show login form
 * POST /auth/staff/login  → authenticate and create staff session
 * GET  /auth/staff/logout → invalidate session → redirect to login
 */
@WebServlet(urlPatterns = {"/auth/staff/login", "/auth/staff/logout"})
public class StaffAuthServlet extends HttpServlet {

    private final StaffDAO staffDAO = new StaffDAO();

    // ── GET ───────────────────────────────────────────────────────────────────
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String path = req.getServletPath();

        if ("/auth/staff/logout".equals(path)) {
            HttpSession session = req.getSession(false);
            if (session != null) session.invalidate();
            resp.sendRedirect(req.getContextPath() + "/auth/staff/login");
            return;
        }

        // Already logged in?
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("staff") != null) {
            redirectByRole((Staff) session.getAttribute("staff"), req, resp);
            return;
        }

        req.getRequestDispatcher("/WEB-INF/views/auth/staff-login.jsp").forward(req, resp);
    }

    // ── POST ──────────────────────────────────────────────────────────────────
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        String email = req.getParameter("email");
        String password = req.getParameter("password");

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            forward(req, resp, "Vui lòng nhập đầy đủ thông tin.");
            return;
        }

        try {
            Staff staff = staffDAO.findByEmail(email.trim());
            if (staff == null || !PasswordUtil.verifyPassword(password, staff.getPasswordHash())) {
                forward(req, resp, "Email hoặc mật khẩu không đúng.");
                return;
            }

            HttpSession session = req.getSession(true);
            session.setAttribute("staff", staff);
            session.setMaxInactiveInterval(60 * 60 * 10); // 10 hours

            redirectByRole(staff, req, resp);

        } catch (SQLException e) {
            e.printStackTrace();
            forward(req, resp, "Lỗi hệ thống, vui lòng thử lại.");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void redirectByRole(Staff staff, HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String ctx = req.getContextPath();
        switch (staff.getRoleName()) {
            case "Receptionist":
                resp.sendRedirect(ctx + "/receptionist/checkin");
                break;
            case "Veterinarian":
                resp.sendRedirect(ctx + "/vet/examination");
                break;
            case "Groomer":
                resp.sendRedirect(ctx + "/groomer/session");
                break;
            default:
                resp.sendRedirect(ctx + "/");
        }
    }

    private void forward(HttpServletRequest req, HttpServletResponse resp, String error)
            throws ServletException, IOException {
        req.setAttribute("error", error);
        req.setAttribute("email", req.getParameter("email"));
        req.getRequestDispatcher("/WEB-INF/views/auth/staff-login.jsp").forward(req, resp);
    }
}
