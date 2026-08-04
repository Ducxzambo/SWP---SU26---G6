package com.petclinic.backend.dao;

import com.petclinic.backend.model.Appointment;
import com.petclinic.backend.model.GroomingRecord;
import com.petclinic.backend.util.DBConnection;
import com.petclinic.backend.dao.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GroomingRecordDAO {

    public static final String CATEGORY_GROOMING = "Grooming";

    private final AppointmentDAO appointmentDAO = new AppointmentDAO();


    public GroomingRecord findByAppointmentId(int appointmentID) throws SQLException {
        String sql = BASE_SELECT + " WHERE gr.AppointmentID = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, appointmentID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public GroomingRecord findById(int recordID) throws SQLException {
        String sql = BASE_SELECT + " WHERE gr.RecordID = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, recordID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public List<GroomingRecord> findHistoryByPetId(int petID) throws SQLException {
        String sql = BASE_SELECT + " WHERE gr.PetID = ? ORDER BY gr.CreatedAt DESC";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, petID);
            try (ResultSet rs = ps.executeQuery()) {
                List<GroomingRecord> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        }
    }


    public List<Appointment> findGroomerQueue(int groomerID, LocalDate date) throws SQLException {
        List<Appointment> assigned   = appointmentDAO.findStaffQueue(groomerID, date, CATEGORY_GROOMING, "GroomingRecords");
        List<Appointment> unassigned = appointmentDAO.findUnassignedArrived(date, CATEGORY_GROOMING, "GroomingRecords");
        return mergeDistinctById(assigned, unassigned);
    }


    public List<Appointment> findGroomerCompletedToday(int groomerID, LocalDate date) throws SQLException {
        return appointmentDAO.findStaffCompletedToday(groomerID, date, CATEGORY_GROOMING, "GroomingRecords");
    }

    private List<Appointment> mergeDistinctById(List<Appointment> base, List<Appointment> extra) {
        Set<Integer> ids = new HashSet<>();
        List<Appointment> result = new ArrayList<>();
        for (Appointment a : base) {
            if (ids.add(a.getAppointmentID())) result.add(a);
        }
        for (Appointment a : extra) {
            if (ids.add(a.getAppointmentID())) result.add(a);
        }
        return result;
    }

    public int save(GroomingRecord rec) throws SQLException {
        String sql = """
                INSERT INTO GroomingRecords
                    (AppointmentID, PetID, GroomerID, CoatCondition, Behavior,
                     ProductsUsed, Notes, FlagForVet, FlagReason)
                VALUES (?,?,?,?,?,?,?,?,?)
                """;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, rec.getAppointmentID());
            ps.setInt(2, rec.getPetID());
            ps.setInt(3, rec.getGroomerID());
            ps.setString(4, rec.getCoatCondition());
            ps.setString(5, rec.getBehavior());
            ps.setString(6, rec.getProductsUsed());
            ps.setString(7, rec.getNotes());
            ps.setBoolean(8, rec.isFlagForVet());
            ps.setString(9, rec.isFlagForVet() ? rec.getFlagReason() : null);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Failed to save GroomingRecord.");
    }


    private static final String BASE_SELECT = """
            SELECT gr.*,
                   p.Name      AS PetName,
                   cu.FullName AS OwnerName,
                   st.FullName AS GroomerName,
                   (
                     SELECT TOP 1 s2.Name
                     FROM AppointmentServices aps2
                     JOIN Services s2 ON s2.ServiceID = aps2.ServiceID
                     JOIN ServiceCategories sc2 ON sc2.CategoryID = s2.CategoryID
                     WHERE aps2.AppointmentID = gr.AppointmentID AND sc2.Name = 'Grooming'
                     ORDER BY aps2.AppointmentServiceID
                   ) AS ServiceName
            FROM GroomingRecords gr
            JOIN Pets      p  ON p.PetID       = gr.PetID
            JOIN Customers cu ON cu.CustomerID = (SELECT CustomerID FROM Pets WHERE PetID = gr.PetID)
            JOIN Staff     st ON st.StaffID    = gr.GroomerID
            """;

    public List<GroomingRecord> findByPet(int petId) throws SQLException {
        String sql = "SELECT gr.*, s.FullName AS GroomerName "
                + "FROM GroomingRecords gr "
                + "JOIN Staff s ON gr.GroomerID = s.StaffID "
                + "WHERE gr.PetID = ? order by gr.AppointmentID desc";
        List<GroomingRecord> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, petId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    private GroomingRecord mapRow(ResultSet rs) throws SQLException {
        GroomingRecord r = new GroomingRecord();
        r.setRecordID(rs.getInt("RecordID"));
        r.setAppointmentID(rs.getInt("AppointmentID"));
        r.setPetID(rs.getInt("PetID"));
        r.setGroomerID(rs.getInt("GroomerID"));
        r.setCoatCondition(rs.getString("CoatCondition"));
        r.setBehavior(rs.getString("Behavior"));
        r.setProductsUsed(rs.getString("ProductsUsed"));
        r.setNotes(rs.getString("Notes"));
        r.setFlagForVet(rs.getBoolean("FlagForVet"));
        r.setFlagReason(rs.getString("FlagReason"));
        Timestamp ts = rs.getTimestamp("CreatedAt");
        if (ts != null) r.setCreatedAt(ts.toLocalDateTime());
        try { r.setPetName(rs.getString("PetName"));         } catch (SQLException ignored) {}
        try { r.setOwnerName(rs.getString("OwnerName"));     } catch (SQLException ignored) {}
        try { r.setGroomerName(rs.getString("GroomerName")); } catch (SQLException ignored) {}
        try { r.setServiceName(rs.getString("ServiceName")); } catch (SQLException ignored) {}
        return r;
    }
}