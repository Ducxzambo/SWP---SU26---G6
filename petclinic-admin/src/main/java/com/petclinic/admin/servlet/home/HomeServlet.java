package com.petclinic.admin.servlet.home;

import com.petclinic.backend.dao.ServiceDAO;
import com.petclinic.backend.model.Customer;
import com.petclinic.backend.model.ServiceCategory;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@WebServlet(urlPatterns = {"", "/", "/home"})
public class HomeServlet extends HttpServlet {

    private final ServiceDAO      serviceDAO      = new ServiceDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        try {
            List<ServiceCategory> categories = serviceDAO.findAllCategoriesWithServices();
            req.setAttribute("navCategories", categories);
            req.setAttribute("today", LocalDate.now().toString());

            HttpSession session = req.getSession(false);
            Customer customer   = (session != null) ? (Customer) session.getAttribute("customer") : null;

            if (customer != null) {
                req.getRequestDispatcher("/WEB-INF/views/customer/home.jsp").forward(req, resp);
            } else {
                req.getRequestDispatcher("/WEB-INF/views/guest/home.jsp").forward(req, resp);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException(e);
        }
    }
}
