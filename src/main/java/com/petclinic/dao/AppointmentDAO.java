package com.petclinic.dao;

import com.petclinic.model.Appointment;
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
                + "(CustomerID, PetID, ServiceID, AppointmentDate, StartTime, EndTime, Status, SlotShift) "
                + "VALUES (?, ?, ?, ?, ?, ?, 'Pending', ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, a.getCustomerID());
            ps.setInt(2, a.getPetID());
            ps.setInt(3, a.getServiceID());
            ps.setDate(4, Date.valueOf(a.getAppointmentDate()));
            ps.setTime(5, Time.valueOf(a.getStartTime()));
            ps.setTime(6, Time.valueOf(a.getEndTime()));
            if (a.getSlotShift() != null) ps.setInt(7, a.getSlotShift());
            else ps.setNull(7, Types.TINYINT);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    public void updateStatus(int appointmentId, String status) throws SQLException {
        String sql = "UPDATE Appointments SET Status = ? WHERE AppointmentID = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, appointmentId);
            ps.executeUpdate();
        }
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public Appointment findById(int appointmentId) throws SQLException {
        String sql = "SELECT a.*, p.Name AS PetName, s.Name AS ServiceName, "
                + "sc.Name AS CategoryName, st.FullName AS staffName "
                + "FROM Appointments a "
                + "JOIN Pets p  ON a.PetID     = p.PetID "
                + "JOIN Services s ON a.ServiceID = s.ServiceID "
                + "JOIN ServiceCategories sc ON s.CategoryID = sc.CategoryID "
                + "LEFT JOIN Staff st ON a.AssignedStaffID = st.StaffID "
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
                + "sc.Name AS CategoryName, st.FullName AS staffName "
                + "FROM Appointments a "
                + "JOIN Pets p ON a.PetID = p.PetID "
                + "JOIN Services s ON a.ServiceID = s.ServiceID "
                + "JOIN ServiceCategories sc ON s.CategoryID = sc.CategoryID "
                + "LEFT JOIN Staff st ON a.AssignedStaffID = st.StaffID "
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

    public List<Appointment> findByPet(int petId) throws SQLException {
        String sql = "SELECT a.*, p.Name AS PetName, s.Name AS ServiceName, "
                + "sc.Name AS CategoryName, st.FullName AS staffName "
                + "FROM Appointments a "
                + "JOIN Pets p ON a.PetID = p.PetID "
                + "JOIN Services s ON a.ServiceID = s.ServiceID "
                + "JOIN ServiceCategories sc ON s.CategoryID = sc.CategoryID "
                + "LEFT JOIN Staff st ON a.AssignedStaffID = st.StaffID "
                + "WHERE a.PetID = ? ORDER BY a.AppointmentDate DESC, a.StartTime DESC";
        List<Appointment> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, petId);
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


    /**
     * Count confirmed appointments for ALL services in the same role-group
     * (Groomer or Vet) trong CÙNG 1 ca (SlotShift) của ngày đó.
     * Dùng SlotShift (cột mới) thay vì so khoảng StartTime/EndTime — khớp
     * đúng cách DB mới xác định "ca" (vd: appointment walk-in giờ lệch
     * nhưng vẫn có SlotShift rõ ràng), và tránh luôn được lỗi JDBC
     * time-vs-datetime vì không còn bind tham số TIME nào ở đây.
     */
    public int countConfirmedInSlotByRoleGroup(LocalDate date, int slotShift, int categoryId)
            throws SQLException {
        int roleId = com.petclinic.dao.ServiceDAO.roleIdForCategory(categoryId);
        String sql = "SELECT COUNT(*) FROM Appointments a "
                + "JOIN Services s ON a.ServiceID = s.ServiceID "
                + "WHERE a.AppointmentDate = ? "
                + "AND a.SlotShift = ? "
                + "AND a.Status IN ('Confirmed') "
                // Grooming = CategoryID 3 (phải khớp BookingService.GROOMING_CATEGORY_ID).
                + "AND (CASE WHEN s.CategoryID = 3 THEN 4 ELSE 3 END) = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            ps.setInt(2, slotShift);
            ps.setInt(3, roleId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Find active appointments (Pending/Confirmed) whose appointment date/time
     * has already passed — used by the overdue scheduler.
     */
    public List<Appointment> findOverdueActive() throws SQLException {
        String sql = "SELECT a.*, p.Name AS PetName, s.Name AS ServiceName, "
                + "sc.Name AS CategoryName, st.FullName AS staffName "
                + "FROM Appointments a "
                + "JOIN Pets p ON a.PetID = p.PetID "
                + "JOIN Services s ON a.ServiceID = s.ServiceID "
                + "JOIN ServiceCategories sc ON s.CategoryID = sc.CategoryID "
                + "LEFT JOIN Staff st ON a.AssignedStaffID = st.StaffID "
                + "WHERE a.Status IN ('Pending','Confirmed') "
                + "AND CAST(CAST(a.AppointmentDate AS DATE) AS DATETIME) "
                + "    + CAST(a.EndTime AS DATETIME) < GETDATE()";
        List<Appointment> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    /** Mark appointment as NoShow (Absent). */
    public void markNoShow(int appointmentId) throws SQLException {
        String sql = "UPDATE Appointments SET Status = 'NoShow' WHERE AppointmentID = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, appointmentId);
            ps.executeUpdate();
        }
    }

    /** Gán Vet/Groomer cho appointment (dùng cho auto-assign khi chuyển Confirmed). */
    public void assignStaff(int appointmentId, int staffId) throws SQLException {
        String sql = "UPDATE Appointments SET AssignedStaffID = ? WHERE AppointmentID = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, staffId);
            ps.setInt(2, appointmentId);
            ps.executeUpdate();
        }
    }

    /** Find all customers with overdue appointments (for email join). */
    public List<Appointment> findOverdueOlderThan24h() throws SQLException {
        String sql = "SELECT a.*, p.Name AS PetName, s.Name AS ServiceName, "
                + "sc.Name AS CategoryName, st.FullName AS staffName "
                + "FROM Appointments a "
                + "JOIN Pets p ON a.PetID = p.PetID "
                + "JOIN Services s ON a.ServiceID = s.ServiceID "
                + "JOIN ServiceCategories sc ON s.CategoryID = sc.CategoryID "
                + "LEFT JOIN Staff st ON a.AssignedStaffID = st.StaffID "
                + "WHERE a.Status IN ('Pending','Confirmed') "
                + "AND CAST(CAST(a.AppointmentDate AS DATE) AS DATETIME) "
                + "    + CAST(a.EndTime AS DATETIME) < DATEADD(HOUR, -24, GETDATE())";
        List<Appointment> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
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
    /** Cập nhật slot mới + SlotShift tương ứng, GIỮ NGUYÊN status hiện tại (không reset về Pending). */
    public void updateSlot(int appointmentId, LocalDate date, LocalTime start, LocalTime end)
            throws SQLException {
        String sql = "UPDATE Appointments SET AppointmentDate=?, StartTime=?, EndTime=?, SlotShift=? "
                + "WHERE AppointmentID=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            ps.setTime(2, Time.valueOf(start));
            ps.setTime(3, Time.valueOf(end));
            Integer shift = com.petclinic.service.BookingService.slotShiftOf(start);
            if (shift != null) ps.setInt(4, shift); else ps.setNull(4, Types.TINYINT);
            ps.setInt(5, appointmentId);
            ps.executeUpdate();
        }
    }

    /** Cancel: cập nhật status và lưu lý do huỷ vào CancelReason (KHÔNG đụng Notes gốc của khách). */
    public void cancel(int appointmentId, String reason) throws SQLException {
        String sql = "UPDATE Appointments SET Status='Cancelled', CancelReason=? "
                + "WHERE AppointmentID=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, reason);
            ps.setInt(2, appointmentId);
            ps.executeUpdate();
        }
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private Appointment mapRow(ResultSet rs) throws SQLException {
        Appointment a = mapRowSimple(rs);
        a.setPetName(rs.getString("PetName"));
        a.setServiceName(rs.getString("ServiceName"));
        a.setCategoryName(rs.getString("CategoryName"));
        a.setStaffName(rs.getString("staffName"));
        return a;
    }

    private Appointment mapRowSimple(ResultSet rs) throws SQLException {
        Appointment a = new Appointment();
        a.setAppointmentID(rs.getInt("AppointmentID"));
        a.setCustomerID(rs.getInt("CustomerID"));
        a.setPetID(rs.getInt("PetID"));
        a.setServiceID(rs.getInt("ServiceID"));
        int vid = rs.getInt("AssignedStaffID"); if (!rs.wasNull()) a.setAssignedStaffID(vid);
        a.setAppointmentDate(rs.getDate("AppointmentDate").toLocalDate());
        a.setStartTime(rs.getTime("StartTime").toLocalTime());
        a.setEndTime(rs.getTime("EndTime").toLocalTime());
        a.setStatus(rs.getString("Status"));
        a.setNotes(rs.getString("Notes"));
        a.setCancelReason(rs.getString("CancelReason"));
        int shift = rs.getInt("SlotShift"); if (!rs.wasNull()) a.setSlotShift(shift);
        return a;
    }
}