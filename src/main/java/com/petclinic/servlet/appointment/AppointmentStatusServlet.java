package com.petclinic.servlet.appointment;

import com.petclinic.dao.AppointmentDAO;
import com.petclinic.model.Appointment;
import com.petclinic.model.Customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

/** GET /appointments/status?id=123 → {"status":"Confirmed"} */
@WebServlet("/appointments/status")
public class AppointmentStatusServlet extends HttpServlet {

    private final AppointmentDAO dao = new AppointmentDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession sess = req.getSession(false);
        Customer customer = sess != null ? (Customer) sess.getAttribute("customer") : null;
        if (customer == null) { resp.sendError(401); return; }

        String idStr = req.getParameter("id");
        if (idStr == null) { resp.sendError(400); return; }

        try {
            int id = Integer.parseInt(idStr);
            Appointment a = dao.findById(id);
            if (a == null || a.getCustomerID() != customer.getCustomerID()) {
                resp.sendError(404); return;
            }
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"status\":\"" + a.getStatus() + "\"}");
        } catch (Exception e) {
            resp.sendError(500);
        }
    }
}
