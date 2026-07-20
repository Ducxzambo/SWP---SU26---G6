package com.petclinic.servlet.pet;

import com.petclinic.dao.*;
import com.petclinic.model.*;

import com.petclinic.service.PetService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * URL map:
 *   GET  /pets                  → list of customer's pets
 *   GET  /pets/profile?id=      → pet profile + medical history + vaccines
 *   GET  /pets/edit?id=         → edit pet form
 *   POST /pets/edit             → save edits
 *   POST /pets/delete           → soft delete pet
 */
@WebServlet(urlPatterns = {
    "/pets", "/pets/profile", "/pets/edit", "/pets/delete"
})
public class PetServlet extends HttpServlet {

    private final PetDAO         petDAO         = new PetDAO();
    private final AppointmentDAO appointmentDAO = new AppointmentDAO();
    private final MedicalRecordDAO mrDAO        = new MedicalRecordDAO();
    private final ServiceDAO     serviceDAO     = new ServiceDAO();
    private final NotificationDAO notifDAO      = new NotificationDAO();

    private final PetService petSvc = new  PetService();

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ── GET ───────────────────────────────────────────────────────────────────
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

    // ── POST ──────────────────────────────────────────────────────────────────
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

    // ══════════════════════════════════════════════════════════════════════════
    //  LIST
    // ══════════════════════════════════════════════════════════════════════════
    private void handleList(HttpServletRequest req, HttpServletResponse resp, Customer customer)
            throws Exception {
        List<Pet> pets = petDAO.findByCustomer(customer.getCustomerID());
        setCommonAttrs(req, customer);
        req.setAttribute("pets", pets);
        req.getRequestDispatcher("/WEB-INF/views/customer/pets/list.jsp").forward(req, resp);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PROFILE
    // ══════════════════════════════════════════════════════════════════════════
    private void handleProfile(HttpServletRequest req, HttpServletResponse resp, Customer customer)
            throws Exception {
        int id = parseId(req.getParameter("id"));
        Pet pet = id > 0 ? petDAO.findById(id) : null;
        if (pet == null || pet.getCustomerID() != customer.getCustomerID()) {
            resp.sendError(404, "Không tìm thấy thú cưng."); return;
        }
        List<Appointment> appointments = appointmentDAO.findByPet(id);

        Map<Integer, MedicalRecord> medicalMap = petSvc.getMedicalRecordsByPet(id);
        Map<Integer, VaccinationRecord> vaccineMap = petSvc.getVaccinationRecordsByPet(id);
        Map<Integer, GroomingRecord> groomingMap = petSvc.getGroomingRecordsByPet(id);

        setCommonAttrs(req, customer);
        req.setAttribute("pet",          pet);
        req.setAttribute("appointments", appointments);
        req.setAttribute("medicalMap",   medicalMap);
        req.setAttribute("vaccineMap",   vaccineMap);
        req.setAttribute("groomingMap",  groomingMap);
        req.getRequestDispatcher("/WEB-INF/views/customer/pets/profile.jsp").forward(req, resp);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  NEW FORM
    // ══════════════════════════════════════════════════════════════════════════
    private void handleNewForm(HttpServletRequest req, HttpServletResponse resp, Customer customer)
            throws Exception {
        setCommonAttrs(req, customer);
        req.getRequestDispatcher("/WEB-INF/views/customer/pets/form.jsp").forward(req, resp);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  NEW SAVE
    // ══════════════════════════════════════════════════════════════════════════
//    private void handleNewSave(HttpServletRequest req, HttpServletResponse resp, Customer customer)
//            throws Exception {
//        Pet pet = bindPetFromRequest(req, customer.getCustomerID());
//        String error = validate(pet);
//        if (error != null) {
//            setCommonAttrs(req, customer);
//            req.setAttribute("error", error);
//            req.setAttribute("pet", pet);
//            req.getRequestDispatcher("/WEB-INF/views/customer/pets/form.jsp").forward(req, resp);
//            return;
//        }
//        petDAO.insert(pet);
//        req.getSession().setAttribute("flashSuccess", "Thêm thú cưng thành công!");
//        resp.sendRedirect(req.getContextPath() + "/pets");
//    }

    // ══════════════════════════════════════════════════════════════════════════
    //  EDIT FORM
    // ══════════════════════════════════════════════════════════════════════════
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

    // ══════════════════════════════════════════════════════════════════════════
    //  EDIT SAVE
    // ══════════════════════════════════════════════════════════════════════════
    private void handleEditSave(HttpServletRequest req, HttpServletResponse resp, Customer customer)
            throws Exception {
        int id = parseId(req.getParameter("petId"));
        Pet existing = id > 0 ? petDAO.findById(id) : null;
        if (existing == null || existing.getCustomerID() != customer.getCustomerID()) {
            resp.sendError(403); return;
        }
        Pet pet = bindPetFromRequest(req, customer.getCustomerID());
        pet.setPetID(id);
        String error = validate(pet);
        if (error != null) {
            setCommonAttrs(req, customer);
            req.setAttribute("error",    error);
            req.setAttribute("pet",      pet);
            req.setAttribute("editMode", true);
            req.getRequestDispatcher("/WEB-INF/views/customer/pets/form.jsp").forward(req, resp);
            return;
        }
        petDAO.update(pet);
        req.getSession().setAttribute("flashSuccess", "Đã cập nhật thông tin thú cưng.");
        resp.sendRedirect(req.getContextPath() + "/pets/profile?id=" + id);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DELETE
    // ══════════════════════════════════════════════════════════════════════════
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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Pet bindPetFromRequest(HttpServletRequest req, int customerId) {
        Pet pet = new Pet();
        pet.setCustomerID(customerId);
        pet.setName(trim(req.getParameter("name")));
        pet.setSpeciesName(trim(req.getParameter("speciesName")));
        pet.setBreedName(trim(req.getParameter("breedName")));
        pet.setGender(trim(req.getParameter("gender")));
        String dob = trim(req.getParameter("dateOfBirth"));
        if (!dob.isEmpty()) {
            try { pet.setDateOfBirth(LocalDate.parse(dob, ISO)); } catch (Exception ignored) {}
        }
        String w = trim(req.getParameter("weight"));
        if (!w.isEmpty()) {
            try { pet.setWeight(new BigDecimal(w)); } catch (Exception ignored) {}
        }
        return pet;
    }

    private String validate(Pet pet) {
        if (pet.getName() == null || pet.getName().isBlank())
            return "Tên thú cưng không được để trống.";
        if (pet.getSpeciesName() == null || pet.getSpeciesName().isBlank())
            return "Vui lòng chọn loài thú cưng.";
        if (pet.getBreedName() == null || pet.getBreedName().isBlank())
            return "Vui lòng nhập giống.";
        if (pet.getDateOfBirth() != null && pet.getDateOfBirth().isAfter(LocalDate.now()))
            return "Ngày sinh không thể là ngày trong tương lai.";
        if (pet.getWeight() != null && pet.getWeight().doubleValue() <= 0)
            return "Cân nặng phải lớn hơn 0.";
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
