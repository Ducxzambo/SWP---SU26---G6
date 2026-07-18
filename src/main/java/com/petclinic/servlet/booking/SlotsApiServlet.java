package com.petclinic.servlet.booking;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.petclinic.dao.ServiceDAO;
import com.petclinic.dao.VaccineDAO;
import com.petclinic.dto.TimeSlot;
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
 */
@WebServlet("/booking/slots")
public class SlotsApiServlet extends HttpServlet {

    private final BookingService bookingSvc = new BookingService();
    private final ServiceDAO serviceDAO = new ServiceDAO();
    private final VaccineDAO vaccineDAO = new VaccineDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Auth check
        HttpSession sess = req.getSession(false);
        if (sess == null || sess.getAttribute("customer") == null) {
            resp.sendError(401);
            return;
        }

        String svcParam = req.getParameter("serviceIds");
        List<Integer> svcIds = new ArrayList<>();

        if (svcParam != null && !svcParam.isBlank()) {
            for (String id : svcParam.split(",")) {
                try {
                    svcIds.add(Integer.parseInt(id.trim()));
                } catch (Exception ignored) {
                }
            }
        }

        try {
            // Loai Noi tru (co luong rieng) VA loai theo TEN "Dieu tri"/"Chan
            // doan" (bac si chi dinh tai phong kham, khong phai lua chon cua
            // khach khi dat lich).
            List<ServiceCategory> cats = serviceDAO.findBookableCategoriesWithServices();

            List<Vaccine> vaccines = vaccineDAO.findAvailable();
            Map<LocalDate, List<TimeSlot>> slots = bookingSvc.generateSlots(svcIds);

            int vaccineServiceId = firstServiceIdOfCategory(BookingService.VACCINE_CATEGORY_ID);

            JsonObject root = new JsonObject();
            root.add("categories", categoriesToJson(cats));
            root.add("vaccines", vaccinesToJson(vaccines));
            root.add("slots", slotsToJson(slots));
            root.addProperty("vaccineCategoryId", BookingService.VACCINE_CATEGORY_ID);
            root.addProperty("vaccineServiceId", vaccineServiceId);

            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write(root.toString());

        } catch (Exception e) {
            resp.sendError(500, e.getMessage());
        }
    }

    /**
     * Service "placeholder" đầu tiên (IsActive) của 1 category — dùng cho Vaccine/Inpatient.
     */
    private int firstServiceIdOfCategory(int categoryId) throws Exception {
        List<Service> svcs = serviceDAO.findByCategory(categoryId);
        return svcs.isEmpty() ? -1 : svcs.get(0).getServiceID();
    }

    private JsonObject slotsToJson(Map<LocalDate, List<TimeSlot>> slots) {
        JsonObject root = new JsonObject();
        for (Map.Entry<LocalDate, List<TimeSlot>> e : slots.entrySet()) {
            JsonArray dayArr = new JsonArray();
            for (TimeSlot ts : e.getValue()) {
                JsonObject o = new JsonObject();
                o.addProperty("key", ts.getSlotKey());
                o.addProperty("display", ts.getDisplayTime());
                o.addProperty("available", ts.isAvailable());
                o.addProperty("placeholder", ts.isPlaceholder());
                o.addProperty("load", ts.getCurrentLoad());
                o.addProperty("cap", (int) ts.getMaxCapacity());
                o.addProperty("fill", ts.getFillPercent());
                o.addProperty("groomLoad", ts.getGroomLoad());
                o.addProperty("groomCap", ts.getGroomCap());
                o.addProperty("vetLoad", ts.getVetLoad());
                o.addProperty("vetCap", ts.getVetCap());
                dayArr.add(o);
            }
            root.add(e.getKey().toString(), dayArr);
        }
        return root;
    }

    private JsonArray categoriesToJson(List<ServiceCategory> cats) {
        JsonArray arr = new JsonArray();
        for (ServiceCategory cat : cats) {
            JsonObject o = new JsonObject();
            o.addProperty("id", cat.getCategoryID());
            o.addProperty("name", cat.getName());

            JsonArray svcArr = new JsonArray();
            if (cat.getServices() != null) {
                for (Service s : cat.getServices()) {
                    JsonObject so = new JsonObject();
                    so.addProperty("id", s.getServiceID());
                    so.addProperty("categoryId", s.getCategoryID());
                    so.addProperty("name", s.getName());
                    so.addProperty("price", s.getPrice());
                    so.addProperty("duration", s.getDurationMinutes());
                    svcArr.add(so);
                }
            }
            o.add("services", svcArr);
            arr.add(o);
        }
        return arr;
    }

    private JsonArray vaccinesToJson(List<Vaccine> vaccines) {
        JsonArray arr = new JsonArray();
        for (Vaccine v : vaccines) {
            JsonObject o = new JsonObject();
            o.addProperty("id", v.getVaccineID());
            o.addProperty("name", v.getName());
            o.addProperty("price", v.getUnitPrice());
            o.addProperty("stock", v.getStockQty());
            arr.add(o);
        }
        return arr;
    }
}
