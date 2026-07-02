package com.petclinic.servlet.vet;

import com.petclinic.dao.AppointmentDAO;
import com.petclinic.model.*;
import com.petclinic.service.ExaminationService;
import com.petclinic.service.ExaminationService.SaveRecordResult;
import com.petclinic.service.ExaminationService.StartExamResult;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@WebServlet("/vet/examination")
public class ExaminationServlet extends HttpServlet {

    private final ExaminationService examinationService = new ExaminationService();

    // ── GET ───────────────────────────────────────────────────────────────────
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Staff vet = getAuthenticatedVet(req, resp);
        if (vet == null) return;

        String action = req.getParameter("action");
        if (action == null) action = "queue";

        switch (action) {
            case "queue"  -> showQueue(req, resp, vet);
            case "start"  -> startExam(req, resp, vet);
            case "form"   -> showExamForm(req, resp, vet);
            case "view"   -> viewRecord(req, resp, vet);
            default       -> resp.sendRedirect(req.getContextPath() + "/vet/examination");
        }
    }

    // ── POST ──────────────────────────────────────────────────────────────────
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        Staff vet = getAuthenticatedVet(req, resp);
        if (vet == null) return;

        String apptIdStr = req.getParameter("appointmentID");
        if (apptIdStr == null || apptIdStr.isBlank()) {
            resp.sendRedirect(req.getContextPath() + "/vet/examination");
            return;
        }

        int appointmentID;
        try { appointmentID = Integer.parseInt(apptIdStr); }
        catch (NumberFormatException e) {
            resp.sendRedirect(req.getContextPath() + "/vet/examination");
            return;
        }

        try {
            Appointment appt = examinationService.getAppointment(appointmentID);
            if (appt == null) {
                forwardFormWithError(req, resp, appointmentID, vet.getStaffID(),
                        "Không tìm thấy lịch hẹn.");
                return;
            }

            // Build MedicalRecord
            MedicalRecord record = new MedicalRecord();
            record.setAppointmentID(appointmentID);
            record.setPetID(appt.getPetID());
            record.setStaffID(vet.getStaffID());

            String weightStr = req.getParameter("weight");
            String tempStr   = req.getParameter("temperature");
            if (weightStr != null && !weightStr.isBlank())
                record.setWeight(new BigDecimal(weightStr));
            if (tempStr != null && !tempStr.isBlank())
                record.setTemperature(new BigDecimal(tempStr));

            record.setSymptoms(req.getParameter("symptoms"));
            record.setDiagnosis(buildDiagnosis(req));
            record.setTreatmentPlan(buildTreatmentPlan(req));

            List<PrescriptionItem> items = parsePrescriptionItems(req);
            String followUpDate = req.getParameter("followUpDate");

            SaveRecordResult result = examinationService.saveMedicalRecord(record, items, followUpDate);
            switch (result) {
                case SUCCESS -> {
                    req.getSession().setAttribute("flashSuccess",
                            "Bệnh án đã lưu thành công. Hóa đơn đang được tạo...");
                    triggerInvoice(appointmentID);
                    resp.sendRedirect(req.getContextPath() + "/vet/examination");
                }
                case INSUFFICIENT_STOCK ->
                        forwardFormWithError(req, resp, appointmentID, vet.getStaffID(),
                                "Thuốc không đủ tồn kho. Vui lòng kiểm tra lại đơn thuốc.");
                case RECORD_ALREADY_EXISTS ->
                        forwardFormWithError(req, resp, appointmentID, vet.getStaffID(),
                                "Bệnh án cho lịch khám này đã tồn tại.");
                case WRONG_STATUS ->
                        forwardFormWithError(req, resp, appointmentID, vet.getStaffID(),
                                "Lịch hẹn không ở trạng thái InProgress.");
                default ->
                        forwardFormWithError(req, resp, appointmentID, vet.getStaffID(),
                                "Lỗi hệ thống, vui lòng thử lại.");
            }
        } catch (NumberFormatException e) {
            forwardFormWithError(req, resp, appointmentID, vet.getStaffID(),
                    "Dữ liệu nhập không hợp lệ (cân nặng, nhiệt độ, số lượng thuốc).");
        } catch (Exception e) {
            e.printStackTrace();
            forwardFormWithError(req, resp, appointmentID, vet.getStaffID(),
                    "Lỗi hệ thống: " + e.getMessage());
        }
    }

    // ── Action handlers ───────────────────────────────────────────────────────

    private void showQueue(HttpServletRequest req, HttpServletResponse resp, Staff vet)
            throws ServletException, IOException {

        // Date filter
        LocalDate filterDate = LocalDate.now();
        String dateParam = req.getParameter("date");
        if (dateParam != null && !dateParam.isBlank()) {
            try { filterDate = LocalDate.parse(dateParam); } catch (DateTimeParseException ignored) {}
        }

        // Shift filter
        String shiftParam = req.getParameter("shift");
        Integer shiftFilter = null;
        if (shiftParam != null && !shiftParam.isBlank()) {
            try { shiftFilter = Integer.parseInt(shiftParam); } catch (NumberFormatException ignored) {}
        }

        try {
            List<Appointment> queue = examinationService.getVetQueue(vet.getStaffID(), filterDate);
            List<Appointment> completed = examinationService.getVetCompletedToday(vet.getStaffID(), filterDate);

            // Apply shift filter in memory (cho cả 2 danh sách)
            if (shiftFilter != null) {
                final int sf = shiftFilter;
                queue = queue.stream()
                        .filter(a -> a.getSlotShift() != null && a.getSlotShift() == sf)
                        .collect(Collectors.toList());
                completed = completed.stream()
                        .filter(a -> a.getSlotShift() != null && a.getSlotShift() == sf)
                        .collect(Collectors.toList());
            }

            req.setAttribute("queue",       queue);
            req.setAttribute("completed",   completed);
            req.setAttribute("filterDate",  filterDate.toString());
            req.setAttribute("isToday",     filterDate.equals(LocalDate.now()));
            req.setAttribute("shiftFilter", shiftParam != null ? shiftParam : "");
            req.getRequestDispatcher("/WEB-INF/views/vet/examination.jsp").forward(req, resp);

        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Không tải được danh sách bệnh nhân.");
            req.getRequestDispatcher("/WEB-INF/views/vet/examination.jsp").forward(req, resp);
        }
    }

    private void startExam(HttpServletRequest req, HttpServletResponse resp, Staff vet)
            throws ServletException, IOException {
        String idStr = req.getParameter("appointmentID");
        if (idStr == null) { resp.sendRedirect(req.getContextPath() + "/vet/examination"); return; }
        try {
            int apptID = Integer.parseInt(idStr);
            StartExamResult result = examinationService.startExamination(apptID, vet.getStaffID());
            if (result == StartExamResult.SUCCESS) {
                resp.sendRedirect(req.getContextPath()
                        + "/vet/examination?action=form&appointmentID=" + apptID);
            } else {
                req.getSession().setAttribute("flashWarning",
                        "Không thể bắt đầu khám: trạng thái lịch hẹn không hợp lệ.");
                resp.sendRedirect(req.getContextPath() + "/vet/examination");
            }
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect(req.getContextPath() + "/vet/examination");
        }
    }

    private void showExamForm(HttpServletRequest req, HttpServletResponse resp, Staff vet)
            throws ServletException, IOException {
        String idStr = req.getParameter("appointmentID");
        if (idStr == null) { resp.sendRedirect(req.getContextPath() + "/vet/examination"); return; }
        try {
            int apptID = Integer.parseInt(idStr);
            Appointment appt = examinationService.getAppointment(apptID);
            if (appt == null) { resp.sendRedirect(req.getContextPath() + "/vet/examination"); return; }

            List<MedicalRecord> history = examinationService.getPetMedicalHistory(appt.getPetID());
            List<Medicine>      medicines = examinationService.getMedicinesInStock();
            List<Service>       labTests  = examinationService.getLabTests();
            List<Service>       treatmentPlans = examinationService.getTreatmentPlans();

            req.setAttribute("appointment",    appt);
            req.setAttribute("history",        history);
            req.setAttribute("medicines",      medicines);
            req.setAttribute("labTests",       labTests);
            req.setAttribute("treatmentPlans", treatmentPlans);
            req.getRequestDispatcher("/WEB-INF/views/vet/examination-detail.jsp").forward(req, resp);

        } catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect(req.getContextPath() + "/vet/examination");
        }
    }

    private void viewRecord(HttpServletRequest req, HttpServletResponse resp, Staff vet)
            throws ServletException, IOException {
        String idStr = req.getParameter("recordID");
        if (idStr == null) { resp.sendRedirect(req.getContextPath() + "/vet/examination"); return; }
        try {
            int recordID = Integer.parseInt(idStr);
            MedicalRecord rec = examinationService.getMedicalRecord(recordID);
            if (rec == null) { resp.sendRedirect(req.getContextPath() + "/vet/examination"); return; }
            req.setAttribute("record", rec);
            req.getRequestDispatcher("/WEB-INF/views/vet/examination-detail.jsp").forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect(req.getContextPath() + "/vet/examination");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Build diagnosis string from checklist + notes.
     * Form fields: labTestID[] (ServiceID), labTestNote_<id>
     */
    private String buildDiagnosis(HttpServletRequest req) {
        String[] ids = req.getParameterValues("labTestID[]");
        if (ids == null || ids.length == 0) return req.getParameter("symptoms");

        StringBuilder sb = new StringBuilder();
        for (String id : ids) {
            String name = req.getParameter("labTestName_" + id);
            String note = req.getParameter("labTestNote_" + id);
            if (name != null) {
                sb.append("[").append(name).append("]");
                if (note != null && !note.isBlank()) sb.append(": ").append(note.trim());
                sb.append("\n");
            }
        }
        return sb.toString().trim();
    }

    /**
     * Build treatment plan string from checklist + notes.
     * Form fields: treatmentID[] (ServiceID), treatmentNote_<id>
     */
    private String buildTreatmentPlan(HttpServletRequest req) {
        String[] ids = req.getParameterValues("treatmentID[]");
        if (ids == null || ids.length == 0) return "";

        StringBuilder sb = new StringBuilder();
        for (String id : ids) {
            String name = req.getParameter("treatmentName_" + id);
            String note = req.getParameter("treatmentNote_" + id);
            if (name != null) {
                sb.append("[").append(name).append("]");
                if (note != null && !note.isBlank()) sb.append(": ").append(note.trim());
                sb.append("\n");
            }
        }
        return sb.toString().trim();
    }

    private List<PrescriptionItem> parsePrescriptionItems(HttpServletRequest req) {
        String[] medicineIDs = req.getParameterValues("medicineID[]");
        String[] dosages     = req.getParameterValues("dosage[]");
        String[] quantities  = req.getParameterValues("quantity[]");

        List<PrescriptionItem> items = new ArrayList<>();
        if (medicineIDs == null) return items;

        for (int i = 0; i < medicineIDs.length; i++) {
            String midStr = medicineIDs[i];
            if (midStr == null || midStr.isBlank()) continue;
            PrescriptionItem item = new PrescriptionItem();
            item.setMedicineID(Integer.parseInt(midStr));
            item.setDosage(dosages != null && i < dosages.length ? dosages[i] : "");
            item.setQuantity(new BigDecimal(
                    quantities != null && i < quantities.length ? quantities[i] : "1"));
            items.add(item);
        }
        return items;
    }

    private void forwardFormWithError(HttpServletRequest req, HttpServletResponse resp,
                                      int appointmentID, int vetID, String errorMsg)
            throws ServletException, IOException {
        try {
            Appointment appt = examinationService.getAppointment(appointmentID);
            List<MedicalRecord> history = appt != null
                    ? examinationService.getPetMedicalHistory(appt.getPetID()) : List.of();
            List<Medicine> medicines     = examinationService.getMedicinesInStock();
            List<Service>  labTests      = examinationService.getLabTests();
            List<Service>  treatmentPlans = examinationService.getTreatmentPlans();

            req.setAttribute("appointment",    appt);
            req.setAttribute("history",        history);
            req.setAttribute("medicines",      medicines);
            req.setAttribute("labTests",       labTests);
            req.setAttribute("treatmentPlans", treatmentPlans);
            req.setAttribute("error",          errorMsg);
            req.setAttribute("symptoms",       req.getParameter("symptoms"));
        } catch (Exception e) { e.printStackTrace(); }
        req.getRequestDispatcher("/WEB-INF/views/vet/examination-detail.jsp").forward(req, resp);
    }

    private Staff getAuthenticatedVet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/staff/login");
            return null;
        }
        Staff staff = (Staff) session.getAttribute("staff");
        if (staff == null || !"Veterinarian".equals(staff.getRoleName())) {
            resp.sendRedirect(req.getContextPath() + "/auth/staff/login");
            return null;
        }
        return staff;
    }

    private void triggerInvoice(int appointmentID) {
        System.out.println("[BP-04 STUB] Generate invoice for appointmentID=" + appointmentID);
    }
}