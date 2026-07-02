package com.petclinic.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Protects customer-facing URLs that require login.
 * Redirects unauthenticated users to /auth/login,
 * saving the originally requested URL so they can be
 * redirected back after login.
 */
@WebFilter(urlPatterns = {"/profile/*", "/appointments/*", "/pets/*", "/invoices/*"})
public class CustomerAuthFilter implements Filter {

    private static final Set<String> PUBLIC_PATHS = new HashSet<>(Arrays.asList(
            "/auth/login", "/auth/register", "/auth/register/verify",
            "/auth/google", "/auth/google/callback",
            "/auth/forgot", "/auth/forgot/verify", "/auth/forgot/reset",
            "/auth/logout"
    ));

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  request  = (HttpServletRequest)  req;
        HttpServletResponse response = (HttpServletResponse) resp;

        String path = request.getServletPath();

        // Always allow public paths
        if (isPublic(path)) { chain.doFilter(req, resp); return; }

        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("customer") != null) {
            chain.doFilter(req, resp);
        } else {
            // Save the target URL for redirect after login
            HttpSession s = request.getSession(true);
            s.setAttribute("redirectAfterLogin", request.getRequestURI()
                    + (request.getQueryString() != null ? "?" + request.getQueryString() : ""));
            response.sendRedirect(request.getContextPath() + "/auth/login");
        }
    }

    private boolean isPublic(String path) {
        if (PUBLIC_PATHS.contains(path)) return true;
        if (path.startsWith("/css/") || path.startsWith("/js/") ||
                path.startsWith("/assets/") || path.startsWith("/images/")) return true;
        return false;
    }

    @Override public void init(FilterConfig config) {}
    @Override public void destroy() {}
}