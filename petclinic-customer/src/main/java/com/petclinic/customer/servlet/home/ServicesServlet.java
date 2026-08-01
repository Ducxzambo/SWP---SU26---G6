package com.petclinic.customer.servlet.home;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import com.petclinic.backend.dao.NotificationDAO;
import com.petclinic.backend.dao.ServiceDAO;
import com.petclinic.backend.model.Customer;
import com.petclinic.backend.model.ServiceCategory;

import java.io.IOException;
import java.util.List;

@WebServlet(urlPatterns = {"/services"})
public class ServicesServlet extends HttpServlet {

    private final ServiceDAO serviceDAO      = new ServiceDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        try {
            List<ServiceCategory> categories = serviceDAO.findAllCategoriesWithServices();

            req.setAttribute("categories",    categories);
            req.setAttribute("navCategories", categories);
            req.setAttribute("focusCategory", parseId(req.getParameter("category")));
            req.setAttribute("focusService",  parseId(req.getParameter("service")));

            attachCustomerContext(req);

            req.getRequestDispatcher("/WEB-INF/views/guest/services.jsp").forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException(e);
        }
    }

    private void attachCustomerContext(HttpServletRequest req) throws Exception {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("customer") != null) {
            Customer c = (Customer) session.getAttribute("customer");
            req.setAttribute("unreadCount", notificationDAO.countUnread(c.getCustomerID()));
        }
    }

    private int parseId(String s) {
        try { return Integer.parseInt(s); }
        catch (Exception e) { return 0; }
    }
}
