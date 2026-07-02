package com.petclinic.service;

import com.petclinic.dao.AppointmentDAO;
import com.petclinic.dao.MedicalRecordDAO;
import com.petclinic.dao.MedicineDAO;
import com.petclinic.model.Appointment;
import com.petclinic.model.MedicalRecord;
import com.petclinic.model.Medicine;
import com.petclinic.model.PrescriptionItem;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * Service layer for BP-02: Examination & Medical Record.
 * <p>
 * Covers:
 * 1. Receptionist check-in  (Confirmed → Arrived, assign vet)
 * 2. Vet starts examination  (Arrived → InProgress)
 * 3. Vet saves medical record + prescription
 * 4. System auto-deducts stock, checks threshold, triggers invoice (BP-04)
 * 5. Appointment marked Done
 */
public class ExaminationService {

    private final AppointmentDAO appointmentDAO = new AppointmentDAO();
    private final MedicalRecordDAO medicalRecordDAO = new MedicalRecordDAO();
    private final MedicineDAO medicineDAO = new MedicineDAO();
    private final StockService stockService = new StockService();

    // ══════════════════════════════════════════════════════════════════════════
    //  RECEPTIONIST: CHECK-IN
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Receptionist confirms pet has arrived.
     * Transition: Confirmed → Arrived.
     * Optionally reassigns vet at check-in time.
     */
    public CheckInResult checkIn(int appointmentID, Integer vetID) throws SQLException {
        Appointment appt = appointmentDAO.findById(appointmentID);
        if (appt == null) return CheckInResult.NOT_FOUND;
        if ("Arrived".equals(appt.getStatus())) return CheckInResult.ALREADY_CHECKED_IN;
        if (!"Confirmed".equals(appt.getStatus())) return CheckInResult.WRONG_STATUS;

        if (vetID != null) {
            appointmentDAO.assignVet(appointmentID, vetID);
        }
        appointmentDAO.updateStatus(appointmentID, "Arrived");
        return CheckInResult.SUCCESS;
    }

    /**
     * Vet starts examining a patient.
     * Transition: Arrived → InProgress.
     */
    public StartExamResult startExamination(int appointmentID, int vetID) throws SQLException {
        Appointment appt = appointmentDAO.findById(appointmentID);
        if (appt == null) return StartExamResult.NOT_FOUND;
        if (!"Arrived".equals(appt.getStatus())) return StartExamResult.WRONG_STATUS;

        appointmentDAO.updateStatus(appointmentID, "InProgress");
        return StartExamResult.SUCCESS;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  VETERINARIAN: START EXAMINATION
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Save the medical record and prescription, deduct stock, mark appointment Done.
     *
     * @param record       filled MedicalRecord (appointmentID, petID, vetID, vitals, diagnosis)
     * @param items        list of PrescriptionItem (may be empty if no prescription)
     * @param followUpDate optional follow-up date stored in TreatmentPlan note
     * @return SaveRecordResult
     */
    public SaveRecordResult saveMedicalRecord(MedicalRecord record,
                                              List<PrescriptionItem> items,
                                              String followUpDate) throws SQLException {
        // Guard: appointment must exist and be InProgress
        Appointment appt = appointmentDAO.findById(record.getAppointmentID());
        if (appt == null) return SaveRecordResult.APPOINTMENT_NOT_FOUND;
        if (!"InProgress".equals(appt.getStatus())) return SaveRecordResult.WRONG_STATUS;

        // Guard: no duplicate medical record for this appointment
        if (medicalRecordDAO.findByAppointmentId(record.getAppointmentID()) != null)
            return SaveRecordResult.RECORD_ALREADY_EXISTS;

        // Append follow-up note to treatment plan
        if (followUpDate != null && !followUpDate.isBlank()) {
            String plan = record.getTreatmentPlan() == null ? "" : record.getTreatmentPlan();
            record.setTreatmentPlan(plan + "\nTái khám: " + followUpDate);
        }

        // Snapshot unit prices from current Medicine table
        if (items != null) {
            for (PrescriptionItem item : items) {
                if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(BigDecimal.ZERO) == 0) {
                    Medicine med = medicineDAO.findById(item.getMedicineID());
                    if (med != null) item.setUnitPrice(med.getUnitPrice());
                }
            }
        }

        try {
            // Save record + items + deduct stock (all in one DB transaction)
            medicalRecordDAO.save(record, items);
            stockService.notifyLowStockAfterPrescription(items);
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("Insufficient stock"))
                return SaveRecordResult.INSUFFICIENT_STOCK;
            throw e;
        }

        // Mark appointment Done
        appointmentDAO.updateStatus(record.getAppointmentID(), "Done");

        return SaveRecordResult.SUCCESS;
    }

    /**
     * Today's Arrived/InProgress appointments for a vet.
     */
    public List<Appointment> getVetQueueToday(int vetID) throws SQLException {
        return appointmentDAO.findVetQueueToday(vetID);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  VETERINARIAN: SAVE MEDICAL RECORD
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Today's Confirmed appointments for check-in screen.
     */
    public List<Appointment> getTodayConfirmedAppointments() throws SQLException {
        return appointmentDAO.findTodayConfirmed();
    }

    /**
     * Search confirmed appointments by keyword (owner or pet name).
     */
    public List<Appointment> searchForCheckIn(String keyword) throws SQLException {
        return appointmentDAO.searchForCheckIn(keyword);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  QUERY HELPERS (used by servlets)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Full medical history for a pet (vet review before exam).
     */
    public List<MedicalRecord> getPetMedicalHistory(int petID) throws SQLException {
        return medicalRecordDAO.findHistoryByPetId(petID);
    }

    /**
     * Fetch a single medical record by ID.
     */
    public MedicalRecord getMedicalRecord(int recordID) throws SQLException {
        return medicalRecordDAO.findById(recordID);
    }

    /**
     * All medicines in stock (for prescription form drop-down).
     */
    public List<Medicine> getMedicinesInStock() throws SQLException {
        return medicineDAO.findAllInStock();
    }

    /**
     * Load appointment by ID.
     */
    public Appointment getAppointment(int appointmentID) throws SQLException {
        return appointmentDAO.findById(appointmentID);
    }

    public enum CheckInResult {SUCCESS, NOT_FOUND, WRONG_STATUS, ALREADY_CHECKED_IN}

    public enum StartExamResult {SUCCESS, NOT_FOUND, WRONG_STATUS}

    public enum SaveRecordResult {
        SUCCESS,
        APPOINTMENT_NOT_FOUND,
        WRONG_STATUS,
        RECORD_ALREADY_EXISTS,
        INSUFFICIENT_STOCK,
        DB_ERROR
    }
}
