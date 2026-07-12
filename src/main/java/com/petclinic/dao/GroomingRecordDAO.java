package com.petclinic.dao;

import com.petclinic.model.Appointment;
import com.petclinic.model.GroomingRecord;
import com.petclinic.model.MedicalRecord;
import com.petclinic.util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Data access cho GroomingRecords + hàng chờ Groomer.
 *
 * Theo schema AppointmentServices (N-N): Appointments KHÔNG có ServiceID hay
 * AssignedStaffID trực tiếp — mỗi dịch vụ trong 1 appointment là 1 dòng riêng
 * trong AppointmentServices, kèm AssignedStaffID RIÊNG cho dòng đó. Nhờ vậy
 * 1 appointment có thể trộn dịch vụ Khám + Grooming, mỗi loại có nhân viên
 * phụ trách khác nhau.
 *
 * DAO này KHÔNG tự viết lại SQL join Appointments/AppointmentServices/Services
 * — mọi truy vấn hàng chờ đều ủy quyền cho AppointmentDAO (đã có sẵn
 * findStaffQueue/findUnassignedArrived/findStaffCompletedToday tổng quát,
 * dùng chung được cho cả Vet lẫn Groomer qua tham số categoryFilter).
 * GroomingRecordDAO chỉ lo phần CRUD bảng GroomingRecords + việc gộp
 * (merge) danh sách "đã gán cho tôi" và "chưa ai nhận" cho đúng nghiệp vụ
 * self-assign của groomer.
 */
public class GroomingRecordDAO {

    public static final String CATEGORY_GROOMING = "Grooming";

    private final AppointmentDAO appointmentDAO = new AppointmentDAO();

    // ── Read: GroomingRecords ───────────────────────────────────────────────────

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

    public List<GroomingRecord> findByPet(int petId) throws SQLException {
        String sql = "SELECT mr.* "
                + "FROM GroomingRecord gr "
                + "WHERE gr.PetID = ? Order by gr.AppointmentID desc";
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

    /** Lịch sử spa của 1 thú cưng — groomer xem lại trước khi bắt đầu phiên mới. */
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

    // ── Hàng chờ Groomer (ủy quyền AppointmentDAO, category = Grooming) ─────────

    /**
     * Hàng chờ của 1 groomer trong 1 ngày: gồm 2 nhóm gộp lại (loại trùng theo AppointmentID)
     *  1. Appointment có ÍT NHẤT 1 dòng dịch vụ Grooming ĐANG GÁN cho groomerID (Arrived/InProgress)
     *  2. Appointment có ÍT NHẤT 1 dòng dịch vụ Grooming CHƯA AI NHẬN (self-assign pool)
     */
    public List<Appointment> findGroomerQueue(int groomerID, LocalDate date) throws SQLException {
        List<Appointment> assigned   = appointmentDAO.findStaffQueue(groomerID, date, CATEGORY_GROOMING);
        List<Appointment> unassigned = appointmentDAO.findUnassignedArrived(date, CATEGORY_GROOMING);
        return mergeDistinctById(assigned, unassigned);
    }

    /** Các ca grooming đã hoàn thành (Done) của 1 groomer trong 1 ngày, kèm RecordID để xem lại. */
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

    // ── Write: GroomingRecords ───────────────────────────────────────────────────

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

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * BASE_SELECT lấy ServiceName qua AppointmentServices (N-N) thay vì
     * Appointments.ServiceID trực tiếp — 1 appointment có thể có nhiều dịch vụ,
     * nên lấy dịch vụ Grooming đầu tiên gắn với appointment đó để hiển thị.
     */
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