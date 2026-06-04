package com.petclinic.dao;

import com.petclinic.model.Appointment;
import com.petclinic.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for the Appointments table.
 * Follows the same pattern as CustomerDAO: one Connection per method, try-with-resources.
 */
public class AppointmentDAO {

    // ── Receptionist: check-in queries ───────────────────────────────────────

    /**
     * Find appointments for today that are in Confirmed status,
     * joined with customer/pet/service/vet names for the check-in screen.
     */
    public List<Appointment> findTodayConfirmed() throws SQLException {
        String sql = """
                SELECT a.AppointmentID, a.CustomerID, a.PetID, a.ServiceID,
                       a.AssignedVetID, a.AppointmentDate, a.StartTime, a.EndTime, a.Status,
                       c.FullName  AS CustomerName,
                       p.Name      AS PetName,
                       s.Name      AS ServiceName,
                       st.FullName AS VetName
                FROM Appointments a
                JOIN Customers        c  ON c.CustomerID  = a.CustomerID
                JOIN Pets             p  ON p.PetID        = a.PetID
                JOIN Services         s  ON s.ServiceID    = a.ServiceID
                LEFT JOIN Staff       st ON st.StaffID     = a.AssignedVetID
                WHERE a.AppointmentDate = CAST(GETDATE() AS DATE)
                  AND a.Status = 'Confirmed'
                ORDER BY a.StartTime
                """;
        return queryList(sql);
    }

    /**
     * Search confirmed appointments by owner name or pet name (for check-in search box).
     */
    public List<Appointment> searchForCheckIn(String keyword) throws SQLException {
        String sql = """
                SELECT a.AppointmentID, a.CustomerID, a.PetID, a.ServiceID,
                       a.AssignedVetID, a.AppointmentDate, a.StartTime, a.EndTime, a.Status,
                       c.FullName  AS CustomerName,
                       p.Name      AS PetName,
                       s.Name      AS ServiceName,
                       st.FullName AS VetName
                FROM Appointments a
                JOIN Customers        c  ON c.CustomerID  = a.CustomerID
                JOIN Pets             p  ON p.PetID        = a.PetID
                JOIN Services         s  ON s.ServiceID    = a.ServiceID
                LEFT JOIN Staff       st ON st.StaffID     = a.AssignedVetID
                WHERE a.AppointmentDate = CAST(GETDATE() AS DATE)
                  AND a.Status = 'Confirmed'
                  AND (c.FullName LIKE ? OR p.Name LIKE ?)
                ORDER BY a.StartTime
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String like = "%" + keyword.trim() + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            return mapList(ps.executeQuery());
        }
    }

    // ── Veterinarian: queue queries ───────────────────────────────────────────

    /**
     * All appointments assigned to a vet today with status Arrived or InProgress.
     */
    public List<Appointment> findVetQueueToday(int vetID) throws SQLException {
        String sql = """
                SELECT a.AppointmentID, a.CustomerID, a.PetID, a.ServiceID,
                       a.AssignedVetID, a.AppointmentDate, a.StartTime, a.EndTime, a.Status,
                       c.FullName  AS CustomerName,
                       p.Name      AS PetName,
                       s.Name      AS ServiceName,
                       st.FullName AS VetName
                FROM Appointments a
                JOIN Customers        c  ON c.CustomerID  = a.CustomerID
                JOIN Pets             p  ON p.PetID        = a.PetID
                JOIN Services         s  ON s.ServiceID    = a.ServiceID
                LEFT JOIN Staff       st ON st.StaffID     = a.AssignedVetID
                WHERE a.AppointmentDate = CAST(GETDATE() AS DATE)
                  AND a.AssignedVetID   = ?
                  AND a.Status IN ('Arrived', 'InProgress')
                ORDER BY
                  CASE a.Status WHEN 'InProgress' THEN 0 ELSE 1 END,
                  a.StartTime
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vetID);
            return mapList(ps.executeQuery());
        }
    }

    // ── Lookup ────────────────────────────────────────────────────────────────

    public Appointment findById(int appointmentID) throws SQLException {
        String sql = """
                SELECT a.AppointmentID, a.CustomerID, a.PetID, a.ServiceID,
                       a.AssignedVetID, a.AppointmentDate, a.StartTime, a.EndTime, a.Status,
                       c.FullName  AS CustomerName,
                       p.Name      AS PetName,
                       s.Name      AS ServiceName,
                       st.FullName AS VetName
                FROM Appointments a
                JOIN Customers        c  ON c.CustomerID  = a.CustomerID
                JOIN Pets             p  ON p.PetID        = a.PetID
                JOIN Services         s  ON s.ServiceID    = a.ServiceID
                LEFT JOIN Staff       st ON st.StaffID     = a.AssignedVetID
                WHERE a.AppointmentID = ?
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, appointmentID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    // ── Status update ─────────────────────────────────────────────────────────

    /**
     * Update appointment status.
     * Valid transitions enforced by service layer:
     * Confirmed → Arrived      (check-in by receptionist)
     * Arrived   → InProgress   (vet starts examination)
     * InProgress → Done        (vet completes examination)
     * Confirmed/Arrived → NoShow
     */
    public void updateStatus(int appointmentID, String newStatus) throws SQLException {
        String sql = "UPDATE Appointments SET Status = ? WHERE AppointmentID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, appointmentID);
            ps.executeUpdate();
        }
    }

    /**
     * Assign a vet to an appointment (receptionist action during check-in).
     */
    public void assignVet(int appointmentID, int vetID) throws SQLException {
        String sql = "UPDATE Appointments SET AssignedVetID = ? WHERE AppointmentID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vetID);
            ps.setInt(2, appointmentID);
            ps.executeUpdate();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<Appointment> queryList(String sql) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return mapList(rs);
        }
    }

    private List<Appointment> mapList(ResultSet rs) throws SQLException {
        List<Appointment> list = new ArrayList<>();
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    private Appointment mapRow(ResultSet rs) throws SQLException {
        Appointment a = new Appointment();
        a.setAppointmentID(rs.getInt("AppointmentID"));
        a.setCustomerID(rs.getInt("CustomerID"));
        a.setPetID(rs.getInt("PetID"));
        a.setServiceID(rs.getInt("ServiceID"));
        int vetID = rs.getInt("AssignedVetID");
        a.setAssignedVetID(rs.wasNull() ? null : vetID);
        Date d = rs.getDate("AppointmentDate");
        if (d != null) a.setAppointmentDate(d.toLocalDate());
        Time st = rs.getTime("StartTime");
        if (st != null) a.setStartTime(st.toLocalTime());
        Time et = rs.getTime("EndTime");
        if (et != null) a.setEndTime(et.toLocalTime());
        a.setStatus(rs.getString("Status"));
        // join fields (may be null if query did not include them)
        try {
            a.setCustomerName(rs.getString("CustomerName"));
        } catch (SQLException ignored) {
        }
        try {
            a.setPetName(rs.getString("PetName"));
        } catch (SQLException ignored) {
        }
        try {
            a.setServiceName(rs.getString("ServiceName"));
        } catch (SQLException ignored) {
        }
        try {
            a.setVetName(rs.getString("VetName"));
        } catch (SQLException ignored) {
        }
        return a;
    }
}