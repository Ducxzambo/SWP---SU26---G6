package com.petclinic.customer.servlet.pet;

import com.petclinic.backend.dao.*;
import com.petclinic.backend.dto.PetTimelineEvent;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import com.petclinic.backend.model.*;
import com.petclinic.backend.service.PetService;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WebServlet(urlPatterns = {
    "/pets", "/pets/profile", "/pets/edit", "/pets/delete"
})
public class PetServlet extends HttpServlet {

    private final PetDAO petDAO         = new PetDAO();
    private final AppointmentDAO appointmentDAO = new AppointmentDAO();
    private final ServiceDAO serviceDAO     = new ServiceDAO();
    private final NotificationDAO notifDAO      = new NotificationDAO();
    private final VaccinationRecordDAO vaccinationRecordDAO = new VaccinationRecordDAO();
    private final MedicalRecordDAO medicalRecordDAO = new MedicalRecordDAO();

    private final PetService petSvc = new PetService();

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final Pattern FOLLOWUP_PATTERN = Pattern.compile("Tai kham:\\s*(\\d{4}-\\d{2}-\\d{2})");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Customer customer = requireLogin(req, resp);
        if (customer == null) return;
        try {
            switch (req.getServletPath()) {
                case "/pets":          handleList(req, resp, customer);    break;
                case "/pets/profile":  handleProfile(req, resp, customer); break;
                case "/pets/edit":     handleEditForm(req, resp, customer);break;
                default: resp.sendRedirect(req.getContextPath() + "/pets");
            }
        } catch (Exception e) { e.printStackTrace(); throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        Customer customer = requireLogin(req, resp);
        if (customer == null) return;
        try {
            switch (req.getServletPath()) {
                case "/pets/edit":   handleEditSave(req, resp, customer); break;
                case "/pets/delete": handleDelete(req, resp, customer);   break;
                default: resp.sendRedirect(req.getContextPath() + "/pets");
            }
        } catch (Exception e) { e.printStackTrace(); throw new ServletException(e); }
    }

    private void handleList(HttpServletRequest req, HttpServletResponse resp, Customer customer)
            throws Exception {
        List<Pet> pets = petDAO.findByCustomer(customer.getCustomerID());
        setCommonAttrs(req, customer);

        int totalVisits = 0;
        for (Pet p : pets) totalVisits += p.getDoneAppointments();

        req.setAttribute("pets", pets);
        req.setAttribute("totalPets", pets.size());
        req.setAttribute("totalVisits", totalVisits);
        req.getRequestDispatcher("/WEB-INF/views/customer/pets/list.jsp").forward(req, resp);
    }

    private void handleProfile(HttpServletRequest req, HttpServletResponse resp, Customer customer)
            throws Exception {
        int id = parseId(req.getParameter("id"));
        Pet pet = id > 0 ? petDAO.findById(id) : null;
        if (pet == null || pet.getCustomerID() != customer.getCustomerID()) {
            resp.sendError(404, "Không tìm thấy thú cưng."); return;
        }

        List<Appointment> appointments = appointmentDAO.findByPet(id);
        List<VaccinationRecord> vaccineRecords = vaccinationRecordDAO.findByPet(id);
        List<MedicalRecord> medicalRecords = medicalRecordDAO.findByPet(id);

        LocalDate today = LocalDate.now();
        Appointment firstDone = null, lastDone = null, nextUpcoming = null;
        int doneCount = 0, cancelledCount = 0, noShowCount = 0;

        List<PetTimelineEvent> timeline = new ArrayList<>();

        for (Appointment a : appointments) {
            if (a.getAppointmentDate() == null) continue;
            if ("Done".equals(a.getStatus())) {
                doneCount++;
                if (firstDone == null || a.getAppointmentDate().isBefore(firstDone.getAppointmentDate())) firstDone = a;
                if (lastDone == null || a.getAppointmentDate().isAfter(lastDone.getAppointmentDate())) lastDone = a;
                timeline.add(new PetTimelineEvent(a.getAppointmentDate(), PetTimelineEvent.Type.DONE,
                        a.getServiceName(), a.getAppointmentID()));
            } else if ("Cancelled".equals(a.getStatus())) {
                cancelledCount++;
            } else if ("NoShow".equals(a.getStatus())) {
                noShowCount++;
            } else if ("Confirmed".equals(a.getStatus())) {
                timeline.add(new PetTimelineEvent(a.getAppointmentDate(), PetTimelineEvent.Type.CONFIRMED,
                        a.getServiceName(), a.getAppointmentID()));
            }
            boolean active = "Pending".equals(a.getStatus()) || "Confirmed".equals(a.getStatus()) || "InProgress".equals(a.getStatus());
            if (active && !a.getAppointmentDate().isBefore(today)) {
                if (nextUpcoming == null || a.getAppointmentDate().isBefore(nextUpcoming.getAppointmentDate())) nextUpcoming = a;
            }
        }

        VaccinationRecord latestVaccine = null, nextDueVaccine = null;
        for (VaccinationRecord vr : vaccineRecords) {
            if (vr.getAdministeredDate() != null) {
                timeline.add(new PetTimelineEvent(vr.getAdministeredDate(), PetTimelineEvent.Type.VACCINE,
                        vr.getVaccineName(), vr.getAppointmentID()));
                if (latestVaccine == null || vr.getAdministeredDate().isAfter(latestVaccine.getAdministeredDate())) {
                    latestVaccine = vr;
                }
            }
            if (vr.getNextDueDate() != null && !vr.getNextDueDate().isBefore(today)
                    && (nextDueVaccine == null || vr.getNextDueDate().isBefore(nextDueVaccine.getNextDueDate()))) {
                nextDueVaccine = vr;
            }
        }

        for (MedicalRecord mr : medicalRecords) {
            if (mr.getTreatmentPlan() == null) continue;
            Matcher m = FOLLOWUP_PATTERN.matcher(mr.getTreatmentPlan());
            while (m.find()) {
                try {
                    LocalDate followUp = LocalDate.parse(m.group(1));
                    timeline.add(new PetTimelineEvent(followUp, PetTimelineEvent.Type.FOLLOWUP,
                            "Tái khám", mr.getAppointmentID()));
                } catch (Exception ignored) {

                }
            }
        }

        timeline.sort(java.util.Comparator.comparing(PetTimelineEvent::getDate));

        setCommonAttrs(req, customer);
        req.setAttribute("pet",            pet);
        req.setAttribute("doneCount",      doneCount);
        req.setAttribute("cancelledCount", cancelledCount);
        req.setAttribute("noShowCount",    noShowCount);
        req.setAttribute("vaccineCount",   vaccineRecords.size());
        req.setAttribute("firstDone",      firstDone);
        req.setAttribute("lastDone",       lastDone);
        req.setAttribute("nextUpcoming",   nextUpcoming);
        req.setAttribute("latestVaccine",  latestVaccine);
        req.setAttribute("nextDueVaccine", nextDueVaccine);
        req.setAttribute("timeline",       timeline);
        req.getRequestDispatcher("/WEB-INF/views/customer/pets/profile.jsp").forward(req, resp);
    }

    private void handleEditForm(HttpServletRequest req, HttpServletResponse resp, Customer customer)
            throws Exception {
        int id = parseId(req.getParameter("id"));
        Pet pet = id > 0 ? petDAO.findById(id) : null;
        if (pet == null || pet.getCustomerID() != customer.getCustomerID()) {
            resp.sendError(404); return;
        }
        setCommonAttrs(req, customer);
        req.setAttribute("pet",      pet);
        req.setAttribute("editMode", true);
        req.getRequestDispatcher("/WEB-INF/views/customer/pets/form.jsp").forward(req, resp);
    }

    private void handleEditSave(HttpServletRequest req, HttpServletResponse resp, Customer customer)
            throws Exception {
        int id = parseId(req.getParameter("petId"));
        Pet existing = id > 0 ? petDAO.findById(id) : null;
        if (existing == null || existing.getCustomerID() != customer.getCustomerID()) {
            resp.sendError(403); return;
        }
        applyEditableFields(existing, req);
        String error = validate(existing);
        if (error != null) {
            setCommonAttrs(req, customer);
            req.setAttribute("error",    error);
            req.setAttribute("pet",      existing);
            req.setAttribute("editMode", true);
            req.getRequestDispatcher("/WEB-INF/views/customer/pets/form.jsp").forward(req, resp);
            return;
        }
        petDAO.update(existing);
        req.getSession().setAttribute("flashSuccess", "Đã cập nhật thông tin thú cưng.");
        resp.sendRedirect(req.getContextPath() + "/pets/profile?id=" + id);
    }

    private void handleDelete(HttpServletRequest req, HttpServletResponse resp, Customer customer)
            throws Exception {
        int id = parseId(req.getParameter("petId"));
        Pet pet = id > 0 ? petDAO.findById(id) : null;
        if (pet == null || pet.getCustomerID() != customer.getCustomerID()) {
            resp.sendError(403); return;
        }
        petDAO.softDelete(id, customer.getCustomerID());
        req.getSession().setAttribute("flashSuccess", "Đã xoá thú cưng khỏi danh sách.");
        resp.sendRedirect(req.getContextPath() + "/pets");
    }

    private void applyEditableFields(Pet pet, HttpServletRequest req) {
        pet.setName(trim(req.getParameter("name")));

        String species = trim(req.getParameter("speciesName"));
        if (!species.isEmpty()) pet.setSpeciesName(species);

        pet.setBreedName(trim(req.getParameter("breedName")));

        String gender = req.getParameter("gender");
        if (gender != null && !gender.isBlank()) pet.setGender(gender);

        String dob = trim(req.getParameter("dateOfBirth"));
        if (!dob.isEmpty()) {
            try { pet.setDateOfBirth(LocalDate.parse(dob, ISO)); } catch (Exception ignored) {}
        } else {
            pet.setDateOfBirth(null);
        }
    }

    private String validate(Pet pet) {
        if (pet.getName() == null || pet.getName().isBlank())
            return "Tên thú cưng không được để trống.";
        if (pet.getDateOfBirth() != null && pet.getDateOfBirth().isAfter(LocalDate.now()))
            return "Ngày sinh không thể là ngày trong tương lai.";
        return null;
    }

    private void setCommonAttrs(HttpServletRequest req, Customer customer) throws Exception {
        req.setAttribute("navCategories",
                serviceDAO.findAllCategoriesWithServices());
        req.setAttribute("unreadCount",
                notifDAO.countUnread(customer.getCustomerID()));
    }

    private Customer requireLogin(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession sess = req.getSession(false);
        Customer c = sess != null ? (Customer) sess.getAttribute("customer") : null;
        if (c == null) {
            req.getSession(true).setAttribute("redirectAfterLogin",
                    req.getRequestURI() + (req.getQueryString() != null ? "?" + req.getQueryString() : ""));
            resp.sendRedirect(req.getContextPath() + "/auth/login");
        }
        return c;
    }

    private int parseId(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return -1; }
    }
    private String trim(String s) { return s != null ? s.trim() : ""; }
}
