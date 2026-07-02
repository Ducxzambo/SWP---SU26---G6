package com.petclinic.servlet.review;

import com.petclinic.dao.*;
import com.petclinic.model.*;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;

/**
 * URL map:
 *   POST /reviews/submit   — customer submits review after Done appointment
 *   GET  /community        — public review wall (guest + customer)
 */
@WebServlet(urlPatterns = {"/reviews/submit", "/community"})
public class ReviewServlet extends HttpServlet {

    private final ReviewDAO      reviewDAO      = new ReviewDAO();
    private final AppointmentDAO appointmentDAO = new AppointmentDAO();
    private final ServiceDAO     serviceDAO     = new ServiceDAO();

    // ── POST /reviews/submit ──────────────────────────────────────────────────
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        HttpSession session = req.getSession(false);
        Customer customer   = session != null ? (Customer) session.getAttribute("customer") : null;
        if (customer == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login"); return;
        }

        int    apptId  = parseId(req.getParameter("appointmentId"));
        int    rating  = parseId(req.getParameter("rating"));
        String comment = req.getParameter("comment");
        boolean pub    = "on".equals(req.getParameter("isPublic"));

        // Validate
        if (apptId <= 0 || rating < 1 || rating > 5) {
            session.setAttribute("flashError", "Dữ liệu đánh giá không hợp lệ.");
            resp.sendRedirect(req.getContextPath() + "/appointments/detail?id=" + apptId);
            return;
        }

        try {
            // Ownership + status check
            Appointment appt = appointmentDAO.findById(apptId);
            if (appt == null || appt.getCustomerID() != customer.getCustomerID()
                    || !"Done".equals(appt.getStatus())) {
                session.setAttribute("flashError", "Chỉ có thể đánh giá lịch khám đã hoàn thành.");
                resp.sendRedirect(req.getContextPath() + "/appointments");
                return;
            }

            Review r = new Review();
            r.setAppointmentID(apptId);
            r.setCustomerID(customer.getCustomerID());
            r.setRating(rating);
            r.setComment(comment != null ? comment.trim() : "");
            r.setIsPublic(pub);

            Review existingReview = reviewDAO.findByAppointment(apptId);
            if (existingReview != null) {
                reviewDAO.update(r);
                session.setAttribute("flashSuccess", "Đã cập nhật đánh giá thành công!");
            } else {
                reviewDAO.insert(r);
                session.setAttribute("flashSuccess", "Cảm ơn bạn đã đánh giá!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("flashError", "Đã xảy ra lỗi khi lưu đánh giá.");
        }

        resp.sendRedirect(req.getContextPath() + "/appointments/detail?id=" + apptId);
    }

    // ── GET /community ────────────────────────────────────────────────────────
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Filters from query params
        int    categoryId  = parseId(req.getParameter("categoryId"));
        int    serviceId   = parseId(req.getParameter("serviceId"));
        int    staffId       = parseId(req.getParameter("staffId"));
        String petSpecies  = req.getParameter("petSpecies");
        int    minRating   = parseId(req.getParameter("minRating"));
        String sortBy      = req.getParameter("sort");

        try {
            List<Review> reviews = reviewDAO.findPublic(
                    categoryId, serviceId, staffId, petSpecies, minRating, sortBy);
            List<ServiceCategory> categories = serviceDAO.findAllCategoriesWithServices();
            List<String>          species    = reviewDAO.findPublicSpecies();

            // Aggregate stats
            double avgAll = reviews.stream().mapToInt(Review::getRating).average().orElse(0);
            long   count  = reviews.size();

            req.setAttribute("reviews",      reviews);
            req.setAttribute("categories",   categories);
            req.setAttribute("navCategories",categories);
            req.setAttribute("allSpecies",   species);
            req.setAttribute("avgRating",    String.format("%.1f", avgAll));
            req.setAttribute("totalReviews", count);

            // Pass back active filters for UI state
            req.setAttribute("f_categoryId", categoryId);
            req.setAttribute("f_serviceId",  serviceId);
            req.setAttribute("f_staffId",      staffId);
            req.setAttribute("f_petSpecies", petSpecies != null ? petSpecies : "");
            req.setAttribute("f_minRating",  minRating);
            req.setAttribute("f_sort",       sortBy != null ? sortBy : "newest");

            // Auth for header
            HttpSession session = req.getSession(false);
            if (session != null && session.getAttribute("customer") != null) {
                Customer c = (Customer) session.getAttribute("customer");
                req.setAttribute("unreadCount",
                        new NotificationDAO().countUnread(c.getCustomerID()));
            }

            req.getRequestDispatcher("/WEB-INF/views/community/reviews.jsp")
                    .forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException(e);
        }
    }

    private int parseId(String s) {

        try {
            return Integer.parseInt(s);
        } catch (Exception e)
        {
            return 0;
        }
    }
}