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
    //  EDIT SAVE — CHỈ sửa được name + dateOfBirth.
    // ══════════════════════════════════════════════════════════════════════════
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
    private void applyEditableFields(Pet pet, HttpServletRequest req) {
        pet.setName(trim(req.getParameter("name")));
        String dob = trim(req.getParameter("dateOfBirth"));
        if (!dob.isEmpty()) {
            try { pet.setDateOfBirth(LocalDate.parse(dob, ISO)); } catch (Exception ignored) {}
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
