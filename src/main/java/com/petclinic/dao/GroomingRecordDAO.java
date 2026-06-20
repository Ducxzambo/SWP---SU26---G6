package com.petclinic.dao;

import com.petclinic.model.GroomingRecord;
import com.petclinic.util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class GroomingRecordDAO {

    // ── Read ──────────────────────────────────────────────────────────────────

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

    /** Full spa history for a pet (groomer view before session). */
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

    // ── Groomer queue ─────────────────────────────────────────────────────────

    /**
     * Sessions available for a groomer:
     * - Assigned to this groomer (Arrived/InProgress)
     * - OR unassigned (AssignedGroomerID IS NULL, Arrived)
     * Both filtered to grooming services only.
     */
    public List<com.petclinic.model.Appointment> findGroomerQueue(int groomerID, LocalDate date)
            throws SQLException {
        String sql = """
                SELECT a.AppointmentID, a.CustomerID, a.PetID, a.ServiceID,
                       a.AssignedVetID, a.AssignedGroomerID,
                       a.AppointmentDate, a.StartTime, a.EndTime, a.Status, a.SlotShift,
                       c.FullName  AS CustomerName,
                       p.Name      AS PetName,
                       s.Name      AS ServiceName,
                       st.FullName AS GroomerName
                FROM Appointments a
                JOIN Customers c ON c.CustomerID = a.CustomerID
                JOIN Pets      p ON p.PetID       = a.PetID
                JOIN Services  s ON s.ServiceID   = a.ServiceID
                JOIN ServiceCategories sc ON sc.CategoryID = s.CategoryID
                LEFT JOIN Staff st ON st.StaffID = a.AssignedGroomerID
                WHERE a.AppointmentDate = ?
                  AND sc.Name = 'Grooming'
                  AND a.Status IN ('Arrived','InProgress')
                  AND (a.AssignedGroomerID = ? OR a.AssignedGroomerID IS NULL)
                ORDER BY
                  CASE a.Status WHEN 'InProgress' THEN 0 ELSE 1 END,
                  a.StartTime
                """;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            ps.setInt(2, groomerID);
            List<com.petclinic.model.Appointment> list = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    com.petclinic.model.Appointment a = new com.petclinic.model.Appointment();
                    a.setAppointmentID(rs.getInt("AppointmentID"));
                    a.setCustomerID(rs.getInt("CustomerID"));
                    a.setPetID(rs.getInt("PetID"));
                    a.setServiceID(rs.getInt("ServiceID"));
                    int gid = rs.getInt("AssignedGroomerID");
                    a.setAssignedGroomerID(rs.wasNull() ? null : gid);
                    Date d = rs.getDate("AppointmentDate"); if (d != null) a.setAppointmentDate(d.toLocalDate());
                    Time st = rs.getTime("StartTime");      if (st != null) a.setStartTime(st.toLocalTime());
                    Time et = rs.getTime("EndTime");        if (et != null) a.setEndTime(et.toLocalTime());
                    a.setStatus(rs.getString("Status"));
                    try { int sh = rs.getInt("SlotShift"); if (!rs.wasNull()) a.setSlotShift(sh); } catch (SQLException ignored){}
                    a.setCustomerName(rs.getString("CustomerName"));
                    a.setPetName(rs.getString("PetName"));
                    a.setServiceName(rs.getString("ServiceName"));
                    try { a.setGroomerName(rs.getString("GroomerName")); } catch (SQLException ignored){}
                    list.add(a);
                }
            }
            return list;
        }
    }

    // ── Write ─────────────────────────────────────────────────────────────────

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

    // ── Assign groomer ────────────────────────────────────────────────────────

    public void assignGroomer(int appointmentID, int groomerID) throws SQLException {
        String sql = "UPDATE Appointments SET AssignedGroomerID = ? WHERE AppointmentID = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, groomerID); ps.setInt(2, appointmentID);
            ps.executeUpdate();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static final String BASE_SELECT = """
            SELECT gr.*,
                   p.Name      AS PetName,
                   cu.FullName AS OwnerName,
                   st.FullName AS GroomerName,
                   s.Name      AS ServiceName
            FROM GroomingRecords gr
            JOIN Pets      p  ON p.PetID       = gr.PetID
            JOIN Customers cu ON cu.CustomerID = (SELECT CustomerID FROM Pets WHERE PetID = gr.PetID)
            JOIN Staff     st ON st.StaffID    = gr.GroomerID
            JOIN Appointments a ON a.AppointmentID = gr.AppointmentID
            JOIN Services   s  ON s.ServiceID  = a.ServiceID
            """;

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
        try { r.setPetName(rs.getString("PetName"));       } catch (SQLException ignored){}
        try { r.setOwnerName(rs.getString("OwnerName"));   } catch (SQLException ignored){}
        try { r.setGroomerName(rs.getString("GroomerName")); } catch (SQLException ignored){}
        try { r.setServiceName(rs.getString("ServiceName")); } catch (SQLException ignored){}
        return r;
    }
}