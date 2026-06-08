package com.petclinic.servlet.vet;

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
import java.util.ArrayList;
import java.util.List;

/**
 * BP-02 Steps 2-5 — Veterinarian examination flow.
 * <p>
 * URL patterns:
 * GET  /vet/examination              → queue dashboard (Arrived + InProgress today)
 * GET  /vet/examination?action=start&appointmentID=X
 * → start examination (Arrived → InProgress) + redirect to form
 * GET  /vet/examination?action=form&appointmentID=X
 * → show examination form + pet history
 * POST /vet/examination              → save medical record + prescription → Done
 * GET  /vet/examination?action=view&recordID=X
 * → view completed medical record (read-only)
 */
@WebServlet("/vet/examination")
public class ExaminationServlet extends HttpServlet {

    private final ExaminationService examinationService = new ExaminationService();

    // ── GET dispatcher ────────────────────────────────────────────────────────
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Staff vet = getAuthenticatedVet(req, resp);
        if (vet == null) return;

        String action = req.getParameter("action");
        if (action == null) action = "queue";

        switch (action) {
            case "queue":
                showQueue(req, resp, vet);
                break;
            case "start":
                startExam(req, resp, vet);
                break;
            case "form":
                showExamForm(req, resp, vet);
                break;
            case "view":
                viewRecord(req, resp, vet);
                break;
            default:
                resp.sendRedirect(req.getContextPath() + "/vet/examination");
        }
    }

    // ── POST: save medical record ─────────────────────────────────────────────
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");

        Staff vet = getAuthenticatedVet(req, resp);
        if (vet == null) return;

        String appointmentIdStr = req.getParameter("appointmentID");
        if (appointmentIdStr == null || appointmentIdStr.isBlank()) {
            resp.sendRedirect(req.getContextPath() + "/vet/examination");
            return;
        }

        int appointmentID;
        try {
            appointmentID = Integer.parseInt(appointmentIdStr);
        } catch (NumberFormatException e) {
            resp.sendRedirect(req.getContextPath() + "/vet/examination");
            return;
        }

        try {
            // Load appointment to get petID
            Appointment appt = examinationService.getAppointment(appointmentID);
            if (appt == null) {
                forwardFormWithError(req, resp, appointmentID, vet.getStaffID(),
                        "Không tìm thấy lịch hẹn.");
                return;
            }

            // Build MedicalRecord from form
            MedicalRecord record = new MedicalRecord();
            record.setAppointmentID(appointmentID);
            record.setPetID(appt.getPetID());
            record.setVetID(vet.getStaffID());

            String weightStr = req.getParameter("weight");
            String tempStr = req.getParameter("temperature");
            if (weightStr != null && !weightStr.isBlank())
                record.setWeight(new BigDecimal(weightStr));
            if (tempStr != null && !tempStr.isBlank())
                record.setTemperature(new BigDecimal(tempStr));

            record.setSymptoms(req.getParameter("symptoms"));
            record.setDiagnosis(req.getParameter("diagnosis"));
            record.setTreatmentPlan(req.getParameter("treatmentPlan"));

            // Build PrescriptionItems from repeating form fields
            List<PrescriptionItem> items = parsePrescriptionItems(req, record.getVetID());

            String followUpDate = req.getParameter("followUpDate");

            // Save via service
            SaveRecordResult result = examinationService.saveMedicalRecord(record, items, followUpDate);

            switch (result) {
                case SUCCESS:
                    req.getSession().setAttribute("flashSuccess",
                            "Bệnh án đã được lưu thành công. Hóa đơn đang được tạo...");
                    // BP-04 hook: trigger invoice generation (stub — replace with InvoiceService call)
                    triggerInvoiceGeneration(appointmentID);
                    resp.sendRedirect(req.getContextPath() + "/vet/examination");
                    break;
                case INSUFFICIENT_STOCK:
                    forwardFormWithError(req, resp, appointmentID, vet.getStaffID(),
                            "Thuốc không đủ tồn kho. Vui lòng kiểm tra lại đơn thuốc.");
                    break;
                case RECORD_ALREADY_EXISTS:
                    forwardFormWithError(req, resp, appointmentID, vet.getStaffID(),
                            "Bệnh án cho lịch khám này đã tồn tại.");
                    break;
                case WRONG_STATUS:
                    forwardFormWithError(req, resp, appointmentID, vet.getStaffID(),
                            "Lịch hẹn không ở trạng thái InProgress. Vui lòng kiểm tra lại.");
                    break;
                default:
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

    /**
     * Queue: show Arrived + InProgress appointments for this vet today.
     */
    private void showQueue(HttpServletRequest req, HttpServletResponse resp, Staff vet)
            throws ServletException, IOException {
        try {
            List<Appointment> queue = examinationService.getVetQueueToday(vet.getStaffID());
            req.setAttribute("queue", queue);
            req.getRequestDispatcher("/WEB-INF/views/vet/examination.jsp").forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Không tải được danh sách bệnh nhân.");
            req.getRequestDispatcher("/WEB-INF/views/vet/examination.jsp").forward(req, resp);
        }
    }

    /**
     * Start exam: transition Arrived → InProgress then redirect to form.
     */
    private void startExam(HttpServletRequest req, HttpServletResponse resp, Staff vet)
            throws ServletException, IOException {
        String idStr = req.getParameter("appointmentID");
        if (idStr == null) {
            resp.sendRedirect(req.getContextPath() + "/vet/examination");
            return;
        }

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

    /**
     * Show examination form with pet history and medicine list.
     */
    private void showExamForm(HttpServletRequest req, HttpServletResponse resp, Staff vet)
            throws ServletException, IOException {
        String idStr = req.getParameter("appointmentID");
        if (idStr == null) {
            resp.sendRedirect(req.getContextPath() + "/vet/examination");
            return;
        }

        try {
            int apptID = Integer.parseInt(idStr);
            Appointment appt = examinationService.getAppointment(apptID);
            if (appt == null || !Integer.valueOf(vet.getStaffID()).equals(appt.getAssignedVetID())) {
                resp.sendRedirect(req.getContextPath() + "/vet/examination");
                return;
            }

            // Load pet medical history for review
            List<MedicalRecord> history = examinationService.getPetMedicalHistory(appt.getPetID());
            List<Medicine> medicines = examinationService.getMedicinesInStock();

            req.setAttribute("appointment", appt);
            req.setAttribute("history", history);
            req.setAttribute("medicines", medicines);
            req.getRequestDispatcher("/WEB-INF/views/vet/examination-detail.jsp").forward(req, resp);

        } catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect(req.getContextPath() + "/vet/examination");
        }
    }

    /**
     * View a completed (read-only) medical record.
     */
    private void viewRecord(HttpServletRequest req, HttpServletResponse resp, Staff vet)
            throws ServletException, IOException {
        String idStr = req.getParameter("recordID");
        if (idStr == null) {
            resp.sendRedirect(req.getContextPath() + "/vet/examination");
            return;
        }

        try {
            int recordID = Integer.parseInt(idStr);
            MedicalRecord rec = examinationService.getMedicalRecord(recordID);
            if (rec == null) {
                resp.sendRedirect(req.getContextPath() + "/vet/examination");
                return;
            }

            req.setAttribute("record", rec);
            req.getRequestDispatcher("/WEB-INF/views/vet/examination-detail.jsp").forward(req, resp);

        } catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect(req.getContextPath() + "/vet/examination");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Parse repeating prescription fields from the form:
     * medicineID[]  dosage[]  quantity[]
     * Skips rows where medicineID is blank.
     */
    private List<PrescriptionItem> parsePrescriptionItems(HttpServletRequest req, int vetID) {
        String[] medicineIDs = req.getParameterValues("medicineID[]");
        String[] dosages = req.getParameterValues("dosage[]");
        String[] quantities = req.getParameterValues("quantity[]");

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
            // unitPrice will be snapshot in service layer
            items.add(item);
        }
        return items;
    }

    /**
     * Re-load form data and forward with error message.
     */
    private void forwardFormWithError(HttpServletRequest req, HttpServletResponse resp,
                                      int appointmentID, int vetID, String errorMsg)
            throws ServletException, IOException {
        try {
            Appointment appt = examinationService.getAppointment(appointmentID);
            List<MedicalRecord> history = appt != null
                    ? examinationService.getPetMedicalHistory(appt.getPetID()) : List.of();
            List<Medicine> medicines = examinationService.getMedicinesInStock();

            req.setAttribute("appointment", appt);
            req.setAttribute("history", history);
            req.setAttribute("medicines", medicines);
            req.setAttribute("error", errorMsg);
            // Re-populate form fields
            req.setAttribute("symptoms", req.getParameter("symptoms"));
            req.setAttribute("diagnosis", req.getParameter("diagnosis"));
            req.setAttribute("treatmentPlan", req.getParameter("treatmentPlan"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        req.getRequestDispatcher("/WEB-INF/views/vet/examination-detail.jsp").forward(req, resp);
    }

    /**
     * Validate vet session; redirect to login if missing. Returns null on failure.
     */
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

    /**
     * BP-04 hook stub — replace with InvoiceService.generateFromAppointment() call
     * once the invoice module is implemented.
     */
    private void triggerInvoiceGeneration(int appointmentID) {
        // TODO: InvoiceService.generateFromAppointment(appointmentID);
        System.out.println("[BP-04 STUB] Generate invoice for appointmentID=" + appointmentID);
    }
}