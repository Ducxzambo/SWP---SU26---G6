package com.petclinic.filter;

import com.petclinic.model.Customer;
import com.petclinic.model.Staff;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.IOException;

/**
 * Protects /inpatient/* URLs.
 * Allows: Staff (any role) OR Customer (read-only list/detail).
 * Blocks:  unauthenticated requests → redirect to /auth/login.
 *
 * Role-level enforcement is done inside each Servlet.
 */
@WebFilter("/inpatient/*")
public class InpatientAuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  request  = (HttpServletRequest)  req;
        HttpServletResponse response = (HttpServletResponse) resp;

        HttpSession session = request.getSession(false);
        if (session != null) {
            Staff    staff    = (Staff)    session.getAttribute("staff");
            Customer customer = (Customer) session.getAttribute("customer");
            if (staff != null || customer != null) {
                chain.doFilter(req, resp);
                return;
            }
        }

        // Not logged in — save target URL and redirect
        HttpSession s = request.getSession(true);
        s.setAttribute("redirectAfterLogin",
            request.getRequestURI() +
            (request.getQueryString() != null ? "?" + request.getQueryString() : ""));
        response.sendRedirect(request.getContextPath() + "/auth/login");
    }

    @Override public void init(FilterConfig config) {}
    @Override public void destroy() {}
}
