package com.petclinic.dao;

import com.petclinic.model.Appointment;
import com.petclinic.model.MedicalRecord;
import com.petclinic.model.PrescriptionItem;
import com.petclinic.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MedicalRecordDAO {

    /** Load medical record cho một appointment */
    public MedicalRecord findByAppointment(int appointmentId) throws SQLException {
        String sql = "SELECT mr.*, s.FullName AS StaffName "
                + "FROM MedicalRecords mr "
                + "JOIN Staff s ON mr.StaffID = s.StaffID "
                + "WHERE mr.AppointmentID = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, appointmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                MedicalRecord mr = mapRecord(rs);
                mr.setPrescriptions(findPrescriptions(mr.getRecordID()));
                return mr;
            }
        }
    }

    public List<MedicalRecord> findByPet(int petId) throws SQLException {
        String sql = "SELECT mr.* "
                + "FROM MedicalRecords mr "
                + "WHERE mr.PetID = ? Order by mr.AppointmentID desc";
        List<MedicalRecord> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, petId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRecord(rs));
            }
        }
        return list;
    }

    public List<PrescriptionItem> findPrescriptions(int recordId) throws SQLException {
        String sql = "SELECT pi.*, m.Name AS MedicineName, m.Unit "
                + "FROM PrescriptionItems pi "
                + "JOIN Medicines m ON pi.MedicineID = m.MedicineID "
                + "WHERE pi.RecordID = ?";
        List<PrescriptionItem> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, recordId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PrescriptionItem item = new PrescriptionItem();
                    item.setItemID(rs.getInt("ItemID"));
                    item.setRecordID(rs.getInt("RecordID"));
                    item.setMedicineID(rs.getInt("MedicineID"));
                    item.setMedicineName(rs.getString("MedicineName"));
                    item.setUnit(rs.getString("Unit"));
                    item.setDosage(rs.getString("Dosage"));
                    item.setQuantity(rs.getBigDecimal("Quantity"));
                    item.setUnitPrice(rs.getBigDecimal("UnitPrice"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    private MedicalRecord mapRecord(ResultSet rs) throws SQLException {
        MedicalRecord mr = new MedicalRecord();
        mr.setRecordID(rs.getInt("RecordID"));
        mr.setAppointmentID(rs.getInt("AppointmentID"));
        mr.setPetID(rs.getInt("PetID"));
        mr.setStaffID(rs.getInt("StaffID"));
        mr.setWeight(rs.getBigDecimal("Weight"));
        mr.setTemperature(rs.getBigDecimal("Temperature"));
        mr.setSymptoms(rs.getString("Symptoms"));
        mr.setDiagnosis(rs.getString("Diagnosis"));
        mr.setTreatmentPlan(rs.getString("TreatmentPlan"));
        Timestamp ts = rs.getTimestamp("CreatedAt");
        if (ts != null) mr.setCreatedAt(ts.toLocalDateTime());
//        mr.setStaffName(rs.getString("StaffName"));
        return mr;
    }
}