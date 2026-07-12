package com.petclinic.service;

import com.petclinic.dao.AppointmentDAO;
import com.petclinic.dao.GroomingRecordDAO;
import com.petclinic.model.Appointment;
import com.petclinic.model.AppointmentServiceItem;
import com.petclinic.model.GroomingRecord;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Service layer cho BP-03 (Grooming), theo mô hình AppointmentServices N-N:
 * mỗi dịch vụ Grooming trong 1 appointment có AssignedStaffID RIÊNG
 * (không còn 1 cột AssignedGroomerID chung trên Appointments).
 */
public class GroomingService {

    public static final String CATEGORY_GROOMING = GroomingRecordDAO.CATEGORY_GROOMING;

    private final AppointmentDAO    appointmentDAO    = new AppointmentDAO();
    private final GroomingRecordDAO groomingRecordDAO = new GroomingRecordDAO();

    // ══ ACCEPT SESSION (Groomer tự nhận ca) ═══════════════════════════════════
    public enum AcceptResult { SUCCESS, NOT_FOUND, WRONG_STATUS, ALREADY_TAKEN, NO_GROOMING_SERVICE }

    /**
     * Groomer tự nhận 1 ca chưa ai phụ trách (hoặc đã gán cho chính mình).
     * Tìm dòng dịch vụ Grooming trong appointment (có thể appointment này
     * còn có cả dịch vụ Khám — chỉ gán vào dòng Grooming, không đụng dòng khác).
     */
    public AcceptResult acceptSession(int appointmentID, int groomerID) throws SQLException {
        Appointment appt = appointmentDAO.findById(appointmentID);
        if (appt == null) return AcceptResult.NOT_FOUND;
        if (!"Arrived".equals(appt.getStatus())) return AcceptResult.WRONG_STATUS;

        List<AppointmentServiceItem> groomingLines = appt.getServicesByCategory(CATEGORY_GROOMING);
        if (groomingLines.isEmpty()) return AcceptResult.NO_GROOMING_SERVICE;

        // Kiểm tra xem đã có groomer khác nhận dòng Grooming nào chưa
        for (AppointmentServiceItem line : groomingLines) {
            Integer current = line.getAssignedStaffID();
            if (current != null && !current.equals(groomerID)) {
                return AcceptResult.ALREADY_TAKEN;
            }
        }

        // Gán groomerID cho MỌI dòng Grooming chưa có người (đồng bộ nếu appointment
        // có nhiều dịch vụ grooming cùng lúc, ví dụ Tắm + Cắt móng).
        for (AppointmentServiceItem line : groomingLines) {
            if (line.getAssignedStaffID() == null) {
                appointmentDAO.assignStaffToService(line.getAppointmentServiceID(), groomerID);
            }
        }
        return AcceptResult.SUCCESS;
    }

    // ══ START SESSION ══════════════════════════════════════════════════════════
    public enum StartResult { SUCCESS, NOT_FOUND, WRONG_STATUS, NO_GROOMING_SERVICE, ALREADY_TAKEN }

    /**
     * Bắt đầu phiên grooming. Nếu dòng dịch vụ Grooming CHƯA có ai nhận,
     * tự động gán cho groomer đang bấm (gộp bước "nhận ca" + "bắt đầu" làm 1).
     */
    public StartResult startSession(int appointmentID, int groomerID) throws SQLException {
        Appointment appt = appointmentDAO.findById(appointmentID);
        if (appt == null) return StartResult.NOT_FOUND;
        if (!"Arrived".equals(appt.getStatus())) return StartResult.WRONG_STATUS;

        List<AppointmentServiceItem> groomingLines = appt.getServicesByCategory(CATEGORY_GROOMING);
        if (groomingLines.isEmpty()) return StartResult.NO_GROOMING_SERVICE;

        for (AppointmentServiceItem line : groomingLines) {
            Integer current = line.getAssignedStaffID();
            if (current != null && !current.equals(groomerID)) {
                return StartResult.ALREADY_TAKEN;
            }
        }

        for (AppointmentServiceItem line : groomingLines) {
            if (line.getAssignedStaffID() == null) {
                appointmentDAO.assignStaffToService(line.getAppointmentServiceID(), groomerID);
            }
        }

        appointmentDAO.updateStatus(appointmentID, "InProgress");
        return StartResult.SUCCESS;
    }

    // ══ SAVE GROOMING RECORD ═══════════════════════════════════════════════════
    public enum SaveResult { SUCCESS, NOT_FOUND, WRONG_STATUS, RECORD_ALREADY_EXISTS, DB_ERROR }

    public SaveResult saveGroomingRecord(GroomingRecord record) throws SQLException {
        Appointment appt = appointmentDAO.findById(record.getAppointmentID());
        if (appt == null) return SaveResult.NOT_FOUND;
        if (!"InProgress".equals(appt.getStatus())) return SaveResult.WRONG_STATUS;
        if (groomingRecordDAO.findByAppointmentId(record.getAppointmentID()) != null)
            return SaveResult.RECORD_ALREADY_EXISTS;

        groomingRecordDAO.save(record);
        appointmentDAO.updateStatus(record.getAppointmentID(), "Done");
        return SaveResult.SUCCESS;
    }

    // ══ QUERY HELPERS ══════════════════════════════════════════════════════════

    public List<Appointment> getGroomerQueue(int groomerID, LocalDate date) throws SQLException {
        return groomingRecordDAO.findGroomerQueue(groomerID, date == null ? LocalDate.now() : date);
    }

    public List<Appointment> getGroomerCompletedToday(int groomerID, LocalDate date) throws SQLException {
        return groomingRecordDAO.findGroomerCompletedToday(groomerID, date == null ? LocalDate.now() : date);
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