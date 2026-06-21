package com.petclinic.dao;

import com.petclinic.model.Appointment;
import com.petclinic.util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class AppointmentDAO {

    public static final int MAX_PER_SHIFT = 10;

    // ── Shift helpers ─────────────────────────────────────────────────────────
    public static int shiftOf(LocalTime t) {
        if (!t.isBefore(LocalTime.of(8,0))   && t.isBefore(LocalTime.of(10,0)))  return 1;
        if (!t.isBefore(LocalTime.of(10,0))  && t.isBefore(LocalTime.of(12,0)))  return 2;
        if (!t.isBefore(LocalTime.of(13,30)) && t.isBefore(LocalTime.of(15,30))) return 3;
        if (!t.isBefore(LocalTime.of(15,30)) && t.isBefore(LocalTime.of(17,30))) return 4;
        return -1;
    }

    public static LocalTime shiftStart(int shift) {
        return switch (shift) {
            case 1 -> LocalTime.of(8,0);
            case 2 -> LocalTime.of(10,0);
            case 3 -> LocalTime.of(13,30);
            case 4 -> LocalTime.of(15,30);
            default -> throw new IllegalArgumentException("Invalid shift: " + shift);
        };
    }

    public static LocalTime shiftEnd(int shift) {
        return switch (shift) {
            case 1 -> LocalTime.of(10,0);
            case 2 -> LocalTime.of(12,0);
            case 3 -> LocalTime.of(15,30);
            case 4 -> LocalTime.of(17,30);
            default -> throw new IllegalArgumentException("Invalid shift: " + shift);
        };
    }

    public static String shiftLabel(int shift) {
        return switch (shift) {
            case 1 -> "Ca 1 (08:00-10:00)";
            case 2 -> "Ca 2 (10:00-12:00)";
            case 3 -> "Ca 3 (13:30-15:30)";
            case 4 -> "Ca 4 (15:30-17:30)";
            default -> "Ngoai gio";
        };
    }

    // ── Slot count ────────────────────────────────────────────────────────────
    public int countSlotBookings(LocalDate date, int shift) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Appointments WHERE AppointmentDate=? AND SlotShift=? AND Status NOT IN ('Cancelled','NoShow')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            ps.setInt(2, shift);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    public boolean isSlotFull(LocalDate date, int shift) throws SQLException {
        return countSlotBookings(date, shift) >= MAX_PER_SHIFT;
    }

    // ── Check-in queries (Examination / BP-02) ────────────────────────────────
    public List<Appointment> findConfirmedByDate(LocalDate date, Integer shift) throws SQLException {
        String extra = (shift != null) ? "AND a.SlotShift = ? " : "";
        String sql = "SELECT a.AppointmentID,a.CustomerID,a.PetID,a.ServiceID,a.AssignedVetID,a.AssignedGroomerID," +
                "a.AppointmentDate,a.StartTime,a.EndTime,a.Status,a.SlotShift," +
                "c.FullName AS CustomerName,p.Name AS PetName,s.Name AS ServiceName," +
                "st.FullName AS VetName, gst.FullName AS GroomerName " +
                "FROM Appointments a " +
                "JOIN Customers c ON c.CustomerID=a.CustomerID " +
                "JOIN Pets p ON p.PetID=a.PetID " +
                "JOIN Services s ON s.ServiceID=a.ServiceID " +
                "LEFT JOIN Staff st  ON st.StaffID=a.AssignedVetID " +
                "LEFT JOIN Staff gst ON gst.StaffID=a.AssignedGroomerID " +
                "WHERE a.AppointmentDate=? AND a.Status='Confirmed' " + extra +
                "ORDER BY a.SlotShift,a.StartTime";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            if (shift != null) ps.setInt(2, shift);
            return mapList(ps.executeQuery());
        }
    }

    public List<Appointment> searchForCheckIn(String keyword, LocalDate date) throws SQLException {
        String sql = "SELECT a.AppointmentID,a.CustomerID,a.PetID,a.ServiceID,a.AssignedVetID,a.AssignedGroomerID," +
                "a.AppointmentDate,a.StartTime,a.EndTime,a.Status,a.SlotShift," +
                "c.FullName AS CustomerName,p.Name AS PetName,s.Name AS ServiceName," +
                "st.FullName AS VetName, gst.FullName AS GroomerName " +
                "FROM Appointments a " +
                "JOIN Customers c ON c.CustomerID=a.CustomerID " +
                "JOIN Pets p ON p.PetID=a.PetID " +
                "JOIN Services s ON s.ServiceID=a.ServiceID " +
                "LEFT JOIN Staff st  ON st.StaffID=a.AssignedVetID " +
                "LEFT JOIN Staff gst ON gst.StaffID=a.AssignedGroomerID " +
                "WHERE a.AppointmentDate=? AND a.Status='Confirmed' AND (c.FullName LIKE ? OR p.Name LIKE ?) " +
                "ORDER BY a.SlotShift,a.StartTime";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            String like = "%" + keyword.trim() + "%";
            ps.setString(2, like); ps.setString(3, like);
            return mapList(ps.executeQuery());
        }
    }

    // ── Check-in queries (Grooming / BP-03) ────────────────────────────────────

    /**
     * Confirmed grooming appointments for a date, optionally filtered by shift.
     * Only includes appointments whose Service belongs to the 'Grooming' category.
     */
    public List<Appointment> findGroomingConfirmedByDate(LocalDate date, Integer shift) throws SQLException {
        String extra = (shift != null) ? "AND a.SlotShift = ? " : "";
        String sql = "SELECT a.AppointmentID,a.CustomerID,a.PetID,a.ServiceID,a.AssignedVetID,a.AssignedGroomerID," +
                "a.AppointmentDate,a.StartTime,a.EndTime,a.Status,a.SlotShift," +
                "c.FullName AS CustomerName,p.Name AS PetName,s.Name AS ServiceName," +
                "st.FullName AS VetName, gst.FullName AS GroomerName " +
                "FROM Appointments a " +
                "JOIN Customers c ON c.CustomerID=a.CustomerID " +
                "JOIN Pets p ON p.PetID=a.PetID " +
                "JOIN Services s ON s.ServiceID=a.ServiceID " +
                "JOIN ServiceCategories sc ON sc.CategoryID=s.CategoryID " +
                "LEFT JOIN Staff st  ON st.StaffID=a.AssignedVetID " +
                "LEFT JOIN Staff gst ON gst.StaffID=a.AssignedGroomerID " +
                "WHERE a.AppointmentDate=? AND a.Status='Confirmed' AND sc.Name='Grooming' " + extra +
                "ORDER BY a.SlotShift,a.StartTime";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            if (shift != null) ps.setInt(2, shift);
            return mapList(ps.executeQuery());
        }
    }

    // ── Walk-in ───────────────────────────────────────────────────────────────
    public int createWalkIn(int customerID, int petID, int serviceID, int vetID) throws SQLException {
        LocalDate today = LocalDate.now();
        LocalTime now   = LocalTime.now();
        int shift = shiftOf(now); if (shift == -1) shift = 1;
        LocalTime end = now.plusMinutes(30);
        String sql = "INSERT INTO Appointments(CustomerID,PetID,ServiceID,AssignedVetID,AppointmentDate,StartTime,EndTime,SlotShift,Status) VALUES(?,?,?,?,?,?,?,?,'Arrived')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1,customerID); ps.setInt(2,petID); ps.setInt(3,serviceID); ps.setInt(4,vetID);
            ps.setDate(5,Date.valueOf(today)); ps.setTime(6,Time.valueOf(now)); ps.setTime(7,Time.valueOf(end));
            ps.setInt(8,shift);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { if (keys.next()) return keys.getInt(1); }
        }
        throw new SQLException("Failed to create walk-in.");
    }

    // ── Vet queue (BP-02) ───────────────────────────────────────────────────────
    public List<Appointment> findVetQueue(int vetID, LocalDate date) throws SQLException {
        String sql = "SELECT a.AppointmentID,a.CustomerID,a.PetID,a.ServiceID,a.AssignedVetID,a.AssignedGroomerID," +
                "a.AppointmentDate,a.StartTime,a.EndTime,a.Status,a.SlotShift," +
                "c.FullName AS CustomerName,p.Name AS PetName,s.Name AS ServiceName," +
                "st.FullName AS VetName, gst.FullName AS GroomerName " +
                "FROM Appointments a " +
                "JOIN Customers c ON c.CustomerID=a.CustomerID " +
                "JOIN Pets p ON p.PetID=a.PetID " +
                "JOIN Services s ON s.ServiceID=a.ServiceID " +
                "LEFT JOIN Staff st  ON st.StaffID=a.AssignedVetID " +
                "LEFT JOIN Staff gst ON gst.StaffID=a.AssignedGroomerID " +
                "WHERE a.AppointmentDate=? AND a.AssignedVetID=? AND a.Status IN('Arrived','InProgress') " +
                "ORDER BY CASE a.Status WHEN 'InProgress' THEN 0 ELSE 1 END,a.SlotShift,a.StartTime";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date)); ps.setInt(2, vetID);
            return mapList(ps.executeQuery());
        }
    }

    // ── Lookup ────────────────────────────────────────────────────────────────
    public Appointment findById(int appointmentID) throws SQLException {
        String sql = "SELECT a.AppointmentID,a.CustomerID,a.PetID,a.ServiceID,a.AssignedVetID,a.AssignedGroomerID," +
                "a.AppointmentDate,a.StartTime,a.EndTime,a.Status,a.SlotShift," +
                "c.FullName AS CustomerName,p.Name AS PetName,s.Name AS ServiceName," +
                "st.FullName AS VetName, gst.FullName AS GroomerName " +
                "FROM Appointments a " +
                "JOIN Customers c ON c.CustomerID=a.CustomerID " +
                "JOIN Pets p ON p.PetID=a.PetID " +
                "JOIN Services s ON s.ServiceID=a.ServiceID " +
                "LEFT JOIN Staff st  ON st.StaffID=a.AssignedVetID " +
                "LEFT JOIN Staff gst ON gst.StaffID=a.AssignedGroomerID " +
                "WHERE a.AppointmentID=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, appointmentID);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? mapRow(rs) : null; }
        }
    }

    public void updateStatus(int appointmentID, String newStatus) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE Appointments SET Status=? WHERE AppointmentID=?")) {
            ps.setString(1, newStatus); ps.setInt(2, appointmentID); ps.executeUpdate();
        }
    }

    public void assignVet(int appointmentID, int vetID) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE Appointments SET AssignedVetID=? WHERE AppointmentID=?")) {
            ps.setInt(1, vetID); ps.setInt(2, appointmentID); ps.executeUpdate();
        }
    }

    public void assignGroomer(int appointmentID, int groomerID) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE Appointments SET AssignedGroomerID=? WHERE AppointmentID=?")) {
            ps.setInt(1, groomerID); ps.setInt(2, appointmentID); ps.executeUpdate();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
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
        int groomerID = rs.getInt("AssignedGroomerID");
        a.setAssignedGroomerID(rs.wasNull() ? null : groomerID);
        Date d = rs.getDate("AppointmentDate"); if (d != null) a.setAppointmentDate(d.toLocalDate());
        Time st = rs.getTime("StartTime");       if (st != null) a.setStartTime(st.toLocalTime());
        Time et = rs.getTime("EndTime");         if (et != null) a.setEndTime(et.toLocalTime());
        a.setStatus(rs.getString("Status"));
        try { int sh = rs.getInt("SlotShift"); if (!rs.wasNull()) a.setSlotShift(sh); } catch (SQLException ignored) {}
        try { a.setCustomerName(rs.getString("CustomerName")); } catch (SQLException ignored) {}
        try { a.setPetName(rs.getString("PetName"));           } catch (SQLException ignored) {}
        try { a.setServiceName(rs.getString("ServiceName"));   } catch (SQLException ignored) {}
        try { a.setVetName(rs.getString("VetName"));           } catch (SQLException ignored) {}
        try { a.setGroomerName(rs.getString("GroomerName"));   } catch (SQLException ignored) {}
        return a;
    }
}