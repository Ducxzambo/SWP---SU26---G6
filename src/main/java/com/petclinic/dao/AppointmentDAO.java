package com.petclinic.dao;

import com.petclinic.model.Appointment;
import com.petclinic.model.StaffAvailability;
import com.petclinic.util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class AppointmentDAO {

    // ── Insert ────────────────────────────────────────────────────────────────

    public int insert(Appointment a) throws SQLException {
        String sql = "INSERT INTO Appointments "
                + "(CustomerID, PetID, ServiceID, AppointmentDate, StartTime, EndTime, Status) "
                + "VALUES (?, ?, ?, ?, ?, ?, 'Pending')";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, a.getCustomerID());
            ps.setInt(2, a.getPetID());
            ps.setInt(3, a.getServiceID());
            ps.setDate(4, Date.valueOf(a.getAppointmentDate()));
            ps.setTime(5, Time.valueOf(a.getStartTime()));
            ps.setTime(6, Time.valueOf(a.getEndTime()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public Appointment findById(int appointmentId) throws SQLException {
        String sql = "SELECT a.*, p.Name AS PetName, s.Name AS ServiceName, "
                + "sc.Name AS CategoryName, st.FullName AS VetName "
                + "FROM Appointments a "
                + "JOIN Pets p  ON a.PetID     = p.PetID "
                + "JOIN Services s ON a.ServiceID = s.ServiceID "
                + "JOIN ServiceCategories sc ON s.CategoryID = sc.CategoryID "
                + "LEFT JOIN Staff st ON a.AssignedVetID = st.StaffID "
                + "WHERE a.AppointmentID = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, appointmentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public List<Appointment> findByCustomer(int customerId) throws SQLException {
        String sql = "SELECT a.*, p.Name AS PetName, s.Name AS ServiceName, "
                + "sc.Name AS CategoryName, st.FullName AS VetName "
                + "FROM Appointments a "
                + "JOIN Pets p ON a.PetID = p.PetID "
                + "JOIN Services s ON a.ServiceID = s.ServiceID "
                + "JOIN ServiceCategories sc ON s.CategoryID = sc.CategoryID "
                + "LEFT JOIN Staff st ON a.AssignedVetID = st.StaffID "
                + "WHERE a.CustomerID = ? ORDER BY a.AppointmentDate DESC, a.StartTime DESC";
        List<Appointment> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    /** All booked slots for a given date (for conflict detection). */
    public List<Appointment> findByDate(LocalDate date) throws SQLException {
        String sql = "SELECT * FROM Appointments "
                + "WHERE AppointmentDate = ? AND Status NOT IN ('Cancelled','NoShow')";
        List<Appointment> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRowSimple(rs));
            }
        }
        return list;
    }

    /** All booked slots for a date EXCLUDING a specific appointment (for reschedule). */
    public List<Appointment> findByDateExcluding(LocalDate date, int excludeId) throws SQLException {
        String sql = "SELECT * FROM Appointments "
                + "WHERE AppointmentDate = ? AND Status NOT IN ('Cancelled','NoShow') "
                + "AND AppointmentID <> ?";
        List<Appointment> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRowSimple(rs));
            }
        }
        return list;
    }

//    /** Count paid/confirmed appointments in a given 120-min slot window for a service. */
//    public int countConfirmedInSlot(LocalDate date, LocalTime start, LocalTime end, int serviceId)
//            throws SQLException {
//        String sql = "SELECT COUNT(*) FROM Appointments "
//                + "WHERE AppointmentDate = ? AND ServiceID = ? "
//                + "AND StartTime >= ? AND StartTime < ? "
//                + "AND Status IN ('Confirmed','InProgress','Done')";
//        try (Connection c = DBConnection.getConnection();
//             PreparedStatement ps = c.prepareStatement(sql)) {
//            ps.setDate(1, Date.valueOf(date));
//            ps.setInt(2, serviceId);
//            ps.setTime(3, Time.valueOf(start));
//            ps.setTime(4, Time.valueOf(end));
//            try (ResultSet rs = ps.executeQuery()) {
//                return rs.next() ? rs.getInt(1) : 0;
//            }
//        }
//    }

    // ── Updates ───────────────────────────────────────────────────────────────

    /** Count Confirmed appointments overlapping a time slot for capacity check. */
    public int countConfirmedInSlot(LocalDate date, LocalTime start, LocalTime end,
                                    int serviceId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Appointments "
                + "WHERE AppointmentDate = ? AND ServiceID = ? "
                + "AND Status = 'Confirmed' "
                + "AND StartTime < ? AND EndTime > ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            ps.setInt(2, serviceId);
            ps.setTime(3, Time.valueOf(end));
            ps.setTime(4, Time.valueOf(start));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** Update appointment Notes field (used for cancel reason). */
    public void updateNotes(int appointmentId, String notes) throws SQLException {
        String sql = "UPDATE Appointments SET Notes=? WHERE AppointmentID=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, notes);
            ps.setInt(2, appointmentId);
            ps.executeUpdate();
        }
    }
    public void updateSlot(int appointmentId, LocalDate date, LocalTime start, LocalTime end)
            throws SQLException {
        String sql = "UPDATE Appointments SET AppointmentDate=?, StartTime=?, EndTime=?, "
                + "Status='Pending' WHERE AppointmentID=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            ps.setTime(2, Time.valueOf(start));
            ps.setTime(3, Time.valueOf(end));
            ps.setInt(4, appointmentId);
            ps.executeUpdate();
        }
    }

    /** Cancel: update status and persist cancel reason in Notes column. */
    public void cancel(int appointmentId, String reason) throws SQLException {
        String sql = "UPDATE Appointments SET Status='Cancelled', Notes=? "
                + "WHERE AppointmentID=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, reason);
            ps.setInt(2, appointmentId);
            ps.executeUpdate();
        }
    }

    // ── Staff availability (RoleID = 3 = Vet) ────────────────────────────────

    public List<StaffAvailability> findVetAvailability() throws SQLException {
        String sql = "SELECT sa.* FROM StaffAvailability sa "
                + "JOIN Staff s ON sa.StaffID = s.StaffID "
                + "WHERE s.RoleID = 3 AND s.IsActive = 1 "
                + "ORDER BY sa.DayOfWeek, sa.StartTime";
        List<StaffAvailability> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                StaffAvailability sa = new StaffAvailability();
                sa.setAvailabilityID(rs.getInt("AvailabilityID"));
                sa.setStaffID(rs.getInt("StaffID"));
                sa.setDayOfWeek(rs.getInt("DayOfWeek"));
                sa.setStartTime(rs.getTime("StartTime").toLocalTime());
                sa.setEndTime(rs.getTime("EndTime").toLocalTime());
                list.add(sa);
            }
        }
        return list;
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private Appointment mapRow(ResultSet rs) throws SQLException {
        Appointment a = mapRowSimple(rs);
        a.setPetName(rs.getString("PetName"));
        a.setServiceName(rs.getString("ServiceName"));
        a.setCategoryName(rs.getString("CategoryName"));
        a.setVetName(rs.getString("VetName"));
        return a;
    }

    private Appointment mapRowSimple(ResultSet rs) throws SQLException {
        Appointment a = new Appointment();
        a.setAppointmentID(rs.getInt("AppointmentID"));
        a.setCustomerID(rs.getInt("CustomerID"));
        a.setPetID(rs.getInt("PetID"));
        a.setServiceID(rs.getInt("ServiceID"));
        int vid = rs.getInt("AssignedVetID"); if (!rs.wasNull()) a.setAssignedVetID(vid);
        a.setAppointmentDate(rs.getDate("AppointmentDate").toLocalDate());
        a.setStartTime(rs.getTime("StartTime").toLocalTime());
        a.setEndTime(rs.getTime("EndTime").toLocalTime());
        a.setStatus(rs.getString("Status"));
        a.setNotes(rs.getString("Notes"));
        return a;
    }
}