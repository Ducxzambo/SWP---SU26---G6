package com.petclinic.backend.dao;

import com.petclinic.backend.service.BookingService;
import com.petclinic.backend.model.Appointment;
import com.petclinic.backend.model.AppointmentService;
import com.petclinic.backend.dao.AppointmentServiceDAO;
import com.petclinic.backend.model.AppointmentServiceItem;
import com.petclinic.backend.util.DBConnection;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public class AppointmentDAO {
    private AppointmentServiceDAO appointmentServiceDAO = new AppointmentServiceDAO();

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
    public List<Appointment> findConfirmedByDate(LocalDate date, Integer shift, String categoryFilter) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT DISTINCT a.AppointmentID,a.CustomerID,a.PetID," +
                        "a.AppointmentDate,a.StartTime,a.EndTime,a.Status,a.SlotShift,a.Notes,a.CancelReason," +
                        "c.FullName AS CustomerName," +
                        "p.Name AS PetName,p.SpeciesName AS PetSpeciesName,p.BreedName AS PetBreedName," +
                        "p.Gender AS PetGender,p.Weight AS PetWeight " +
                        "FROM Appointments a " +
                        "JOIN Customers c ON c.CustomerID=a.CustomerID " +
                        "LEFT JOIN Pets p ON p.PetID=a.PetID "
        );
        if (categoryFilter != null) {
            sql.append("JOIN AppointmentServices aps ON aps.AppointmentID=a.AppointmentID ")
                    .append("JOIN Services s ON s.ServiceID=aps.ServiceID ")
                    .append("JOIN ServiceCategories sc ON sc.CategoryID=s.CategoryID ");
        }
        sql.append("WHERE a.AppointmentDate=? AND a.Status='Confirmed' ");
        if (shift != null) sql.append("AND a.SlotShift=? ");
        if (categoryFilter != null) sql.append("AND sc.Name=? ");
        sql.append("ORDER BY a.SlotShift,a.StartTime");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setDate(idx++, Date.valueOf(date));
            if (shift != null) ps.setInt(idx++, shift);
            if (categoryFilter != null) ps.setString(idx++, categoryFilter);
            List<Appointment> list = mapList(ps.executeQuery());
            attachServices(list);
            return list;
        }
    }

    public List<Appointment> searchForCheckIn(String keyword, LocalDate date, String categoryFilter) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT DISTINCT a.AppointmentID,a.CustomerID,a.PetID," +
                        "a.AppointmentDate,a.StartTime,a.EndTime,a.Status,a.SlotShift,a.Notes,a.CancelReason," +
                        "c.FullName AS CustomerName,p.Name AS PetName,p.SpeciesName, p.BreedName, p.Gender, p.Weight " +
                        "FROM Appointments a " +
                        "JOIN Customers c ON c.CustomerID=a.CustomerID " +
                        "LEFT JOIN Pets p ON p.PetID=a.PetID "
        );
        if (categoryFilter != null) {
            sql.append("JOIN AppointmentServices aps ON aps.AppointmentID=a.AppointmentID ")
                    .append("JOIN Services s ON s.ServiceID=aps.ServiceID ")
                    .append("JOIN ServiceCategories sc ON sc.CategoryID=s.CategoryID ");
        }
        sql.append("WHERE a.AppointmentDate=? AND a.Status='Confirmed' AND (c.FullName LIKE ? OR p.Name LIKE ? OR p.Name IS NULL) ");
        if (categoryFilter != null) sql.append("AND sc.Name=? ");
        sql.append("ORDER BY a.SlotShift,a.StartTime");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setDate(idx++, Date.valueOf(date));
            String like = "%" + keyword.trim() + "%";
            ps.setString(idx++, like);
            ps.setString(idx++, like);
            if (categoryFilter != null) ps.setString(idx++, categoryFilter);
            List<Appointment> list = mapList(ps.executeQuery());
            attachServices(list);
            return list;
        }
    }


    // Walk-in
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

    // Thêm 1 dịch vụ vào appointment đã tồn tại + staff phụ trách
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

    // Gán nhân viên phụ trách theo từng dòng dịch vụ
    public void assignStaffToService(int appointmentServiceID, int staffID) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE AppointmentServices SET AssignedStaffID=? WHERE AppointmentServiceID=?")) {
            ps.setInt(1, staffID); ps.setInt(2, appointmentServiceID); ps.executeUpdate();
        }
    }

    // Gán 1 nhân viên cho mọi dịch vụ thuộc 1 category trong 1 appointment (gán hàng loạt lúc check-in).
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

    // Vet queue (BP-02)
    public List<Appointment> findStaffQueue(int staffID, LocalDate date, String categoryFilter,
                                            String excludeIfRecordExistsIn) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT DISTINCT a.AppointmentID,a.CustomerID,a.PetID," +
                        "a.AppointmentDate,a.StartTime,a.EndTime,a.Status,a.SlotShift,a.Notes,a.CancelReason," +
                        "c.FullName AS CustomerName,p.Name AS PetName, " +
                        "CASE a.Status WHEN 'InProgress' THEN 0 ELSE 1 END AS StatusOrder " + // <-- THÊM CỘT NÀY VÀO SELECT
                        "FROM Appointments a " +
                        "JOIN Customers c ON c.CustomerID=a.CustomerID " +
                        "LEFT JOIN Pets p ON p.PetID=a.PetID " +
                        "JOIN AppointmentServices aps ON aps.AppointmentID=a.AppointmentID " +
                        "JOIN Services s ON s.ServiceID=aps.ServiceID " +
                        "JOIN ServiceCategories sc ON sc.CategoryID=s.CategoryID " +
                        "WHERE a.AppointmentDate=? AND aps.AssignedStaffID=? AND a.Status IN('Arrived','InProgress') AND sc.Name=? "
        );
        if (excludeIfRecordExistsIn != null) {
            sql.append("AND NOT EXISTS (SELECT 1 FROM ").append(excludeIfRecordExistsIn)
                    .append(" rec WHERE rec.AppointmentID = a.AppointmentID) ");
        }
        sql.append("ORDER BY StatusOrder, a.SlotShift, a.StartTime");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setDate(1, Date.valueOf(date));
            ps.setInt(2, staffID);
            ps.setString(3, categoryFilter);
            List<Appointment> list = mapList(ps.executeQuery());
            attachServices(list);
            return list;
        }
    }
    //  Helpers
    private List<Appointment> mapList(ResultSet rs) throws SQLException {
        List<Appointment> list = new ArrayList<>();
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }


    //  Insert

    public int insert(Appointment a) throws SQLException {
        String sql = "INSERT INTO Appointments "
                + "(CustomerID, PetID, AppointmentDate, StartTime, EndTime, Status, SlotShift) "
                + "VALUES (?, ?, ?, ?, ?, 'Pending', ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, a.getCustomerID());
            if (a.getPetID() != null) ps.setInt(2, a.getPetID());
            else ps.setNull(2, Types.INTEGER);
            ps.setDate(3, Date.valueOf(a.getAppointmentDate()));
            ps.setTime(4, Time.valueOf(a.getStartTime()));
            ps.setTime(5, Time.valueOf(a.getEndTime()));
            if (a.getSlotShift() != null) ps.setInt(6, a.getSlotShift());
            else ps.setNull(6, Types.TINYINT);
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

    public List<Appointment> findUnassignedArrived(LocalDate date, String categoryFilter,
                                                   String excludeIfRecordExistsIn) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT DISTINCT a.AppointmentID,a.CustomerID,a.PetID," +
                        "a.AppointmentDate,a.StartTime,a.EndTime,a.Status,a.SlotShift,a.Notes,a.CancelReason," +
                        "c.FullName AS CustomerName,p.Name AS PetName " +
                        "FROM Appointments a " +
                        "JOIN Customers c ON c.CustomerID=a.CustomerID " +
                        "LEFT JOIN Pets p ON p.PetID=a.PetID " +
                        "JOIN AppointmentServices aps ON aps.AppointmentID=a.AppointmentID " +
                        "JOIN Services s ON s.ServiceID=aps.ServiceID " +
                        "JOIN ServiceCategories sc ON sc.CategoryID=s.CategoryID " +
                        "WHERE a.AppointmentDate=? AND aps.AssignedStaffID IS NULL AND a.Status='Arrived' AND sc.Name=? "
        );
        if (excludeIfRecordExistsIn != null) {
            sql.append("AND NOT EXISTS (SELECT 1 FROM ").append(excludeIfRecordExistsIn)
                    .append(" rec WHERE rec.AppointmentID = a.AppointmentID) ");
        }
        sql.append("ORDER BY a.SlotShift, a.StartTime");
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setDate(1, Date.valueOf(date));
            ps.setString(2, categoryFilter);
            List<Appointment> list = mapList(ps.executeQuery());
            attachServices(list);
            return list;
        }
    }

    public List<Appointment> findStaffCompletedToday(int staffID, LocalDate date, String categoryFilter,
                                                     String recordTable) throws SQLException {
        String sql =
                "SELECT DISTINCT a.AppointmentID,a.CustomerID,a.PetID," +
                        "a.AppointmentDate,a.StartTime,a.EndTime,a.Status,a.SlotShift,a.Notes,a.CancelReason," +
                        "c.FullName AS CustomerName,p.Name AS PetName, rec.RecordID AS RecordID " +
                        "FROM Appointments a " +
                        "JOIN Customers c ON c.CustomerID=a.CustomerID " +
                        "LEFT JOIN Pets p ON p.PetID=a.PetID " +
                        "JOIN AppointmentServices aps ON aps.AppointmentID=a.AppointmentID " +
                        "JOIN Services s ON s.ServiceID=aps.ServiceID " +
                        "JOIN ServiceCategories sc ON sc.CategoryID=s.CategoryID " +
                        "JOIN " + recordTable + " rec ON rec.AppointmentID = a.AppointmentID " +
                        "WHERE a.AppointmentDate=? AND aps.AssignedStaffID=? AND sc.Name=? " +
                        "  AND a.Status IN ('InProgress','Done') " +
                        "ORDER BY a.SlotShift, a.StartTime";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            ps.setInt(2, staffID);
            ps.setString(3, categoryFilter);
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

    //  Lịch sử lịch hẹn (Receptionist) + Hoàn tất lịch hẹn
    public List<Appointment> findAppointmentHistory(LocalDate date, Integer shift) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT a.AppointmentID,a.CustomerID,a.PetID," +
                        "a.AppointmentDate,a.StartTime,a.EndTime,a.Status,a.SlotShift,a.Notes,a.CancelReason," +
                        "c.FullName AS CustomerName,p.Name AS PetName " +
                        "FROM Appointments a " +
                        "JOIN Customers c ON c.CustomerID=a.CustomerID " +
                        "JOIN Pets p ON p.PetID=a.PetID " +
                        "WHERE a.AppointmentDate=? "
        );
        if (shift != null) sql.append("AND a.SlotShift=? ");
        sql.append("ORDER BY a.SlotShift, a.StartTime");

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

    //Kiểm tra mọi category dịch vụ trong appointment đã có record tương ứng chưa
    public List<String> findMissingRecordCategories(int appointmentID) throws SQLException {
        String sql =
                "SELECT DISTINCT sc.Name AS CategoryName " +
                        "FROM AppointmentServices aps " +
                        "JOIN Services s ON s.ServiceID = aps.ServiceID " +
                        "JOIN ServiceCategories sc ON sc.CategoryID = s.CategoryID " +
                        "WHERE aps.AppointmentID = ? " +
                        "  AND (" +
                        "    (sc.Name IN ('Chẩn đoán','Điều trị') AND NOT EXISTS " +
                        "        (SELECT 1 FROM MedicalRecords mr WHERE mr.AppointmentID = aps.AppointmentID))" +
                        "    OR " +
                        "    (sc.Name = 'Grooming' AND NOT EXISTS " +
                        "        (SELECT 1 FROM GroomingRecords gr WHERE gr.AppointmentID = aps.AppointmentID))" +
                        "  )";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, appointmentID);
            List<String> missing = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) missing.add(rs.getString("CategoryName"));
            }
            return missing;
        }
    }

    //Lễ tân xác nhận hoàn tất lịch hẹn: chuyển Status -> Done.
    public boolean finalizeAppointment(int appointmentID) throws SQLException {
        String sql = "UPDATE Appointments SET Status='Done' WHERE AppointmentID=? AND Status IN ('Arrived','InProgress')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, appointmentID);
            return ps.executeUpdate() > 0;
        }
    }

    // Queries
    public Appointment findById(int appointmentID) throws SQLException {
        Appointment appt = new Appointment();
        String sql = "SELECT a.AppointmentID,a.CustomerID,a.PetID," +
                "a.AppointmentDate,a.StartTime,a.EndTime,a.Status,a.SlotShift,a.Notes,a.CancelReason," +
                "c.FullName AS CustomerName,p.Name AS PetName " +
                "FROM Appointments a " +
                "JOIN Customers c ON c.CustomerID=a.CustomerID " +
                "LEFT JOIN Pets p ON p.PetID=a.PetID " +
                "WHERE a.AppointmentID=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, appointmentID);
            try (ResultSet rs = ps.executeQuery()) {
                appt = rs.next() ? mapRow(rs) : null;
            }
        }
        if (appt != null) attachServicesAndVaccines(Collections.singletonList(appt));
        return appt;
    }

    public List<Appointment> findByCustomer(int customerId) throws SQLException {
        String sql = "SELECT a.*, p.Name AS PetName "
                + "FROM Appointments a "
                + "LEFT JOIN Pets p ON a.PetID = p.PetID "
                + "WHERE a.CustomerID = ? ORDER BY a.AppointmentDate DESC, a.StartTime DESC";
        List<Appointment> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        attachServicesAndVaccines(list);
        return list;
    }

    public List<Appointment> findByPet(int petId) throws SQLException {
        String sql = "SELECT a.*, p.Name AS PetName "
                + "FROM Appointments a "
                + "LEFT JOIN Pets p ON a.PetID = p.PetID "
                + "WHERE a.PetID = ? ORDER BY a.AppointmentDate DESC, a.StartTime DESC";
        List<Appointment> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, petId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        attachServicesAndVaccines(list);
        return list;
    }

    public int countConfirmedInSlotByRoleGroup(LocalDate date, int slotShift, int roleId)
            throws SQLException {
        // roleId: 4 = Groomer, 3 = Vet
        String sql = "SELECT COUNT(*) FROM Appointments a "
                + "WHERE a.AppointmentDate = ? "
                + "AND a.SlotShift = ? "
                + "AND a.Status = 'Confirmed' "
                + "AND ( "
                + "  (? = 4 AND EXISTS (SELECT 1 FROM AppointmentServices asvc "
                + "                     JOIN Services s ON asvc.ServiceID = s.ServiceID "
                + "                     WHERE asvc.AppointmentID = a.AppointmentID AND s.CategoryID = 3)) "
                + "  OR "
                + "  (? = 3 AND EXISTS (SELECT 1 FROM AppointmentServices asvc "
                + "                     JOIN Services s ON asvc.ServiceID = s.ServiceID "
                + "                     WHERE asvc.AppointmentID = a.AppointmentID AND s.CategoryID <> 3)) "
                + ")";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            ps.setInt(2, slotShift);
            ps.setInt(3, roleId);
            ps.setInt(4, roleId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public List<Appointment> findOverdueActive() throws SQLException {
        String sql = "SELECT a.*, p.Name AS PetName "
                + "FROM Appointments a "
                + "LEFT JOIN Pets p ON a.PetID = p.PetID "
                + "WHERE a.Status IN ('Pending','Confirmed') "
                + "AND CAST(CAST(a.AppointmentDate AS DATE) AS DATETIME) "
                + "    + CAST(a.EndTime AS DATETIME) < GETDATE()";
        List<Appointment> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        attachServicesAndVaccines(list);
        return list;
    }

    public void markNoShow(int appointmentId) throws SQLException {
        String sql = "UPDATE Appointments SET Status = 'NoShow' WHERE AppointmentID = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, appointmentId);
            ps.executeUpdate();
        }
    }

     public List<Appointment> findOverdueOlderThan24h() throws SQLException {
        String sql = "SELECT a.*, p.Name AS PetName "
                + "FROM Appointments a "
                + "LEFT JOIN Pets p ON a.PetID = p.PetID "
                + "WHERE a.Status IN ('Pending','Confirmed') "
                + "AND CAST(CAST(a.AppointmentDate AS DATE) AS DATETIME) "
                + "    + CAST(a.EndTime AS DATETIME) < DATEADD(HOUR, -24, GETDATE())";
        List<Appointment> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        attachServicesAndVaccines(list);
        return list;
    }


    public void updateSlot(int appointmentId, LocalDate date, LocalTime start, LocalTime end)
            throws SQLException {
        String sql = "UPDATE Appointments SET AppointmentDate=?, StartTime=?, EndTime=?, SlotShift=? "
                + "WHERE AppointmentID=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            ps.setTime(2, Time.valueOf(start));
            ps.setTime(3, Time.valueOf(end));
            Integer shift = BookingService.slotShiftOf(start);
            if (shift != null) ps.setInt(4, shift); else ps.setNull(4, Types.TINYINT);
            ps.setInt(5, appointmentId);
            ps.executeUpdate();
        }
    }

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

    // helpers
    private void attachServicesAndVaccines(List<Appointment> list) throws SQLException {
        if (list == null || list.isEmpty()) return;
        List<Integer> ids = list.stream().map(Appointment::getAppointmentID).collect(Collectors.toList());

        Map<Integer, List<AppointmentService>> svcMap = appointmentServiceDAO.findByAppointmentIds(ids);

        for (Appointment a : list) {
            a.setServices(svcMap.getOrDefault(a.getAppointmentID(), new ArrayList<>()));
        }
    }

    // Mapping
    private Appointment mapRow(ResultSet rs) throws SQLException {
        Appointment a = new Appointment();
        a.setAppointmentID(rs.getInt("AppointmentID"));
        a.setCustomerID(rs.getInt("CustomerID"));
        int pid = rs.getInt("PetID");
        a.setPetID(rs.wasNull() ? null : pid);
        Date d = rs.getDate("AppointmentDate"); if (d != null) a.setAppointmentDate(d.toLocalDate());
        Time st = rs.getTime("StartTime");       if (st != null) a.setStartTime(st.toLocalTime());
        Time et = rs.getTime("EndTime");         if (et != null) a.setEndTime(et.toLocalTime());
        a.setStatus(rs.getString("Status"));
        try { int sh = rs.getInt("SlotShift"); if (!rs.wasNull()) a.setSlotShift(sh); } catch (SQLException ignored) {}
        try { a.setNotes(rs.getString("Notes")); } catch (SQLException ignored) {}
        try { a.setCancelReason(rs.getString("CancelReason")); } catch (SQLException ignored) {}
        try { a.setCustomerName(rs.getString("CustomerName")); } catch (SQLException ignored) {}
        try { a.setPetName(rs.getString("PetName"));           } catch (SQLException ignored) {}
        try { a.setPetSpeciesName(rs.getString("PetSpeciesName")); } catch (SQLException ignored) {}
        try { a.setPetBreedName(rs.getString("PetBreedName"));   } catch (SQLException ignored) {}
        try { a.setPetGender(rs.getString("PetGender"));         } catch (SQLException ignored) {}
        try { a.setPetWeight(rs.getBigDecimal("PetWeight"));     } catch (SQLException ignored) {}
        return a;
    }

    // Gán pet vào appointment (dùng khi lễ tân check-in online booking chưa có pet).
    public void assignPet(int appointmentID, int petID) throws SQLException {
        String sql = "UPDATE Appointments SET PetID=? WHERE AppointmentID=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, petID); ps.setInt(2, appointmentID); ps.executeUpdate();
        }
    }

    private Appointment mapRowSimple(ResultSet rs) throws SQLException {
        Appointment a = new Appointment();
        a.setAppointmentID(rs.getInt("AppointmentID"));
        a.setCustomerID(rs.getInt("CustomerID"));
        int petId = rs.getInt("PetID"); if (!rs.wasNull()) a.setPetID(petId);
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

    private AppointmentService mapServiceItem(ResultSet rs) throws SQLException {
        AppointmentService item = new AppointmentService();
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

    // Tìm appointment theo danh sách Status
    public List<Appointment> findByStatuses(List<String> statuses, String keyword) throws SQLException {
        if (statuses == null || statuses.isEmpty()) return new ArrayList<>();

        StringBuilder sql = new StringBuilder(
                "SELECT a.*, p.Name AS PetName, c.FullName AS CustomerName "
                        + "FROM Appointments a "
                        + "LEFT JOIN Pets p ON a.PetID = p.PetID "
                        + "JOIN Customers c ON a.CustomerID = c.CustomerID "
                        + "WHERE a.Status IN (" + placeholders(statuses.size()) + ")");
        List<Object> params = new ArrayList<>(statuses);

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (c.FullName LIKE ? OR p.Name LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
        }
        sql.append(" ORDER BY a.AppointmentDate DESC, a.StartTime DESC");

        List<Appointment> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Appointment a = mapRow(rs);
                    a.setCustomerName(rs.getString("CustomerName"));
                    list.add(a);
                }
            }
        }
        return list;
    }

    private String placeholders(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) sb.append(i == 0 ? "?" : ", ?");
        return sb.toString();
    }

}