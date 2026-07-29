package com.petclinic.customer.servlet.home;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import com.petclinic.backend.dao.AppointmentDAO;
import com.petclinic.backend.dao.NotificationDAO;
import com.petclinic.backend.dao.PetDAO;
import com.petclinic.backend.dao.ServiceDAO;
import com.petclinic.backend.model.Appointment;
import com.petclinic.backend.model.Customer;
import com.petclinic.backend.model.ServiceCategory;

import java.io.IOException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@WebServlet(urlPatterns = {"", "/", "/home"})
public class HomeServlet extends HttpServlet {

    private final ServiceDAO serviceDAO           = new ServiceDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();
    private final AppointmentDAO appointmentDAO   = new AppointmentDAO();
    private final PetDAO petDAO                   = new PetDAO();

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
                req.setAttribute("unreadCount", notificationDAO.countUnread(customer.getCustomerID()));

                List<Appointment> appts = appointmentDAO.findByCustomer(customer.getCustomerID());
                LocalDate today = LocalDate.now();

                Map<String, Integer> statusDist = new LinkedHashMap<>();
                statusDist.put("Pending", 0);
                statusDist.put("Confirmed", 0);
                statusDist.put("InProgress", 0);
                statusDist.put("Done", 0);
                statusDist.put("Cancelled", 0);
                statusDist.put("NoShow", 0);

                int upcomingCount = 0, doneCount = 0;
                for (Appointment a : appts) {
                    statusDist.merge(a.getStatus(), 1, Integer::sum);
                    boolean active = "Pending".equals(a.getStatus()) || "Confirmed".equals(a.getStatus())
                            || "InProgress".equals(a.getStatus());
                    boolean futureOrToday = a.getAppointmentDate() != null && !a.getAppointmentDate().isBefore(today);
                    if (active && futureOrToday) upcomingCount++;
                    if ("Done".equals(a.getStatus())) doneCount++;
                }

                int maxDist = statusDist.values().stream().mapToInt(Integer::intValue).max().orElse(1);

                req.setAttribute("totalAppointments", appts.size());
                req.setAttribute("upcomingCount", upcomingCount);
                req.setAttribute("doneCount", doneCount);
                req.setAttribute("totalPets", petDAO.findByCustomer(customer.getCustomerID()).size());
                req.setAttribute("statusDist", statusDist);
                req.setAttribute("maxDist", Math.max(maxDist, 1));
                req.setAttribute("recentAppointments", appts.subList(0, Math.min(5, appts.size())));

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