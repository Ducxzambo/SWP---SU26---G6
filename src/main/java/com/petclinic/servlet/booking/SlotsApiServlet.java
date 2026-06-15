package com.petclinic.servlet.booking;

import com.petclinic.model.*;
import com.petclinic.service.BookingService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

/**
 * GET /booking/slots?serviceIds=1,2,3
 * Returns updated slot JSON after service selection changes.
 */
@WebServlet("/booking/slots")
public class SlotsApiServlet extends HttpServlet {

    private final BookingService bookingSvc = new BookingService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Auth check
        HttpSession sess = req.getSession(false);
        if (sess == null || sess.getAttribute("customer") == null) {
            resp.sendError(401); return;
        }

        String svcParam = req.getParameter("serviceIds");
        List<Integer> svcIds = new ArrayList<>();
        if (svcParam != null && !svcParam.isBlank()) {
            for (String id : svcParam.split(",")) {
                try { svcIds.add(Integer.parseInt(id.trim())); } catch (Exception ignored) {}
            }
        }

        try {
            Map<LocalDate, List<TimeSlot>> slots = bookingSvc.generateSlots(svcIds);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write(slotsToJson(slots));
        } catch (Exception e) {
            resp.sendError(500, e.getMessage());
        }
    }

    private String slotsToJson(Map<LocalDate, List<TimeSlot>> slots) {
        StringBuilder sb = new StringBuilder("{");
        boolean fd = true;
        for (Map.Entry<LocalDate, List<TimeSlot>> e : slots.entrySet()) {
            if (!fd) sb.append(","); fd = false;
            sb.append("\"").append(e.getKey()).append("\":[");
            boolean fs = true;
            for (TimeSlot ts : e.getValue()) {
                if (!fs) sb.append(","); fs = false;
                sb.append("{")
                  .append("\"key\":\"").append(ts.getSlotKey()).append("\",")
                  .append("\"display\":\"").append(ts.getDisplayTime()).append("\",")
                  .append("\"available\":").append(ts.isAvailable()).append(",")
                  .append("\"load\":").append(ts.getCurrentLoad()).append(",")
                  .append("\"cap\":").append(ts.getMaxCapacity()).append(",")
                  .append("\"fill\":").append(ts.getFillPercent())
                  .append("}");
            }
            sb.append("]");
        }
        return sb.append("}").toString();
    }
}
