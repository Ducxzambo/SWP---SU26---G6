package com.petclinic.dao;

import com.petclinic.model.Appointment;
import com.petclinic.model.AppointmentServiceItem;
import com.petclinic.util.DBConnection;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

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
        StringBuilder sql = new StringBuilder(
                "SELECT DISTINCT a.AppointmentID,a.CustomerID,a.PetID," +
                        "a.AppointmentDate,a.StartTime,a.EndTime,a.Status,a.SlotShift,a.Notes,a.CancelReason," +
                        "c.FullName AS CustomerName,p.Name AS PetName " +
                        "FROM Appointments a " +
                        "JOIN Customers c ON c.CustomerID=a.CustomerID " +
                        "JOIN Pets p ON p.PetID=a.PetID "
        );

        sql.append("WHERE a.AppointmentDate=? AND a.Status='Confirmed' ");
        if (shift != null) sql.append("AND a.SlotShift=? ");
        sql.append("ORDER BY a.SlotShift,a.StartTime");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setDate(idx++, Date.valueOf(date));
            if (shift != null) ps.setInt(idx++, shift);
            List<Appointment> list = mapList(ps.executeQuery());
            attachServices(list);
            return list;
        }
    }

    public List<Appointment> searchForCheckIn(String keyword, LocalDate date) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT DISTINCT a.AppointmentID,a.CustomerID,a.PetID," +
                        "a.AppointmentDate,a.StartTime,a.EndTime,a.Status,a.SlotShift,a.Notes,a.CancelReason," +
                        "c.FullName AS CustomerName,p.Name AS PetName " +
                        "FROM Appointments a " +
                        "JOIN Customers c ON c.CustomerID=a.CustomerID " +
                        "JOIN Pets p ON p.PetID=a.PetID "
        );

        sql.append("WHERE a.AppointmentDate=? AND a.Status='Confirmed' AND (c.FullName LIKE ? OR p.Name LIKE ?) ");
        sql.append("ORDER BY a.SlotShift,a.StartTime");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setDate(idx++, Date.valueOf(date));
            String like = "%" + keyword.trim() + "%";
            ps.setString(idx++, like);
            ps.setString(idx++, like);
            List<Appointment> list = mapList(ps.executeQuery());
            attachServices(list);
            return list;
        }
    }


    // ── Walk-in ───────────────────────────────────────────────────────────────
    public int createWalkIn(int customerID, int petID, List<Integer> serviceIDs,
                            List<java.math.BigDecimal> unitPrices,
                            List<Integer> staffIDs) throws SQLException {
        LocalDate today = LocalDate.now();
        LocalTime now   = LocalTime.now();
        int shift = shiftOf(now); if (shift == -1) shift = 1;
        LocalTime end = now.plusMinutes(30);

        Connection conn = DBConnection.getConnection();
        try {
            conn.setAutoCommit(false);

            int appointmentID;
            String sql = "INSERT INTO Appointments(CustomerID,PetID,AppointmentDate,StartTime,EndTime,SlotShift,Status) " +
                    "VALUES(?,?,?,?,?,?,'Arrived')";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, customerID); ps.setInt(2, petID);
                ps.setDate(3, Date.valueOf(today)); ps.setTime(4, Time.valueOf(now)); ps.setTime(5, Time.valueOf(end));
                ps.setInt(6, shift);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("Failed to create walk-in appointment.");
                    appointmentID = keys.getInt(1);
                }
            }

            String svcSql = "INSERT INTO AppointmentServices(AppointmentID, ServiceID, UnitPrice, AssignedStaffID) VALUES (?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(svcSql)) {
                for (int i = 0; i < serviceIDs.size(); i++) {
                    ps.setInt(1, appointmentID);
                    ps.setInt(2, serviceIDs.get(i));
                    ps.setBigDecimal(3, unitPrices.get(i));
                    Integer sid = staffIDs.get(i);
                    if (sid != null) ps.setInt(4, sid); else ps.setNull(4, Types.INTEGER);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit();
            return appointmentID;

        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
            conn.close();
        }
    }

    /** Thêm 1 dịch vụ vào appointment đã tồn tại, kèm staff phụ trách tùy chọn. */
    public void addServiceToAppointment(int appointmentID, int serviceID,
                                        java.math.BigDecimal unitPrice, Integer staffID) throws SQLException {
        String sql = "INSERT INTO AppointmentServices(AppointmentID, ServiceID, UnitPrice, AssignedStaffID) VALUES (?,?,?,?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, appointmentID); ps.setInt(2, serviceID); ps.setBigDecimal(3, unitPrice);
            if (staffID != null) ps.setInt(4, staffID); else ps.setNull(4, Types.INTEGER);
            ps.executeUpdate();
        }
    }

    // ── Gán nhân viên phụ trách theo TỪNG DÒNG dịch vụ ──────────────────────────

    /** Gán 1 nhân viên cho 1 dòng dịch vụ cụ thể (AppointmentServiceID). */
    public void assignStaffToService(int appointmentServiceID, int staffID) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE AppointmentServices SET AssignedStaffID=? WHERE AppointmentServiceID=?")) {
            ps.setInt(1, staffID); ps.setInt(2, appointmentServiceID); ps.executeUpdate();
        }
    }

    /** Gán 1 nhân viên cho MỌI dịch vụ thuộc 1 category trong 1 appointment (gán hàng loạt lúc check-in). */
    public void assignStaffToCategory(int appointmentID, String categoryName, int staffID) throws SQLException {
        String sql = "UPDATE aps SET aps.AssignedStaffID = ? " +
                "FROM AppointmentServices aps " +
                "JOIN Services s ON s.ServiceID = aps.ServiceID " +
                "JOIN ServiceCategories sc ON sc.CategoryID = s.CategoryID " +
                "WHERE aps.AppointmentID = ? AND sc.Name = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, staffID); ps.setInt(2, appointmentID); ps.setString(3, categoryName);
            ps.executeUpdate();
        }
    }

    // ── Vet queue (BP-02) ───────────────────────────────────────────────────────


    /**
     * Arrived/InProgress appointments có ít nhất 1 dòng service ĐANG GÁN cho staffID
     * và thuộc categoryFilter. Dùng cho hàng chờ của bác sĩ/groomer.
     */
    public List<Appointment> findStaffQueue(int staffID, LocalDate date) throws SQLException {
        String sql =
                "SELECT DISTINCT a.AppointmentID,a.CustomerID,a.PetID," +
                        "a.AppointmentDate,a.StartTime,a.EndTime,a.Status,a.SlotShift,a.Notes,a.CancelReason," +
                        "c.FullName AS CustomerName,p.Name AS PetName " +
                        "FROM Appointments a " +
                        "JOIN Customers c ON c.CustomerID=a.CustomerID " +
                        "JOIN Pets p ON p.PetID=a.PetID " +
                        "JOIN AppointmentServices aps ON aps.AppointmentID=a.AppointmentID " +
                        "JOIN Services s ON s.ServiceID=aps.ServiceID " +
                        "JOIN ServiceCategories sc ON sc.CategoryID=s.CategoryID " +
                        "WHERE a.AppointmentDate=? AND aps.AssignedStaffID=? AND a.Status IN('Arrived','InProgress')  " +
                        "ORDER BY CASE a.Status WHEN 'InProgress' THEN 0 ELSE 1 END, a.SlotShift, a.StartTime";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            ps.setInt(2, staffID);
            List<Appointment> list = mapList(ps.executeQuery());
            attachServices(list);
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

    /**
     * Arrived appointments có ÍT NHẤT 1 dòng service thuộc categoryFilter CHƯA gán staff
     * (AssignedStaffID IS NULL) — dùng cho self-assign (groomer tự nhận ca).
     */
    public List<Appointment> findUnassignedArrived(LocalDate date, String categoryFilter) throws SQLException {
        String sql =
                "SELECT DISTINCT a.AppointmentID,a.CustomerID,a.PetID," +
                        "a.AppointmentDate,a.StartTime,a.EndTime,a.Status,a.SlotShift,a.Notes,a.CancelReason," +
                        "c.FullName AS CustomerName,p.Name AS PetName " +
                        "FROM Appointments a " +
                        "JOIN Customers c ON c.CustomerID=a.CustomerID " +
                        "JOIN Pets p ON p.PetID=a.PetID " +
                        "JOIN AppointmentServices aps ON aps.AppointmentID=a.AppointmentID " +
                        "JOIN Services s ON s.ServiceID=aps.ServiceID " +
                        "JOIN ServiceCategories sc ON sc.CategoryID=s.CategoryID " +
                        "WHERE a.AppointmentDate=? AND aps.AssignedStaffID IS NULL AND a.Status='Arrived' AND sc.Name=? " +
                        "ORDER BY a.SlotShift, a.StartTime";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            ps.setString(2, categoryFilter);
            List<Appointment> list = mapList(ps.executeQuery());
            attachServices(list);
            return list;
        }
    }

    /** Các ca đã hoàn thành (Done) có dịch vụ thuộc categoryFilter, gán cho staffID, trong 1 ngày — kèm RecordID. */
    public List<Appointment> findStaffCompletedToday(int staffID, LocalDate date
                                                     ) throws SQLException {
        String sql =
                "SELECT DISTINCT a.AppointmentID,a.CustomerID,a.PetID," +
                        "a.AppointmentDate,a.StartTime,a.EndTime,a.Status,a.SlotShift,a.Notes,a.CancelReason," +
                        "c.FullName AS CustomerName,p.Name AS PetName, rec.RecordID AS RecordID " +
                        "FROM Appointments a " +
                        "JOIN Customers c ON c.CustomerID=a.CustomerID " +
                        "JOIN Pets p ON p.PetID=a.PetID " +
                        "JOIN AppointmentServices aps ON aps.AppointmentID=a.AppointmentID " +
                        "JOIN Services s ON s.ServiceID=aps.ServiceID " +
                        "JOIN ServiceCategories sc ON sc.CategoryID=s.CategoryID " +
                        "LEFT JOIN rec ON rec.AppointmentID = a.AppointmentID " +
                        "WHERE a.AppointmentDate=? AND aps.AssignedStaffID=? AND a.Status='Done' " +
                        "ORDER BY a.SlotShift, a.StartTime";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            ps.setInt(2, staffID);
            List<Appointment> list = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Appointment a = mapRow(rs);
                    int recId = rs.getInt("RecordID");
                    a.setRecordID(rs.wasNull() ? null : recId);
                    list.add(a);
                }
            }
            attachServices(list);
            return list;
        }
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public Appointment findById(int appointmentID) throws SQLException {
        String sql = "SELECT a.AppointmentID,a.CustomerID,a.PetID," +
                "a.AppointmentDate,a.StartTime,a.EndTime,a.Status,a.SlotShift,a.Notes,a.CancelReason," +
                "c.FullName AS CustomerName,p.Name AS PetName " +
                "FROM Appointments a " +
                "JOIN Customers c ON c.CustomerID=a.CustomerID " +
                "JOIN Pets p ON p.PetID=a.PetID " +
                "WHERE a.AppointmentID=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, appointmentID);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Appointment a = mapRow(rs);
                a.setServices(loadServicesFor(conn, appointmentID));
                return a;
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



    private List<AppointmentServiceItem> loadServicesFor(Connection conn, int appointmentID) throws SQLException {
        String sql = "SELECT aps.AppointmentServiceID, aps.AppointmentID, aps.ServiceID, aps.UnitPrice, aps.AssignedStaffID, " +
                "s.Name AS ServiceName, sc.Name AS CategoryName, st.FullName AS StaffName " +
                "FROM AppointmentServices aps " +
                "JOIN Services s ON s.ServiceID = aps.ServiceID " +
                "JOIN ServiceCategories sc ON sc.CategoryID = s.CategoryID " +
                "LEFT JOIN Staff st ON st.StaffID = aps.AssignedStaffID " +
                "WHERE aps.AppointmentID = ? ORDER BY aps.AppointmentServiceID";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, appointmentID);
            List<AppointmentServiceItem> list = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapServiceItem(rs));
            }
            return list;
        }
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private Appointment mapRow(ResultSet rs) throws SQLException {
        Appointment a = new Appointment();
        a.setAppointmentID(rs.getInt("AppointmentID"));
        a.setCustomerID(rs.getInt("CustomerID"));
        a.setPetID(rs.getInt("PetID"));
        Date d = rs.getDate("AppointmentDate"); if (d != null) a.setAppointmentDate(d.toLocalDate());
        Time st = rs.getTime("StartTime");       if (st != null) a.setStartTime(st.toLocalTime());
        Time et = rs.getTime("EndTime");         if (et != null) a.setEndTime(et.toLocalTime());
        a.setStatus(rs.getString("Status"));
        try { int sh = rs.getInt("SlotShift"); if (!rs.wasNull()) a.setSlotShift(sh); } catch (SQLException ignored) {}
        try { a.setNotes(rs.getString("Notes")); } catch (SQLException ignored) {}
        try { a.setCancelReason(rs.getString("CancelReason")); } catch (SQLException ignored) {}
        try { a.setCustomerName(rs.getString("CustomerName")); } catch (SQLException ignored) {}
        try { a.setPetName(rs.getString("PetName"));           } catch (SQLException ignored) {}
        return a;
    }

    private Appointment mapRowSimple(ResultSet rs) throws SQLException {
        Appointment a = new Appointment();
        a.setAppointmentID(rs.getInt("AppointmentID"));
        a.setCustomerID(rs.getInt("CustomerID"));
        a.setPetID(rs.getInt("PetID"));
        a.setServiceID(rs.getInt("ServiceID"));
        int vid = rs.getInt("AssignedStaffID");
        a.setAppointmentDate(rs.getDate("AppointmentDate").toLocalDate());
        a.setStartTime(rs.getTime("StartTime").toLocalTime());
        a.setEndTime(rs.getTime("EndTime").toLocalTime());
        a.setStatus(rs.getString("Status"));
        a.setNotes(rs.getString("Notes"));
        a.setCancelReason(rs.getString("CancelReason"));
        int shift = rs.getInt("SlotShift"); if (!rs.wasNull()) a.setSlotShift(shift);
        return a;
    }

    private void attachServices(List<Appointment> appointments) throws SQLException {
        if (appointments.isEmpty()) return;

        Map<Integer, Appointment> byId = new HashMap<>();
        for (Appointment a : appointments) byId.put(a.getAppointmentID(), a);

        String placeholders = String.join(",", Collections.nCopies(appointments.size(), "?"));
        String sql = "SELECT aps.AppointmentServiceID, aps.AppointmentID, aps.ServiceID, aps.UnitPrice, aps.AssignedStaffID, " +
                "s.Name AS ServiceName, sc.Name AS CategoryName, st.FullName AS StaffName " +
                "FROM AppointmentServices aps " +
                "JOIN Services s ON s.ServiceID = aps.ServiceID " +
                "JOIN ServiceCategories sc ON sc.CategoryID = s.CategoryID " +
                "LEFT JOIN Staff st ON st.StaffID = aps.AssignedStaffID " +
                "WHERE aps.AppointmentID IN (" + placeholders + ") " +
                "ORDER BY aps.AppointmentID, aps.AppointmentServiceID";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            for (Appointment a : appointments) ps.setInt(idx++, a.getAppointmentID());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int apptId = rs.getInt("AppointmentID");
                    Appointment a = byId.get(apptId);
                    if (a != null) a.getServices().add(mapServiceItem(rs));
                }
            }
        }
    }

    private AppointmentServiceItem mapServiceItem(ResultSet rs) throws SQLException {
        AppointmentServiceItem item = new AppointmentServiceItem();
        item.setAppointmentServiceID(rs.getInt("AppointmentServiceID"));
        item.setAppointmentID(rs.getInt("AppointmentID"));
        item.setServiceID(rs.getInt("ServiceID"));
        item.setUnitPrice(rs.getBigDecimal("UnitPrice"));
        int staffID = rs.getInt("AssignedStaffID");
        item.setAssignedStaffID(rs.wasNull() ? null : staffID);
        item.setServiceName(rs.getString("ServiceName"));
        item.setCategoryName(rs.getString("CategoryName"));
        item.setStaffName(rs.getString("StaffName"));
        return item;
    }


}