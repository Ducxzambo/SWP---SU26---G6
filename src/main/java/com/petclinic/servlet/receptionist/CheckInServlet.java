package com.petclinic.servlet.receptionist;

import com.petclinic.dao.AppointmentDAO;
import com.petclinic.dao.StaffDAO;
import com.petclinic.model.*;
import com.petclinic.service.ExaminationService;
import com.petclinic.service.ExaminationService.CheckInResult;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * BP-02 Step 1 — Receptionist check-in.
 *
 * GET  /receptionist/checkin                → danh sách Confirmed, lọc ngày + ca
 * POST /receptionist/checkin                → check-in bình thường (Confirmed → Arrived)
 * POST /receptionist/checkin?action=walkinLookup → Bước 1 walk-in: tra SĐT
 * POST /receptionist/checkin?action=walkinSubmit → Bước 2 walk-in: tạo lịch (nhiều service, mỗi service 1 staff riêng)
 * POST /receptionist/checkin?action=assignStaff  → gán/đổi staff cho 1 dòng dịch vụ cụ thể
 */
@WebServlet("/receptionist/checkin")
public class CheckInServlet extends HttpServlet {

    private final ExaminationService examinationService = new ExaminationService();
    private final StaffDAO           staffDAO           = new StaffDAO();

    // ── GET ───────────────────────────────────────────────────────────────────
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        Staff staff = (session != null) ? (Staff) session.getAttribute("staff") : null;
        if (staff == null || !"Receptionist".equals(staff.getRoleName())) {
            resp.sendRedirect(req.getContextPath() + "/auth/staff/login");
            return;
        }

        LocalDate filterDate = parseDate(req.getParameter("date"));
        Integer shiftFilter = parseShift(req.getParameter("shift"));

        try {
            String keyword = req.getParameter("q");
            List<Appointment> appointments = (keyword != null && !keyword.isBlank())
                    ? examinationService.searchForCheckIn(keyword, filterDate, null)
                    : examinationService.getConfirmedByDate(filterDate, shiftFilter, null);

            loadFormOptions(req);

            boolean isToday = filterDate.equals(LocalDate.now());
            int currentSlotCount  = isToday ? examinationService.getCurrentShiftCount() : 0;
            boolean currentSlotFull = currentSlotCount >= AppointmentDAO.MAX_PER_SHIFT;
            int currentShift = AppointmentDAO.shiftOf(LocalTime.now());
            String currentShiftLabel = (currentShift > 0)
                    ? AppointmentDAO.shiftLabel(currentShift) : "Ngoài giờ làm việc";

            req.setAttribute("appointments",      appointments);
            req.setAttribute("keyword",           keyword);
            req.setAttribute("filterDate",        filterDate.toString());
            req.setAttribute("shiftFilter",       shiftFilter != null ? shiftFilter.toString() : "");
            req.setAttribute("isToday",           isToday);
            req.setAttribute("currentSlotCount",  currentSlotCount);
            req.setAttribute("currentSlotFull",   currentSlotFull);
            req.setAttribute("currentShiftLabel", currentShiftLabel);

            req.getRequestDispatcher("/WEB-INF/views/receptionist/checkin.jsp").forward(req, resp);

        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Lỗi hệ thống khi tải danh sách lịch hẹn.");
            req.getRequestDispatcher("/WEB-INF/views/receptionist/checkin.jsp").forward(req, resp);
        }
    }

    // ── POST ──────────────────────────────────────────────────────────────────
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        HttpSession session = req.getSession(false);
        Staff staff = (session != null) ? (Staff) session.getAttribute("staff") : null;
        if (staff == null || !"Receptionist".equals(staff.getRoleName())) {
            resp.sendRedirect(req.getContextPath() + "/auth/staff/login");
            return;
        }

        String action = req.getParameter("action");
        if ("walkinLookup".equals(action)) { handleWalkInLookup(req, resp); return; }
        if ("walkinSubmit".equals(action)) { handleWalkInSubmit(req, resp, session); return; }
        if ("assignStaff".equals(action))  { handleAssignStaff(req, resp, session); return; }

        // ── Check-in bình thường ─────────────────────────────────────────────
        String appointmentIdStr = req.getParameter("appointmentID");
        if (appointmentIdStr == null || appointmentIdStr.isBlank()) {
            session.setAttribute("flashError", "Thiếu mã lịch hẹn.");
            resp.sendRedirect(req.getContextPath() + "/receptionist/checkin");
            return;
        }

        try {
            int appointmentID = Integer.parseInt(appointmentIdStr);
            CheckInResult result = examinationService.checkIn(appointmentID);
            switch (result) {
                case SUCCESS ->
                        session.setAttribute("flashSuccess", "Check-in thành công!");
                case ALREADY_CHECKED_IN ->
                        session.setAttribute("flashWarning", "Thú cưng này đã được check-in trước đó.");
                case WRONG_STATUS ->
                        session.setAttribute("flashWarning", "Lịch hẹn không ở trạng thái Confirmed, không thể check-in.");
                default ->
                        session.setAttribute("flashError", "Không tìm thấy lịch hẹn.");
            }
        } catch (NumberFormatException e) {
            session.setAttribute("flashError", "ID lịch hẹn không hợp lệ.");
        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("flashError", "Lỗi hệ thống, vui lòng thử lại.");
        }
        resp.sendRedirect(req.getContextPath() + "/receptionist/checkin");
    }

    // ── Gán staff cho 1 dòng dịch vụ (bảng check-in, dùng dropdown inline) ─────
    private void handleAssignStaff(HttpServletRequest req, HttpServletResponse resp, HttpSession session)
            throws IOException {
        String apptServiceIdStr = req.getParameter("appointmentServiceID");
        String staffIdStr       = req.getParameter("staffID");

        if (isBlank(apptServiceIdStr) || isBlank(staffIdStr)) {
            session.setAttribute("flashError", "Thiếu thông tin gán nhân viên.");
            resp.sendRedirect(req.getContextPath() + "/receptionist/checkin");
            return;
        }
        try {
            examinationService.assignStaffToServiceLine(
                    Integer.parseInt(apptServiceIdStr), Integer.parseInt(staffIdStr));
            session.setAttribute("flashSuccess", "Đã gán nhân viên phụ trách.");
        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("flashError", "Lỗi khi gán nhân viên: " + e.getMessage());
        }
        resp.sendRedirect(req.getContextPath() + "/receptionist/checkin");
    }

    // ── WALK-IN Bước 1: tra số điện thoại ──────────────────────────────────────
    private void handleWalkInLookup(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String phone = req.getParameter("phone");
        if (phone == null || phone.isBlank()) {
            reloadCheckinWithWalkInError(req, resp, "Vui lòng nhập số điện thoại.", null);
            return;
        }
        phone = phone.trim();
        try {
            Customer customer = examinationService.findCustomerByPhone(phone);
            if (customer != null) {
                List<Pet> pets = examinationService.getPetsByCustomer(customer.getCustomerID());
                reloadCheckinWithWalkInStep2(req, resp, phone, customer, pets);
            } else {
                reloadCheckinWithWalkInStep2(req, resp, phone, null, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            reloadCheckinWithWalkInError(req, resp, "Lỗi hệ thống khi tra cứu số điện thoại.", phone);
        }
    }

    // ── WALK-IN Bước 2: tạo lịch nhiều dịch vụ, mỗi dịch vụ 1 staff riêng ───────
    private void handleWalkInSubmit(HttpServletRequest req, HttpServletResponse resp,
                                    HttpSession session) throws ServletException, IOException {
        String phone         = req.getParameter("phone");
        String customerIdStr = req.getParameter("customerID");
        String petIdStr      = req.getParameter("petID");
        String fullName      = req.getParameter("fullName");
        String petName       = req.getParameter("petName");
        String species       = req.getParameter("species");
        String breed         = req.getParameter("breed");

        String[] serviceIdArr = req.getParameterValues("serviceID[]");
        if (serviceIdArr == null || serviceIdArr.length == 0) {
            reloadCheckinWithWalkInError(req, resp, "Vui lòng chọn ít nhất 1 dịch vụ.", phone);
            return;
        }

        try {
            List<Integer> serviceIDs = new ArrayList<>();
            List<Integer> staffIDs   = new ArrayList<>();
            for (String sidStr : serviceIdArr) {
                int sid = Integer.parseInt(sidStr);
                serviceIDs.add(sid);
                String staffStr = req.getParameter("staffFor_" + sid);
                staffIDs.add((staffStr != null && !staffStr.isBlank()) ? Integer.parseInt(staffStr) : null);
            }

            boolean hasCustomerID = !isBlank(customerIdStr);
            boolean hasPetID      = !isBlank(petIdStr);
            int apptID;

            if (hasCustomerID && hasPetID) {
                apptID = examinationService.createWalkInExisting(
                        Integer.parseInt(customerIdStr), Integer.parseInt(petIdStr),
                        serviceIDs, staffIDs);

            } else if (hasCustomerID) {
                if (isBlank(petName)) {
                    reloadCheckinWithWalkInError(req, resp, "Vui lòng nhập tên thú cưng mới.", phone);
                    return;
                }
                apptID = examinationService.createWalkInWithNewPet(
                        Integer.parseInt(customerIdStr), petName.trim(),
                        blankToDefault(species, "Chưa rõ"), blankToDefault(breed, "Chưa rõ"),
                        serviceIDs, staffIDs);

            } else {
                if (isBlank(fullName) || isBlank(petName)) {
                    reloadCheckinWithWalkInError(req, resp,
                            "Vui lòng nhập đầy đủ Họ tên và Tên thú cưng.", phone);
                    return;
                }
                apptID = examinationService.createWalkInWithNewCustomer(
                        fullName.trim(), phone.trim(), petName.trim(),
                        blankToDefault(species, "Chưa rõ"), blankToDefault(breed, "Chưa rõ"),
                        serviceIDs, staffIDs);
            }

            if (apptID == -1) {
                session.setAttribute("flashError",
                        "Ca hiện tại đã đủ 10 thú cưng, không thể nhận thêm khách.");
            } else {
                session.setAttribute("flashSuccess",
                        "Walk-in thành công! Lịch khám #" + apptID + " đã tạo.");
            }
            resp.sendRedirect(req.getContextPath() + "/receptionist/checkin");

        } catch (NumberFormatException e) {
            reloadCheckinWithWalkInError(req, resp, "Dữ liệu không hợp lệ.", phone);
        } catch (Exception e) {
            e.printStackTrace();
            reloadCheckinWithWalkInError(req, resp, "Lỗi hệ thống: " + e.getMessage(), phone);
        }
    }

    // ── Helpers: reload trang checkin kèm trạng thái modal walk-in ─────────────

    private void reloadCheckinWithWalkInStep2(HttpServletRequest req, HttpServletResponse resp,
                                              String phone, Customer customer, List<Pet> pets)
            throws ServletException, IOException {
        loadCommonCheckinAttributes(req);
        req.setAttribute("walkInStep",     2);
        req.setAttribute("walkInPhone",    phone);
        req.setAttribute("walkInCustomer", customer);
        req.setAttribute("walkInPets",     pets);
        req.getRequestDispatcher("/WEB-INF/views/receptionist/checkin.jsp").forward(req, resp);
    }

    private void reloadCheckinWithWalkInError(HttpServletRequest req, HttpServletResponse resp,
                                              String error, String phone)
            throws ServletException, IOException {
        loadCommonCheckinAttributes(req);
        req.setAttribute("walkInError", error);
        req.setAttribute("walkInStep",  phone != null ? 2 : 1);
        req.setAttribute("walkInPhone", phone);
        req.getRequestDispatcher("/WEB-INF/views/receptionist/checkin.jsp").forward(req, resp);
    }

    private void loadCommonCheckinAttributes(HttpServletRequest req) {
        try {
            LocalDate filterDate = parseDate(req.getParameter("date"));
            Integer shiftFilter  = parseShift(req.getParameter("shift"));

            List<Appointment> appointments = examinationService.getConfirmedByDate(filterDate, shiftFilter, null);
            loadFormOptions(req);

            boolean isToday = filterDate.equals(LocalDate.now());
            int currentSlotCount = isToday ? examinationService.getCurrentShiftCount() : 0;
            int currentShift = AppointmentDAO.shiftOf(LocalTime.now());

            req.setAttribute("appointments",      appointments);
            req.setAttribute("filterDate",        filterDate.toString());
            req.setAttribute("shiftFilter",       shiftFilter != null ? shiftFilter.toString() : "");
            req.setAttribute("isToday",           isToday);
            req.setAttribute("currentSlotCount",  currentSlotCount);
            req.setAttribute("currentSlotFull",   currentSlotCount >= AppointmentDAO.MAX_PER_SHIFT);
            req.setAttribute("currentShiftLabel", currentShift > 0
                    ? AppointmentDAO.shiftLabel(currentShift) : "Ngoài giờ làm việc");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Load danh sách vet, groomer, service, vetWorkload — dùng chung cho GET và mọi reload. */
    private void loadFormOptions(HttpServletRequest req) {
        try {
            List<Staff> vets     = staffDAO.findAllVets();
            List<Staff> groomers = staffDAO.findAllGroomers();
            List<com.petclinic.model.Service> services = examinationService.getAllActiveServices();

            req.setAttribute("vets",        vets);
            req.setAttribute("groomers",    groomers);
            req.setAttribute("services",    services);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private LocalDate parseDate(String p) {
        if (p == null || p.isBlank()) return LocalDate.now();
        try { return LocalDate.parse(p); } catch (DateTimeParseException e) { return LocalDate.now(); }
    }

    private Integer parseShift(String p) {
        if (p == null || p.isBlank()) return null;
        try { return Integer.parseInt(p); } catch (NumberFormatException e) { return null; }
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }
    private String blankToDefault(String s, String def) { return isBlank(s) ? def : s.trim(); }
}