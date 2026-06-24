package com.petclinic.dao;

import com.petclinic.model.GroomingRecord;
import com.petclinic.util.DBConnection;

import java.sql.*;

public class GroomingRecordDAO {

    /** Load grooming record cho 1 appointment (null nếu groomer chưa ghi nhận). */
    public GroomingRecord findByAppointment(int appointmentId) throws SQLException {
        String sql = "SELECT gr.*, s.FullName AS GroomerName "
                + "FROM GroomingRecords gr "
                + "JOIN Staff s ON gr.GroomerID = s.StaffID "
                + "WHERE gr.AppointmentID = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, appointmentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    private GroomingRecord mapRow(ResultSet rs) throws SQLException {
        GroomingRecord gr = new GroomingRecord();
        gr.setRecordID(rs.getInt("RecordID"));
        gr.setAppointmentID(rs.getInt("AppointmentID"));
        gr.setPetID(rs.getInt("PetID"));
        gr.setGroomerID(rs.getInt("GroomerID"));
        gr.setCoatCondition(rs.getString("CoatCondition"));
        gr.setBehavior(rs.getString("Behavior"));
        gr.setProductsUsed(rs.getString("ProductsUsed"));
        gr.setNotes(rs.getString("Notes"));
        gr.setFlagForVet(rs.getBoolean("FlagForVet"));
        gr.setFlagReason(rs.getString("FlagReason"));
        Timestamp ts = rs.getTimestamp("CreatedAt");
        if (ts != null) gr.setCreatedAt(ts.toLocalDateTime());
        gr.setGroomerName(rs.getString("GroomerName"));
        return gr;
    }
}