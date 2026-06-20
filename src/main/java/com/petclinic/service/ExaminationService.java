package com.petclinic.service;

import com.petclinic.dao.*;
import com.petclinic.model.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class ExaminationService {

    private final AppointmentDAO appointmentDAO = new AppointmentDAO();
    private final MedicalRecordDAO medicalRecordDAO = new MedicalRecordDAO();
    private final MedicineDAO medicineDAO = new MedicineDAO();
    private final ServiceDAO serviceDAO = new ServiceDAO();

    public CheckInResult checkIn(int appointmentID, Integer vetID) throws SQLException {
        Appointment appt = appointmentDAO.findById(appointmentID);
        if (appt == null) return CheckInResult.NOT_FOUND;
        if ("Arrived".equals(appt.getStatus())) return CheckInResult.ALREADY_CHECKED_IN;
        if (!"Confirmed".equals(appt.getStatus())) return CheckInResult.WRONG_STATUS;
        if (vetID != null) appointmentDAO.assignVet(appointmentID, vetID);
        appointmentDAO.updateStatus(appointmentID, "Arrived");
        return CheckInResult.SUCCESS;
    }

    /**
     * Walk-in: tạo lịch + Arrived ngay.
     * Kiểm tra slot hiện tại không quá 10.
     * Returns appointmentID on success, -1 on slot full.
     */
    public int createWalkIn(int customerID, int petID,
                            int serviceID, int vetID) throws SQLException {
        LocalDate today = LocalDate.now();
        int shift = AppointmentDAO.shiftOf(LocalTime.now());
        if (shift == -1) shift = 1;
        if (appointmentDAO.isSlotFull(today, shift)) return -1; // slot full
        return appointmentDAO.createWalkIn(customerID, petID, serviceID, vetID);
    }

    /**
     * Returns number of booked pets in current shift.
     */
    public int getCurrentShiftCount() throws SQLException {
        int shift = AppointmentDAO.shiftOf(LocalTime.now());
        if (shift == -1) return 0;
        return appointmentDAO.countSlotBookings(LocalDate.now(), shift);
    }

    public boolean isCurrentShiftFull() throws SQLException {
        int shift = AppointmentDAO.shiftOf(LocalTime.now());
        if (shift == -1) return false;
        return appointmentDAO.isSlotFull(LocalDate.now(), shift);
    }

    // ══ SLOT INFO ══════════════════════════════════════════════════════════════

    public StartExamResult startExamination(int appointmentID, int vetID) throws SQLException {
        Appointment appt = appointmentDAO.findById(appointmentID);
        if (appt == null) return StartExamResult.NOT_FOUND;
        if (!"Arrived".equals(appt.getStatus())) return StartExamResult.WRONG_STATUS;
        appointmentDAO.updateStatus(appointmentID, "InProgress");
        return StartExamResult.SUCCESS;
    }

    public SaveRecordResult saveMedicalRecord(MedicalRecord record,
                                              List<PrescriptionItem> items,
                                              String followUpDate) throws SQLException {
        Appointment appt = appointmentDAO.findById(record.getAppointmentID());
        if (appt == null) return SaveRecordResult.APPOINTMENT_NOT_FOUND;
        if (!"InProgress".equals(appt.getStatus())) return SaveRecordResult.WRONG_STATUS;
        if (medicalRecordDAO.findByAppointmentId(record.getAppointmentID()) != null)
            return SaveRecordResult.RECORD_ALREADY_EXISTS;

        if (followUpDate != null && !followUpDate.isBlank()) {
            String plan = record.getTreatmentPlan() == null ? "" : record.getTreatmentPlan();
            record.setTreatmentPlan(plan + "\nTai kham: " + followUpDate);
        }

        if (items != null) {
            for (PrescriptionItem item : items) {
                if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(BigDecimal.ZERO) == 0) {
                    Medicine med = medicineDAO.findById(item.getMedicineID());
                    if (med != null) item.setUnitPrice(med.getUnitPrice());
                }
            }
        }

        try {
            medicalRecordDAO.save(record, items);
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("Insufficient stock"))
                return SaveRecordResult.INSUFFICIENT_STOCK;
            throw e;
        }
        appointmentDAO.updateStatus(record.getAppointmentID(), "Done");
        return SaveRecordResult.SUCCESS;
    }

    // ══ QUERY HELPERS ═════════════════════════════════════════════════════════
    public List<Appointment> getConfirmedByDate(LocalDate date, Integer shift) throws SQLException {
        return appointmentDAO.findConfirmedByDate(date == null ? LocalDate.now() : date, shift);
    }

    public List<Appointment> searchForCheckIn(String keyword, LocalDate date) throws SQLException {
        return appointmentDAO.searchForCheckIn(keyword, date == null ? LocalDate.now() : date);
    }

    public List<Appointment> getVetQueue(int vetID, LocalDate date) throws SQLException {
        return appointmentDAO.findVetQueue(vetID, date == null ? LocalDate.now() : date);
    }

    public List<MedicalRecord> getPetMedicalHistory(int petID) throws SQLException {
        return medicalRecordDAO.findHistoryByPetId(petID);
    }

    public MedicalRecord getMedicalRecord(int recordID) throws SQLException {
        return medicalRecordDAO.findById(recordID);
    }

    public List<Medicine> getMedicinesInStock() throws SQLException {
        return medicineDAO.findAllInStock();
    }

    public Appointment getAppointment(int appointmentID) throws SQLException {
        return appointmentDAO.findById(appointmentID);
    }

    public List<com.petclinic.model.Service> getLabTests() throws SQLException {
        return serviceDAO.findLabTests();
    }

    public List<com.petclinic.model.Service> getTreatmentPlans() throws SQLException {
        return serviceDAO.findTreatmentPlans();
    }

    // ══ CHECK-IN ══════════════════════════════════════════════════════════════
    public enum CheckInResult {SUCCESS, NOT_FOUND, WRONG_STATUS, ALREADY_CHECKED_IN}

    // ══ WALK-IN ═══════════════════════════════════════════════════════════════
    public enum WalkInResult {SUCCESS, SLOT_FULL, DB_ERROR}

    // ══ VET QUEUE ══════════════════════════════════════════════════════════════
    public enum StartExamResult {SUCCESS, NOT_FOUND, WRONG_STATUS}

    // ══ SAVE MEDICAL RECORD ═══════════════════════════════════════════════════
    public enum SaveRecordResult {
        SUCCESS, APPOINTMENT_NOT_FOUND, WRONG_STATUS,
        RECORD_ALREADY_EXISTS, INSUFFICIENT_STOCK, DB_ERROR
    }

    public List<Service> getTestServices() throws SQLException {
        return serviceDAO.findLabTests();
    }

    public List<Service> getTreatmentPlanServices() throws SQLException {
        return serviceDAO.findTreatmentPlans();
    }

}