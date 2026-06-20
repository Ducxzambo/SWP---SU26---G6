package com.petclinic.service;

import com.petclinic.dao.AppointmentDAO;
import com.petclinic.dao.GroomingRecordDAO;
import com.petclinic.model.Appointment;
import com.petclinic.model.GroomingRecord;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class GroomingService {

    private final AppointmentDAO    appointmentDAO    = new AppointmentDAO();
    private final GroomingRecordDAO groomingRecordDAO = new GroomingRecordDAO();

    // ══ CHECK-IN (Receptionist) ════════════════════════════════════════════════
    public enum CheckInResult { SUCCESS, NOT_FOUND, WRONG_STATUS, ALREADY_CHECKED_IN }

    public CheckInResult checkIn(int appointmentID, Integer groomerID) throws SQLException {
        Appointment appt = appointmentDAO.findById(appointmentID);
        if (appt == null)                       return CheckInResult.NOT_FOUND;
        if ("Arrived".equals(appt.getStatus())) return CheckInResult.ALREADY_CHECKED_IN;
        if (!"Confirmed".equals(appt.getStatus())) return CheckInResult.WRONG_STATUS;

        if (groomerID != null) groomingRecordDAO.assignGroomer(appointmentID, groomerID);
        appointmentDAO.updateStatus(appointmentID, "Arrived");
        return CheckInResult.SUCCESS;
    }

    // ══ ACCEPT SESSION (Groomer) ═══════════════════════════════════════════════
    public enum AcceptResult { SUCCESS, NOT_FOUND, ALREADY_TAKEN, WRONG_STATUS }

    /**
     * Groomer accepts an unassigned or self-assigned session.
     * Sets AssignedGroomerID + keeps status as Arrived (not started yet).
     */
    public AcceptResult acceptSession(int appointmentID, int groomerID) throws SQLException {
        Appointment appt = appointmentDAO.findById(appointmentID);
        if (appt == null) return AcceptResult.NOT_FOUND;
        if (!"Arrived".equals(appt.getStatus())) return AcceptResult.WRONG_STATUS;

        // Already taken by another groomer?
        if (appt.getAssignedGroomerID() != null && appt.getAssignedGroomerID() != groomerID)
            return AcceptResult.ALREADY_TAKEN;

        groomingRecordDAO.assignGroomer(appointmentID, groomerID);
        return AcceptResult.SUCCESS;
    }

    // ══ START SESSION (Groomer) ════════════════════════════════════════════════
    public enum StartResult { SUCCESS, NOT_FOUND, WRONG_STATUS, NOT_ASSIGNED }

    public StartResult startSession(int appointmentID, int groomerID) throws SQLException {
        Appointment appt = appointmentDAO.findById(appointmentID);
        if (appt == null) return StartResult.NOT_FOUND;
        if (!"Arrived".equals(appt.getStatus())) return StartResult.WRONG_STATUS;
        if (appt.getAssignedGroomerID() == null || appt.getAssignedGroomerID() != groomerID)
            return StartResult.NOT_ASSIGNED;

        appointmentDAO.updateStatus(appointmentID, "InProgress");
        return StartResult.SUCCESS;
    }

    // ══ SAVE GROOMING RECORD (Groomer) ═════════════════════════════════════════
    public enum SaveResult {
        SUCCESS, NOT_FOUND, WRONG_STATUS, RECORD_ALREADY_EXISTS, DB_ERROR
    }

    public SaveResult saveGroomingRecord(GroomingRecord record) throws SQLException {
        Appointment appt = appointmentDAO.findById(record.getAppointmentID());
        if (appt == null) return SaveResult.NOT_FOUND;
        if (!"InProgress".equals(appt.getStatus())) return SaveResult.WRONG_STATUS;
        if (groomingRecordDAO.findByAppointmentId(record.getAppointmentID()) != null)
            return SaveResult.RECORD_ALREADY_EXISTS;

        try {
            groomingRecordDAO.save(record);
        } catch (SQLException e) {
            throw e;
        }

        appointmentDAO.updateStatus(record.getAppointmentID(), "Done");
        return SaveResult.SUCCESS;
    }

    // ══ QUERY HELPERS ══════════════════════════════════════════════════════════

    public List<Appointment> getGroomerQueue(int groomerID, LocalDate date) throws SQLException {
        return groomingRecordDAO.findGroomerQueue(groomerID, date == null ? LocalDate.now() : date);
    }

    public GroomingRecord getGroomingRecord(int recordID) throws SQLException {
        return groomingRecordDAO.findById(recordID);
    }

    public GroomingRecord getGroomingRecordByAppointment(int appointmentID) throws SQLException {
        return groomingRecordDAO.findByAppointmentId(appointmentID);
    }

    public List<GroomingRecord> getPetGroomingHistory(int petID) throws SQLException {
        return groomingRecordDAO.findHistoryByPetId(petID);
    }

    public Appointment getAppointment(int appointmentID) throws SQLException {
        return appointmentDAO.findById(appointmentID);
    }
}