package com.petclinic.servlet.booking;

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
            int inpatientServiceId = firstServiceIdOfCategory(BookingService.INPATIENT_CATEGORY_ID);

            StringBuilder sb = new StringBuilder("{");
            sb.append("\"categories\":").append(categoriesToJson(cats)).append(",");
            sb.append("\"vaccines\":").append(vaccinesToJson(vaccines)).append(",");
            sb.append("\"slots\":").append(slotsToJson(slots)).append(",");
            sb.append("\"vaccineCategoryId\":").append(BookingService.VACCINE_CATEGORY_ID).append(",");
            sb.append("\"vaccineServiceId\":").append(vaccineServiceId).append(",");
            sb.append("\"inpatientServiceId\":").append(inpatientServiceId);
            sb.append("}");

            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write(sb.toString());

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

    // ── JSON builders (chuyển toàn bộ về từ BookingServlet) ────────────────────

    private String slotsToJson(Map<LocalDate, List<TimeSlot>> slots) {
        StringBuilder sb = new StringBuilder("{");
        boolean fd = true;

        for (Map.Entry<LocalDate, List<TimeSlot>> e : slots.entrySet()) {
            if (!fd) sb.append(",");
            fd = false;
            sb.append("\"").append(e.getKey()).append("\":[");

            boolean fs = true;
            for (TimeSlot ts : e.getValue()) {
                if (!fs) sb.append(",");
                fs = false;
                sb.append("{")
                        .append("\"key\":\"").append(ts.getSlotKey()).append("\",")
                        .append("\"display\":\"").append(ts.getDisplayTime()).append("\",")
                        .append("\"available\":").append(ts.isAvailable()).append(",")
                        .append("\"load\":").append(ts.getCurrentLoad()).append(",")
                        .append("\"cap\":").append((int) ts.getMaxCapacity()).append(",")
                        .append("\"fill\":").append(ts.getFillPercent()).append(",")
                        .append("\"groomLoad\":").append(ts.getGroomLoad()).append(",")
                        .append("\"groomCap\":").append(ts.getGroomCap()).append(",")
                        .append("\"vetLoad\":").append(ts.getVetLoad()).append(",")
                        .append("\"vetCap\":").append(ts.getVetCap())
                        .append("}");
            }
            sb.append("]");
        }
        return sb.append("}").toString();
    }

    private String categoriesToJson(List<ServiceCategory> cats) {
        StringBuilder sb = new StringBuilder("[");

        for (int i = 0; i < cats.size(); i++) {
            ServiceCategory cat = cats.get(i);
            if (i > 0) sb.append(",");

            sb.append("{\"id\":").append(cat.getCategoryID())
                    .append(",\"name\":\"").append(esc(cat.getName())).append("\"")
                    .append(",\"services\":[");

            if (cat.getServices() != null) {
                for (int j = 0; j < cat.getServices().size(); j++) {
                    Service s = cat.getServices().get(j);
                    if (j > 0) sb.append(",");

                    sb.append("{\"id\":").append(s.getServiceID())
                            .append(",\"categoryId\":").append(s.getCategoryID())
                            .append(",\"name\":\"").append(esc(s.getName())).append("\"")
                            .append(",\"price\":").append(s.getPrice())
                            .append(",\"duration\":").append(s.getDurationMinutes())
                            .append("}");
                }
            }
            sb.append("]}");
        }
        return sb.append("]").toString();
    }

    private String vaccinesToJson(List<Vaccine> vaccines) {
        StringBuilder sb = new StringBuilder("[");

        for (int i = 0; i < vaccines.size(); i++) {
            Vaccine v = vaccines.get(i);
            if (i > 0) sb.append(",");

            sb.append("{\"id\":").append(v.getVaccineID())
                    .append(",\"name\":\"").append(esc(v.getName())).append("\"")
                    .append(",\"price\":").append(v.getUnitPrice())
                    .append(",\"stock\":").append(v.getStockQty())
                    .append("}");
        }
        return sb.append("]").toString();
    }

    private String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}