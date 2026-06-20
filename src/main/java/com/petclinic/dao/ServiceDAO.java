package com.petclinic.dao;

import com.petclinic.model.Service;
import com.petclinic.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for Services and ServiceCategories.
 */
public class ServiceDAO {

    /**
     * All active services under a given category name.
     */
    public List<Service> findByCategory(String categoryName) throws SQLException {
        String sql = """
                SELECT s.ServiceID, s.Name, s.Price, s.DurationMinutes, s.IsActive,
                       sc.Name AS CategoryName
                FROM Services s
                JOIN ServiceCategories sc ON sc.CategoryID = s.CategoryID
                WHERE sc.Name = ? AND s.IsActive = 1
                ORDER BY s.ServiceID
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, categoryName);
            return mapList(ps.executeQuery());
        }
    }

    /**
     * All active diagnostic (xét nghiệm) services.
     */
    public List<Service> findLabTests() throws SQLException {
        return findByCategory("Chẩn đoán");
    }

    /**
     * All active treatment plan services.
     */
    public List<Service> findTreatmentPlans() throws SQLException {
        return findByCategory("Phác đồ điều trị");
    }

    /**
     * Count pets booked in a given shift on a given date (for slot limit check).
     */
    public int countSlotBookings(LocalDateShift key) throws SQLException {
        String sql = """
                SELECT COUNT(*) FROM Appointments
                WHERE AppointmentDate = ?
                  AND SlotShift       = ?
                  AND Status NOT IN ('Cancelled','NoShow')
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(key.date()));
            ps.setInt(2, key.shift());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // ── Mapping ───────────────────────────────────────────────────────────────
    private List<Service> mapList(ResultSet rs) throws SQLException {
        List<Service> list = new ArrayList<>();
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    private Service mapRow(ResultSet rs) throws SQLException {
        Service s = new Service();
        s.setServiceID(rs.getInt("ServiceID"));
        s.setName(rs.getString("Name"));
        s.setPrice(rs.getBigDecimal("Price"));
        s.setDurationMinutes(rs.getInt("DurationMinutes"));
        s.setActive(rs.getBoolean("IsActive"));
        try {
            s.setCategoryName(rs.getString("CategoryName"));
        } catch (SQLException ignored) {
        }
        return s;
    }

    // ── tiny record for shift key ─────────────────────────────────────────────
    public record LocalDateShift(java.time.LocalDate date, int shift) {
    }
}