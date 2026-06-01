package com.petclinic.filter;

import com.petclinic.model.User;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.Set;

/**
 * Bảo vệ các route yêu cầu đăng nhập.
 * Mapping: tất cả URL, nhưng whitelist các trang public.
 */
@WebFilter("/*")
public class AuthFilter implements Filter {

    // Các URL không cần đăng nhập
    private static final Set<String> PUBLIC_PATHS = Set.of(
        "/login", "/logout", "/register",
        "/forgot-password", "/reset-password"
    );

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  request  = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String path = request.getServletPath();

        // Cho qua: tài nguyên tĩnh và public pages
        if (path.startsWith("/assets/") || PUBLIC_PATHS.contains(path)) {
            chain.doFilter(req, res);
            return;
        }

        HttpSession session = request.getSession(false);
        User currentUser = (session != null)
            ? (User) session.getAttribute("currentUser")
            : null;

        if (currentUser == null) {
            // Lưu URL gốc để redirect sau khi login
            String originalUrl = request.getRequestURI();
            response.sendRedirect(request.getContextPath() + "/login?next=" + originalUrl);
            return;
        }

        // Role-based access control
        String roleName = currentUser.getRoleName();
        if (path.startsWith("/admin/") && !"Admin".equals(roleName)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Không có quyền truy cập.");
            return;
        }
        if (path.startsWith("/vet/") && !"Veterinarian".equals(roleName) && !"Admin".equals(roleName)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Không có quyền truy cập.");
            return;
        }

        chain.doFilter(req, res);
    }
}
