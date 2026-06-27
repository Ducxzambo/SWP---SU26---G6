package com.petclinic.dao;

import com.petclinic.model.VaccinationRecord;
import com.petclinic.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VaccinationRecordDAO {

    /** Lịch sử tiêm vaccine gắn với 1 appointment (1 appointment có thể có nhiều mũi). */
    public List<VaccinationRecord> findByAppointment(int appointmentId) throws SQLException {
        String sql = "SELECT vr.*, v.Name AS VaccineName, s.FullName AS StaffName "
                + "FROM VaccinationRecords vr "
                + "JOIN Vaccines v ON vr.VaccineID = v.VaccineID "
                + "JOIN Staff s ON vr.StaffID = s.StaffID "
                + "WHERE vr.AppointmentID = ? ORDER BY vr.AdministeredDate DESC";
        List<VaccinationRecord> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, appointmentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<VaccinationRecord> findByPet(int petId) throws SQLException {
        String sql = "SELECT vr.*, v.Name AS VaccineName, s.FullName AS StaffName "
                + "FROM VaccinationRecords vr JOIN Vaccines v  ON vr.VaccineID = v.VaccineID "
                + "JOIN Staff s ON  vr.StaffID = s.StaffID "
                + "WHERE vr.PetID = ? ORDER BY vr.AdministeredDate DESC";
        List<VaccinationRecord> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, petId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    private VaccinationRecord mapRow(ResultSet rs) throws SQLException {
        VaccinationRecord vr = new VaccinationRecord();
        vr.setVaccineRecordID(rs.getInt("VaccineRecordID"));
        vr.setPetID(rs.getInt("PetID"));
        vr.setVaccineID(rs.getInt("VaccineID"));
        vr.setStaffID(rs.getInt("StaffID"));
        int apptId = rs.getInt("AppointmentID");
        if (!rs.wasNull()) vr.setAppointmentID(apptId);
        Date ad = rs.getDate("AdministeredDate");
        if (ad != null) vr.setAdministeredDate(ad.toLocalDate());
        Date nd = rs.getDate("NextDueDate");
        if (nd != null) vr.setNextDueDate(nd.toLocalDate());
        vr.setVaccineName(rs.getString("VaccineName"));
//        vr.setStaffName(rs.getString("StaffName"));
        return vr;
    }
}