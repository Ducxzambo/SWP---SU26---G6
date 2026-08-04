package com.petclinic.admin.servlet.vet;

import com.petclinic.backend.dao.AppointmentDAO;
import com.petclinic.backend.dao.MedicalRecordDAO;
import com.petclinic.backend.model.*;
import com.petclinic.backend.service.ExaminationService;
import com.petclinic.backend.service.ExaminationService.SaveRecordResult;
import com.petclinic.backend.service.ExaminationService.StartExamResult;
import com.petclinic.backend.service.InvoiceSyncService;
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
import java.util.Set;
import java.util.stream.Collectors;

@WebServlet("/vet/examination")
public class ExaminationServlet extends HttpServlet {

    private final AppointmentDAO appointmentDAO = new AppointmentDAO();
    private final MedicalRecordDAO medicalRecordDAO = new MedicalRecordDAO();
    private final ExaminationService examinationService = new ExaminationService();
    private final InvoiceSyncService invoiceSyncService = new InvoiceSyncService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Staff vet = getAuthenticatedVet(req, resp);
        if (vet == null) return;

        String action = req.getParameter("action");
        if (action == null) action = "queue";

        switch (action) {
            case "queue"   -> showQueue(req, resp, vet);
            case "history" -> showHistory(req, resp, vet);
            case "start"   -> startExam(req, resp, vet);
            case "form"    -> showExamForm(req, resp, vet);
            case "view"    -> viewRecord(req, resp, vet);
            default        -> resp.sendRedirect(req.getContextPath() + "/vet/examination");
        }
    }

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
                forwardFormWithError(req, resp, appointmentID, "Không tìm thấy lịch hẹn.");
                return;
            }

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
            record.setGeneralConclusion(req.getParameter("conclusion"));

            // 1. LUÔN LẤY DỮ LIỆU TỪ REQUEST LÊN TRƯỚC (Dành cho lần lưu đầu tiên)
            List<SupplyUsageItem> reqSupplies = parseSupplyItems(req);
            List<PrescriptionItem> reqItems = parsePrescriptionItems(req);
            List<Integer> reqSelectedServiceIds = parseSelectedServiceIds(req);

            String followUpDate = req.getParameter("followUpDate");
            String submitAction = req.getParameter("submitAction"); // "save" hoặc "complete"

            // 2. THỰC HIỆN LƯU VỚI DỮ LIỆU TỪ REQUEST
            SaveRecordResult result = examinationService.saveMedicalRecord(record, reqItems, reqSupplies, followUpDate);

            switch (result) {
                case SUCCESS -> {
                    invoiceSyncService.syncAfterExamination(appointmentID, vet.getStaffID(),
                            reqSelectedServiceIds, reqItems, reqSupplies);

                    if ("complete".equals(submitAction)) {
                        // Ca khám mới tinh -> Lưu xong -> Hoàn thành luôn
                        ExaminationService.CompleteResult cr =
                                examinationService.completeExamination(appointmentID);
                        if (cr == ExaminationService.CompleteResult.SUCCESS) {
                            req.getSession().setAttribute("flashSuccess",
                                    "Bệnh án đã lưu thành công. Hóa đơn đã được cập nhật.");
                            resp.sendRedirect(req.getContextPath() + "/vet/examination");
                        } else {
                            forwardFormWithError(req, resp, appointmentID,
                                    "Lưu bệnh án thành công nhưng không thể hoàn tất: " + cr);
                        }
                    } else {
                        // Chỉ lưu, ở lại form - hóa đơn vẫn đã được cập nhật ở trên
                        req.getSession().setAttribute("flashSuccess",
                                "Đã lưu bệnh án thành công. Hóa đơn đã được cập nhật.");
                        resp.sendRedirect(req.getContextPath()
                                + "/vet/examination?action=form&appointmentID=" + appointmentID);
                    }
                }
                case RECORD_ALREADY_EXISTS -> {
                    // 3. BỆNH ÁN ĐÃ TỒN TẠI
                    if ("complete".equals(submitAction)) {
                        ExaminationService.CompleteResult cr =
                                examinationService.completeExamination(appointmentID);
                        if (cr == ExaminationService.CompleteResult.SUCCESS) {
                            req.getSession().setAttribute("flashSuccess",
                                    "Bệnh án đã hoàn thành. Hóa đơn đang tạo...");
                            invoiceSyncService.syncAfterExamination(appointmentID, vet.getStaffID(),
                                    reqSelectedServiceIds, reqItems, reqSupplies);

                            resp.sendRedirect(req.getContextPath() + "/vet/examination");
                        } else {
                            forwardFormWithError(req, resp, appointmentID,
                                    "Không thể hoàn tất: " + cr);
                        }
                    } else {
                        forwardFormWithError(req, resp, appointmentID,
                                "Bệnh án cho lịch khám này đã tồn tại.");
                    }
                }
                case INSUFFICIENT_STOCK ->
                        forwardFormWithError(req, resp, appointmentID,
                                "Thuốc không đủ tồn kho. Vui lòng kiểm tra lại đơn thuốc.");
                case INSUFFICIENT_SUPPLY_STOCK ->
                        forwardFormWithError(req, resp, appointmentID,
                                "Vật tư không đủ tồn kho. Vui lòng kiểm tra lại.");
                case WRONG_STATUS ->
                        forwardFormWithError(req, resp, appointmentID,
                                "Lịch hẹn không ở trạng thái InProgress.");
                default ->
                        forwardFormWithError(req, resp, appointmentID, "Lỗi hệ thống, vui lòng thử lại.");
            }
        } catch (NumberFormatException e) {
            forwardFormWithError(req, resp, appointmentID,
                    "Dữ liệu nhập không hợp lệ (cân nặng, nhiệt độ, số lượng thuốc).");
        } catch (Exception e) {
            e.printStackTrace();
            forwardFormWithError(req, resp, appointmentID, "Lỗi hệ thống: " + e.getMessage());
        }
    }


    private void showQueue(HttpServletRequest req, HttpServletResponse resp, Staff vet)
            throws ServletException, IOException {

        LocalDate filterDate = parseDate(req.getParameter("date"));
        String shiftParam = req.getParameter("shift");
        Integer shiftFilter = parseShift(shiftParam);

        try {
            List<Appointment> queue = examinationService.getVetQueue(vet.getStaffID(), filterDate);

            if (shiftFilter != null) {
                queue = queue.stream()
                        .filter(a -> a.getSlotShift() != null && a.getSlotShift().equals(shiftFilter))
                        .collect(Collectors.toList());
            }

            req.setAttribute("queue",       queue);
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

    // Tab Lịch sử: các ca bác sĩ này đã lưu bệnh án xong
    private void showHistory(HttpServletRequest req, HttpServletResponse resp, Staff vet)
            throws ServletException, IOException {

        LocalDate filterDate = parseDate(req.getParameter("date"));
        String shiftParam = req.getParameter("shift");
        Integer shiftFilter = parseShift(shiftParam);

        try {
            List<Appointment> completed = examinationService.getVetCompletedToday(vet.getStaffID(), filterDate);

            if (shiftFilter != null) {
                completed = completed.stream()
                        .filter(a -> a.getSlotShift() != null && a.getSlotShift().equals(shiftFilter))
                        .collect(Collectors.toList());
            }

            req.setAttribute("completed",   completed);
            req.setAttribute("filterDate",  filterDate.toString());
            req.setAttribute("isToday",     filterDate.equals(LocalDate.now()));
            req.setAttribute("shiftFilter", shiftParam != null ? shiftParam : "");
            req.getRequestDispatcher("/WEB-INF/views/vet/examination-history.jsp").forward(req, resp);

        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Không tải được lịch sử.");
            req.getRequestDispatcher("/WEB-INF/views/vet/examination-history.jsp").forward(req, resp);
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

            List<MedicalRecord> history      = examinationService.getPetMedicalHistory(appt.getPetID());
            List<Medicine>      medicines    = examinationService.getMedicinesInStock();
            List<Service>       labTests     = examinationService.getLabTests();
            List<Service>       treatmentPlans = examinationService.getTreatmentPlans();
            List<Supply>        supplies = examinationService.getSuppliesInStock();

            Set<Integer> preSelectedLabIds = appt.getServicesByCategory("Chẩn đoán")
                    .stream().map(s -> s.getServiceID()).collect(Collectors.toSet());
            Set<Integer> preSelectedTreatIds = appt.getServicesByCategory("Điều trị")
                    .stream().map(s -> s.getServiceID()).collect(Collectors.toSet());

            req.setAttribute("appointment",    appt);
            req.setAttribute("history",        history);
            req.setAttribute("medicines",      medicines);
            req.setAttribute("labTests",       labTests);
            req.setAttribute("treatmentPlans", treatmentPlans);
            req.setAttribute("supplies",       supplies);
            req.setAttribute("preSelectedLabIds",   preSelectedLabIds);
            req.setAttribute("preSelectedTreatIds", preSelectedTreatIds);
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
            Appointment app = appointmentDAO.findById(rec != null ? rec.getAppointmentID() : -1);
            if (rec == null) { resp.sendRedirect(req.getContextPath() + "/vet/examination"); return; }
            req.setAttribute("record", rec);
            req.setAttribute("appt", app);
            req.getRequestDispatcher("/WEB-INF/views/vet/examination-detail.jsp").forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect(req.getContextPath() + "/vet/examination");
        }
    }

    // Helpers

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

    private List<SupplyUsageItem> parseSupplyItems(HttpServletRequest req) {
        String[] supplyIDs  = req.getParameterValues("supplyID[]");
        String[] quantities = req.getParameterValues("supplyQty[]");
        String[] notes      = req.getParameterValues("supplyNote[]");

        List<SupplyUsageItem> items = new ArrayList<>();
        if (supplyIDs == null) return items;

        for (int i = 0; i < supplyIDs.length; i++) {
            String sidStr = supplyIDs[i];
            if (sidStr == null || sidStr.isBlank()) continue;
            SupplyUsageItem item = new SupplyUsageItem();
            item.setSupplyID(Integer.parseInt(sidStr));
            item.setQuantity(new BigDecimal(
                    quantities != null && i < quantities.length ? quantities[i] : "1"));
            if (notes != null && i < notes.length) item.setNotes(notes[i]);
            items.add(item);
        }
        return items;
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
                                      int appointmentID, String errorMsg)
            throws ServletException, IOException {
        try {
            Appointment appt = examinationService.getAppointment(appointmentID);
            List<MedicalRecord> history = appt != null
                    ? examinationService.getPetMedicalHistory(appt.getPetID()) : List.of();
            List<Medicine> medicines      = examinationService.getMedicinesInStock();
            List<Service>  labTests       = examinationService.getLabTests();
            List<Service>  treatmentPlans = examinationService.getTreatmentPlans();

            Set<Integer> preSelectedLabIds = appt != null
                    ? appt.getServicesByCategory("Chẩn đoán")
                      .stream().map(s -> s.getServiceID()).collect(Collectors.toSet())
                    : java.util.Collections.emptySet();
            Set<Integer> preSelectedTreatIds = appt != null
                    ? appt.getServicesByCategory("Điều trị")
                      .stream().map(s -> s.getServiceID()).collect(Collectors.toSet())
                    : java.util.Collections.emptySet();

            req.setAttribute("appointment",    appt);
            req.setAttribute("history",        history);
            req.setAttribute("medicines",      medicines);
            req.setAttribute("labTests",       labTests);
            req.setAttribute("treatmentPlans", treatmentPlans);
            req.setAttribute("error",          errorMsg);
            req.setAttribute("symptoms",       req.getParameter("symptoms"));
            req.setAttribute("preSelectedLabIds",   preSelectedLabIds);
            req.setAttribute("preSelectedTreatIds", preSelectedTreatIds);
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

    private LocalDate parseDate(String p) {
        if (p == null || p.isBlank()) return LocalDate.now();
        try { return LocalDate.parse(p); } catch (DateTimeParseException e) { return LocalDate.now(); }
    }

    private Integer parseShift(String p) {
        if (p == null || p.isBlank()) return null;
        try { return Integer.parseInt(p); } catch (NumberFormatException e) { return null; }
    }

    private List<Integer> parseSelectedServiceIds(HttpServletRequest req) {
        List<Integer> ids = new ArrayList<>();
        appendIds(ids, req.getParameterValues("labTestID[]"));
        appendIds(ids, req.getParameterValues("treatmentID[]"));
        return ids;
    }

    private void appendIds(List<Integer> out, String[] arr) {
        if (arr == null) return;
        for (String s : arr) {
            if (s == null || s.isBlank()) continue;
            try { out.add(Integer.parseInt(s.trim())); } catch (NumberFormatException ignored) { }
        }
    }
}