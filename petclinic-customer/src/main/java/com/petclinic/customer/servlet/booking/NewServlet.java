package com.petclinic.customer.servlet.booking;

import com.google.gson.JsonObject;
import com.petclinic.backend.model.Pet;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import com.petclinic.backend.dao.PetDAO;
import com.petclinic.backend.dao.ServiceDAO;
import com.petclinic.backend.dao.VaccineDAO;
import com.petclinic.backend.dto.BookingSelection;
import com.petclinic.backend.model.Customer;
import com.petclinic.backend.model.Service;
import com.petclinic.backend.model.Vaccine;
import com.petclinic.backend.service.BookingService;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@WebServlet(urlPatterns = {"/booking/new"})
public class NewServlet extends HttpServlet {

    private final ServiceDAO serviceDAO = new ServiceDAO();
    private final PetDAO petDAO = new PetDAO();
    private final VaccineDAO vaccineDAO = new VaccineDAO();
    private final BookingService bookingSvc = new BookingService();

    // ── GET ───────────────────────────────────────────────────────────────────

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Customer customer = requireLogin(req, resp);
        if (customer == null) return;

        try {
            String prefillCat = req.getParameter("prefillCat");

            req.setAttribute("navCategories", serviceDAO.findAllCategoriesWithServices());
            req.setAttribute("today", LocalDate.now().toString());
            req.setAttribute("prefillCat", prefillCat != null ? prefillCat : "");
            req.setAttribute("pets", petDAO.findByCustomerId(customer.getCustomerID()));
            req.setAttribute("resumeData", buildResumeJson(req));

            req.getRequestDispatcher("/WEB-INF/views/booking/new.jsp").forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException(e);
        }
    }

    private String buildResumeJson(HttpServletRequest req) {
        HttpSession sess = req.getSession(false);
        if (sess == null || sess.getAttribute("bk_isInpatient") == null) return null;

        boolean isInpatient = Boolean.TRUE.equals(sess.getAttribute("bk_isInpatient"));

        JsonObject o = new JsonObject();
        o.addProperty("isInpatient", isInpatient);
        o.addProperty("notes", (String) sess.getAttribute("bk_notes"));
        Object petIdAttr = sess.getAttribute("bk_petId");
        o.addProperty("petId", petIdAttr != null ? String.valueOf(petIdAttr) : "");

        if (isInpatient) {
            o.addProperty("inpatientDate", (String) sess.getAttribute("bk_iDate"));
            o.addProperty("inpatientPeriod", (String) sess.getAttribute("bk_iPeriod"));
        } else {
            o.addProperty("payload", (String) sess.getAttribute("bk_payload"));
            o.addProperty("slotKey", (String) sess.getAttribute("bk_slotKey"));
        }

        return o.toString();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        Customer customer = requireLogin(req, resp);
        if (customer == null) return;

        try {
            boolean isInpatient = "true".equals(req.getParameter("isInpatient"));
            if (isInpatient) {
                handleStep1PostInpatient(req, resp, customer);
            } else {
                handleStep1PostNormal(req, resp, customer);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException(e);
        }
    }

    private void handleStep1PostInpatient(HttpServletRequest req, HttpServletResponse resp, Customer customer) throws Exception {
        Integer petId;
        try {
            petId = resolvePetSelection(req, customer);
        } catch (IllegalArgumentException e) {
            forwardStep1Error(req, resp, customer, e.getMessage());
            return;
        }
        if (petId == null) {
            forwardStep1Error(req, resp, customer, "Vui lòng chọn thú cưng hoặc nhập thông tin thú cưng mới.");
            return;
        }

        String iDate = req.getParameter("inpatientDate");
        String iPeriod = req.getParameter("inpatientPeriod");
        String notes = req.getParameter("notes");

        if (iDate == null || iDate.isBlank()) {
            forwardStep1Error(req, resp, customer, "Vui lòng chọn ngày nhập viện.");
            return;
        }
        if (iPeriod == null || iPeriod.isBlank()) {
            forwardStep1Error(req, resp, customer, "Vui lòng chọn buổi sáng hoặc chiều.");
            return;
        }

        long depositAmount = bookingSvc.computeDeposit(BigDecimal.valueOf(200000), true);

        HttpSession sess = req.getSession(true);
        sess.setAttribute("bk_isInpatient", true);
        sess.setAttribute("bk_iDate", iDate);
        sess.setAttribute("bk_iPeriod", iPeriod);
        sess.setAttribute("bk_notes", notes);
        sess.setAttribute("bk_total", BigDecimal.valueOf(depositAmount));
        sess.setAttribute("bk_deposit", depositAmount);
        sess.setAttribute("bk_petId", petId);

        req.setAttribute("isInpatient", true);
        req.setAttribute("inpatientDate", iDate);
        req.setAttribute("inpatientPeriod", "morning".equals(iPeriod) ? "Buổi sáng (08:00–12:00)" : "Buổi chiều (13:30–17:30)");
        req.setAttribute("notes", notes);
        req.setAttribute("totalPrice", BigDecimal.valueOf(depositAmount));
        req.setAttribute("depositAmount", depositAmount);
        req.setAttribute("navCategories", serviceDAO.findAllCategoriesWithServices());
        req.setAttribute("selectedPet", petDAO.findByPetId(petId));

        req.getRequestDispatcher("/WEB-INF/views/booking/confirm.jsp").forward(req, resp);
    }

    private void handleStep1PostNormal(HttpServletRequest req, HttpServletResponse resp, Customer customer) throws Exception {
        Integer petId;
        try {
            petId = resolvePetSelection(req, customer);
        } catch (IllegalArgumentException e) {
            forwardStep1Error(req, resp, customer, e.getMessage());
            return;
        }
        if (petId == null) {
            forwardStep1Error(req, resp, customer, "Vui lòng chọn thú cưng hoặc nhập thông tin thú cưng mới.");
            return;
        }

        String bookingPayload = req.getParameter("bookingPayload");
        String slotKey = req.getParameter("slotKey");
        String notes = req.getParameter("notes");

        if (bookingPayload == null || bookingPayload.isBlank()) {
            forwardStep1Error(req, resp, customer, "Vui lòng chọn dịch vụ.");
            return;
        }
        if (slotKey == null || slotKey.isBlank()) {
            forwardStep1Error(req, resp, customer, "Vui lòng chọn một khung giờ.");
            return;
        }

        BookingSelection selection = BookingSelection.parse(bookingPayload);
        int totalItems = selection.getServiceIds().size() + selection.getVaccineIds().size();
        if (totalItems < 1) {
            forwardStep1Error(req, resp, customer, "Vui lòng chọn ít nhất 1 dịch vụ hoặc vaccine.");
            return;
        }

        Map<Integer, Vaccine> vaccineById = new LinkedHashMap<>();
        for (Vaccine v : vaccineDAO.findAvailable()) vaccineById.put(v.getVaccineID(), v);

        List<Service> svcs = selection.getServiceIds().isEmpty()
                ? Collections.emptyList()
                : serviceDAO.findByIds(selection.getServiceIds());

        List<Vaccine> vaccines = new ArrayList<>();
        for (Integer vid : selection.getVaccineIds()) {
            Vaccine v = vaccineById.get(vid);
            if (v != null) vaccines.add(v);
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Service s : svcs) total = total.add(s.getPrice() != null ? s.getPrice() : BigDecimal.ZERO);
        for (Vaccine v : vaccines) total = total.add(v.getUnitPrice() != null ? v.getUnitPrice() : BigDecimal.ZERO);

        long depositAmount = bookingSvc.computeDeposit(total, false);

        HttpSession sess = req.getSession(true);
        sess.setAttribute("bk_payload", bookingPayload);
        sess.setAttribute("bk_slotKey", slotKey);
        sess.setAttribute("bk_isInpatient", false);
        sess.setAttribute("bk_notes", notes);
        sess.setAttribute("bk_total", total);
        sess.setAttribute("bk_deposit", depositAmount);
        sess.setAttribute("bk_petId", petId);

        req.setAttribute("services", svcs);
        req.setAttribute("vaccines", vaccines);
        req.setAttribute("slotKey", slotKey);
        req.setAttribute("isInpatient", false);
        req.setAttribute("notes", notes);
        req.setAttribute("totalPrice", total);
        req.setAttribute("depositAmount", depositAmount);
        req.setAttribute("navCategories", serviceDAO.findAllCategoriesWithServices());
        req.setAttribute("selectedPet", petDAO.findByPetId(petId));

        req.getRequestDispatcher("/WEB-INF/views/booking/confirm.jsp").forward(req, resp);
    }

    private Integer resolvePetSelection(HttpServletRequest req, Customer customer) throws Exception {
        String petIdParam = req.getParameter("petId");
        if (petIdParam != null && !petIdParam.isBlank()) {
            int pid;
            try {
                pid = Integer.parseInt(petIdParam.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Thú cưng không hợp lệ.");
            }
            Pet existing = petDAO.findByPetId(pid);
            if (existing == null || existing.getCustomerID() != customer.getCustomerID()) {
                throw new IllegalArgumentException("Thú cưng không hợp lệ.");
            }
            return pid;
        }

        String petName = req.getParameter("newPetName");
        if (petName == null || petName.isBlank()) {
            return null;
        }

        Pet pet = new Pet();
        pet.setCustomerID(customer.getCustomerID());
        pet.setName(petName.trim());

        String species = req.getParameter("newPetSpecies");
        pet.setSpeciesName(species != null && !species.isBlank() ? species.trim() : "Chưa rõ");

        String breed = req.getParameter("newPetBreed");
        pet.setBreedName(breed != null && !breed.isBlank() ? breed.trim() : "Chưa rõ");

        String gender = req.getParameter("newPetGender");
        pet.setGender(gender != null && !gender.isBlank() ? gender : "Unknown");

        String dobStr = req.getParameter("newPetDob");
        if (dobStr != null && !dobStr.isBlank()) {
            try {
                LocalDate dob = LocalDate.parse(dobStr.trim());
                if (!dob.isAfter(LocalDate.now())) pet.setDateOfBirth(dob);
            } catch (Exception ignored) {

            }
        }

        int newId = petDAO.insert(pet);
        return newId > 0 ? newId : null;
    }


    // Helpers
    private void forwardStep1Error(HttpServletRequest req, HttpServletResponse resp, Customer customer, String msg) throws Exception {
        req.setAttribute("error", msg);
        doGet(req, resp);
    }


    private Customer requireLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession sess = req.getSession(false);
        Customer c = sess != null ? (Customer) sess.getAttribute("customer") : null;
        if (c == null) resp.sendRedirect(req.getContextPath() + "/auth/login");
        return c;
    }

}
