package com.petclinic.backend.dao;

import com.petclinic.backend.service.BookingService;
import com.petclinic.backend.model.Service;
import com.petclinic.backend.model.ServiceCategory;
import com.petclinic.backend.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceDAO {

    // ── Categories ────────────────────────────────────────────────────────────

    public List<ServiceCategory> findAllCategories() throws SQLException {
        String sql = "SELECT * FROM ServiceCategories ORDER BY Name";
        List<ServiceCategory> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapCategory(rs));
        }
        return list;
    }

    /** Load all categories each with their active services populated. */
    public List<ServiceCategory> findAllCategoriesWithServices() throws SQLException {
        List<ServiceCategory> cats = findAllCategories();
        for (ServiceCategory cat : cats) {
            cat.setServices(findByCategory(cat.getCategoryID()));
        }
        return cats;
    }

    /**
     * Tên các nhóm dịch vụ KHÔNG được phép chọn qua booking wizard của khách
     */
    private static final java.util.Set<String> BOOKING_EXCLUDED_CATEGORY_NAMES =
            java.util.Set.of("Điều trị", "Chẩn đoán");

    /**
     * Danh sách category dùng cho booking wizard của khách
     */
    public List<ServiceCategory> findBookableCategoriesWithServices() throws SQLException {
        List<ServiceCategory> cats = findAllCategoriesWithServices();
        cats.removeIf(c -> c.getCategoryID() == BookingService.INPATIENT_CATEGORY_ID
                || BOOKING_EXCLUDED_CATEGORY_NAMES.contains(c.getName()));
        return cats;
    }

    /**
     * Service "đại diện" đầu tiên đang active của 1 category — dùng cho các
     * category chỉ cần MỘT dòng AppointmentServices duy nhất để đánh dấu
     * (Vaccine, Dịch vụ nội trú)
     */
    public Service findFirstActiveByCategory(int categoryId) throws SQLException {
        List<Service> list = findByCategory(categoryId);
        return list.isEmpty() ? null : list.get(0);
    }

    // ── Services ──────────────────────────────────────────────────────────────

    /** All active services under a given category name. */
    public List<Service> findByCategory(String categoryName) throws SQLException {
        String sql = """
                SELECT s.ServiceID, s.Name, s.Price, s.DurationMinutes, s.IsActive,
                       sc.Name AS CategoryName, sc.CategoryID AS CategoryID
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

    public List<Service> findByCategory(int categoryId) throws SQLException {
        String sql = "SELECT * FROM Services WHERE CategoryID = ? AND IsActive = 1 ORDER BY Name";
        List<Service> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapService(rs));
            }
        }
        return list;
    }

    public Service findById(int serviceId) throws SQLException {
        String sql = "SELECT * FROM Services WHERE ServiceID = ? AND IsActive = 1";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, serviceId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapService(rs) : null;
            }
        }
    }

    public List<Service> findByIds(List<Integer> ids) throws SQLException {
        if (ids == null || ids.isEmpty()) return new ArrayList<>();
        StringBuilder sb = new StringBuilder("SELECT * FROM Services WHERE ServiceID IN (");
        for (int i = 0; i < ids.size(); i++) { sb.append(i > 0 ? ",?" : "?"); }
        sb.append(") AND IsActive = 1");
        List<Service> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sb.toString())) {
            for (int i = 0; i < ids.size(); i++) ps.setInt(i + 1, ids.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapService(rs));
            }
        }
        return list;
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private ServiceCategory mapCategory(ResultSet rs) throws SQLException {
        return new ServiceCategory(rs.getInt("CategoryID"), rs.getString("Name"));
    }
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
        try { s.setCategoryName(rs.getString("CategoryName")); } catch (SQLException ignored) {}
        try { s.setCategoryID(rs.getInt("CategoryID")); } catch (SQLException ignored) {}
        return s;
    }

    private Service mapService(ResultSet rs) throws SQLException {
        Service s = new Service();
        s.setServiceID(rs.getInt("ServiceID"));
        s.setCategoryID(rs.getInt("CategoryID"));
        s.setName(rs.getString("Name"));
        s.setPrice(rs.getBigDecimal("Price"));
        s.setDurationMinutes(rs.getInt("DurationMinutes"));
        s.setActive(rs.getBoolean("IsActive"));
        return s;
    }

    // ── Staff capacity helpers ────────────────────────────────────────────────

    /** RoleID rule: Groomer (4) handles ServiceCategoryID=3; Vet (3) handles everything else. */
    public static int roleIdForCategory(int categoryId) {
        return categoryId == 3 ? 4 : 3;
    }

    public int countStaffByRoleId(int roleId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Staff WHERE RoleID = ? AND IsActive = 1";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, roleId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Math.max(1, rs.getInt(1)) : 1;
            }
        }
    }

    /** All active services across all categories (for general dropdowns, e.g. walk-in). */
    public List<Service> findAllActive() throws SQLException {
        String sql = """
                SELECT s.ServiceID, s.Name, s.Price, s.DurationMinutes, s.IsActive,
                       sc.Name AS CategoryName, sc.CategoryID AS CategoryID
                FROM Services s
                JOIN ServiceCategories sc ON sc.CategoryID = s.CategoryID
                WHERE s.IsActive = 1
                  AND sc.Name NOT IN ('Chẩn đoán', 'Điều trị')
                ORDER BY sc.Name, s.ServiceID
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return mapList(rs);
        }
    }

    /** All active diagnostic (xét nghiệm) services. */
    public List<Service> findLabTests() throws SQLException {
        return findByCategory("Chẩn đoán");
    }

    /** All active treatment plan services. */
    public List<Service> findTreatmentPlans() throws SQLException {
        return findByCategory("Điều trị");
    }
}