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
        String sql = "SELECT a.AppointmentID,a.CustomerID,a.PetID,a.ServiceID,a.AssignedStaffID," +
                "a.AppointmentDate,a.StartTime,a.EndTime,a.Status,a.SlotShift," +
                "c.FullName AS CustomerName,p.Name AS PetName,s.Name AS ServiceName," +
                "st.FullName AS StaffName " +
                "FROM Appointments a " +
                "JOIN Customers c ON c.CustomerID=a.CustomerID " +
                "JOIN Pets p ON p.PetID=a.PetID " +
                "JOIN Services s ON s.ServiceID=a.ServiceID " +
                "LEFT JOIN Staff st  ON st.StaffID=a.AssignedStaffID " +
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
        String sql = "SELECT a.AppointmentID,a.CustomerID,a.PetID,a.ServiceID,a.AssignedStaffID," +
                "a.AppointmentDate,a.StartTime,a.EndTime,a.Status,a.SlotShift," +
                "c.FullName AS CustomerName,p.Name AS PetName,s.Name AS ServiceName," +
                "st.FullName AS StaffName " +
                "FROM Appointments a " +
                "JOIN Customers c ON c.CustomerID=a.CustomerID " +
                "JOIN Pets p ON p.PetID=a.PetID " +
                "JOIN Services s ON s.ServiceID=a.ServiceID " +
                "LEFT JOIN Staff st  ON st.StaffID=a.AssignedStaffID " +
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
        String sql = "SELECT a.AppointmentID,a.CustomerID,a.PetID,a.ServiceID,a.AssignedStaffID," +
                "a.AppointmentDate,a.StartTime,a.EndTime,a.Status,a.SlotShift," +
                "c.FullName AS CustomerName,p.Name AS PetName,s.Name AS ServiceName," +
                "st.FullName AS StaffName " +
                "FROM Appointments a " +
                "JOIN Customers c ON c.CustomerID=a.CustomerID " +
                "JOIN Pets p ON p.PetID=a.PetID " +
                "JOIN Services s ON s.ServiceID=a.ServiceID " +
                "JOIN ServiceCategories sc ON sc.CategoryID=s.CategoryID " +
                "LEFT JOIN Staff st  ON st.StaffID=a.AssignedStaffID " +
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
        String sql = "INSERT INTO Appointments(CustomerID,PetID,ServiceID,AssignedStaffID,AppointmentDate,StartTime,EndTime,SlotShift,Status) VALUES(?,?,?,?,?,?,?,?,'Arrived')";
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
        String sql = "SELECT a.AppointmentID,a.CustomerID,a.PetID,a.ServiceID,a.AssignedStaffID," +
                "a.AppointmentDate,a.StartTime,a.EndTime,a.Status,a.SlotShift," +
                "c.FullName AS CustomerName,p.Name AS PetName,s.Name AS ServiceName," +
                "st.FullName AS StaffName " +
                "FROM Appointments a " +
                "JOIN Customers c ON c.CustomerID=a.CustomerID " +
                "JOIN Pets p ON p.PetID=a.PetID " +
                "JOIN Services s ON s.ServiceID=a.ServiceID " +
                "LEFT JOIN Staff st  ON st.StaffID=a.AssignedStaffID " +
                "WHERE a.AppointmentDate=? AND a.AssignedStaffID=? AND a.Status IN('Arrived','InProgress') " +
                "ORDER BY CASE a.Status WHEN 'InProgress' THEN 0 ELSE 1 END,a.SlotShift,a.StartTime";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date)); ps.setInt(2, vetID);
            return mapList(ps.executeQuery());
        }
    }

    public List<Appointment> findVetCompletedToday(int vetID, LocalDate date) throws SQLException {
        String sql = "SELECT a.AppointmentID,a.CustomerID,a.PetID,a.ServiceID,a.AssignedStaffID," +
                "a.AppointmentDate,a.StartTime,a.EndTime,a.Status,a.SlotShift," +
                "c.FullName AS CustomerName,p.Name AS PetName,s.Name AS ServiceName," +
                "st.FullName AS StaffName, mr.RecordID AS RecordID " +
                "FROM Appointments a " +
                "JOIN Customers c ON c.CustomerID=a.CustomerID " +
                "JOIN Pets p ON p.PetID=a.PetID " +
                "JOIN Services s ON s.ServiceID=a.ServiceID " +
                "LEFT JOIN Staff st  ON st.StaffID=a.AssignedStaffID " +
                "LEFT JOIN MedicalRecords mr ON mr.AppointmentID = a.AppointmentID " +
                "WHERE a.AppointmentDate=? AND a.AssignedStaffID=? AND a.Status='Done' " +
                "ORDER BY a.SlotShift, a.StartTime";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date)); ps.setInt(2, vetID);
            List<Appointment> list = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Appointment a = mapRow(rs);
                    int recId = rs.getInt("RecordID");
                    a.setRecordID(rs.wasNull() ? null : recId);
                    list.add(a);
                }
            }
            return list;
        }
    }


    public void assignVet(int appointmentID, int vetID) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE Appointments SET AssignedStaffID=? WHERE AppointmentID=?")) {
            ps.setInt(1, vetID); ps.setInt(2, appointmentID); ps.executeUpdate();
        }
    }

    public void assignGroomer(int appointmentID, int groomerID) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE Appointments SET AssignedStaffID=? WHERE AppointmentID=?")) {
            ps.setInt(1, groomerID); ps.setInt(2, appointmentID); ps.executeUpdate();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private List<Appointment> mapList(ResultSet rs) throws SQLException {
        List<Appointment> list = new ArrayList<>();
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }


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
        Appointment a = new Appointment();
        a.setAppointmentID(rs.getInt("AppointmentID"));
        a.setCustomerID(rs.getInt("CustomerID"));
        a.setPetID(rs.getInt("PetID"));
        a.setServiceID(rs.getInt("ServiceID"));
        int vetID = rs.getInt("AssignedStaffID");
        a.setAssignedStaffID(rs.wasNull() ? null : vetID);
        Date d = rs.getDate("AppointmentDate"); if (d != null) a.setAppointmentDate(d.toLocalDate());
        Time st = rs.getTime("StartTime");       if (st != null) a.setStartTime(st.toLocalTime());
        Time et = rs.getTime("EndTime");         if (et != null) a.setEndTime(et.toLocalTime());
        a.setStatus(rs.getString("Status"));
        try { int sh = rs.getInt("SlotShift"); if (!rs.wasNull()) a.setSlotShift(sh); } catch (SQLException ignored) {}
        try { a.setCustomerName(rs.getString("CustomerName")); } catch (SQLException ignored) {}
        try { a.setPetName(rs.getString("PetName"));           } catch (SQLException ignored) {}
        try { a.setServiceName(rs.getString("ServiceName"));   } catch (SQLException ignored) {}
        try { a.setStaffName(rs.getString("StaffName"));           } catch (SQLException ignored) {}
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