package com.petclinic.dao;

import com.petclinic.model.Appointment;
import com.petclinic.model.AppointmentService;
import com.petclinic.util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AppointmentDAO {

    private final AppointmentServiceDAO appointmentServiceDAO = new AppointmentServiceDAO();

    // ── Insert ────────────────────────────────────────────────────────────────
    public int insert(Appointment a) throws SQLException {
        String sql = "INSERT INTO Appointments "
                + "(CustomerID, PetID, AppointmentDate, StartTime, EndTime, Status, SlotShift) "
                + "VALUES (?, ?, ?, ?, ?, 'Pending', ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, a.getCustomerID());
            ps.setInt(2, a.getPetID());
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

    // ── Queries ───────────────────────────────────────────────────────────────

    public Appointment findById(int appointmentId) throws SQLException {
        String sql = "SELECT a.*, p.Name AS PetName "
                + "FROM Appointments a "
                + "JOIN Pets p ON a.PetID = p.PetID "
                + "WHERE a.AppointmentID = ?";
        Appointment appt;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, appointmentId);
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
                + "JOIN Pets p ON a.PetID = p.PetID "
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
                + "JOIN Pets p ON a.PetID = p.PetID "
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

    /** Tất cả booked appointment cho 1 date. */
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

    /** Tất cả booked appoint cho 1 date, ngoại trừ 1 appointment cụ thể (dùng ở reschedule) */
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


    /** Đếm slot đã confirmed, theo cả groomer và vet */
    public int countConfirmedInSlotByRoleGroup(LocalDate date, int slotShift, int roleId)
            throws SQLException {
        // roleId: 4 = Groomer, 3 = Vet (khop com.petclinic.dao.ServiceDAO.roleIdForCategory)
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

    /** Tất cả booked appointment đã quá hạn (đã qua end time) nhưng vẫn còn Pending/Confirmed */
    public List<Appointment> findOverdueActive() throws SQLException {
        String sql = "SELECT a.*, p.Name AS PetName "
                + "FROM Appointments a "
                + "JOIN Pets p ON a.PetID = p.PetID "
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

    /** Mark NoShow */
    public void markNoShow(int appointmentId) throws SQLException {
        String sql = "UPDATE Appointments SET Status = 'NoShow' WHERE AppointmentID = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, appointmentId);
            ps.executeUpdate();
        }
    }

    /** Tìm customer quá hẹn 24h, dùng đánh dấu mốc gửi email */
    public List<Appointment> findOverdueOlderThan24h() throws SQLException {
        String sql = "SELECT a.*, p.Name AS PetName "
                + "FROM Appointments a "
                + "JOIN Pets p ON a.PetID = p.PetID "
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


    /** Update Notes */
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

    /** Cancel: cập nhật status và lưu CancelReason. */
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

    // ── Attach helper ─────────────────────────────────────────────────────────

    /** Nạp danh sách AppointmentServices cho 1 nhóm appointment */
    private void attachServicesAndVaccines(List<Appointment> list) throws SQLException {
        if (list == null || list.isEmpty()) return;
        List<Integer> ids = list.stream().map(Appointment::getAppointmentID).collect(Collectors.toList());

        Map<Integer, List<AppointmentService>> svcMap = appointmentServiceDAO.findByAppointmentIds(ids);

        for (Appointment a : list) {
            a.setServices(svcMap.getOrDefault(a.getAppointmentID(), new ArrayList<>()));
        }
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private Appointment mapRow(ResultSet rs) throws SQLException {
        Appointment a = mapRowSimple(rs);
        a.setPetName(rs.getString("PetName"));
        return a;
    }

    private Appointment mapRowSimple(ResultSet rs) throws SQLException {
        Appointment a = new Appointment();
        a.setAppointmentID(rs.getInt("AppointmentID"));
        a.setCustomerID(rs.getInt("CustomerID"));
        a.setPetID(rs.getInt("PetID"));
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
