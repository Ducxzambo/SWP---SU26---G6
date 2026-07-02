package com.petclinic.dao;

import com.petclinic.model.Service;
import com.petclinic.model.ServiceCategory;
import com.petclinic.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for Services and ServiceCategories.
 */
public class ServiceDAO {

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

    /** All active services under a given CategoryID — dùng cho walk-in (chọn category trước). */
    public List<Service> findByCategoryId(int categoryID) throws SQLException {
        String sql = """
                SELECT s.ServiceID, s.Name, s.Price, s.DurationMinutes, s.IsActive,
                       sc.Name AS CategoryName, sc.CategoryID AS CategoryID
                FROM Services s
                JOIN ServiceCategories sc ON sc.CategoryID = s.CategoryID
                WHERE s.CategoryID = ? AND s.IsActive = 1
                ORDER BY s.ServiceID
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, categoryID);
            return mapList(ps.executeQuery());
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
                  AND sc.Name NOT IN ('Chẩn đoán', 'Phác đồ điều trị')
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

    /** Tìm 1 Service theo ID — dùng để snapshot giá tại thời điểm khám. */
    public Service findById(int serviceID) throws SQLException {
        String sql = """
                SELECT s.ServiceID, s.Name, s.Price, s.DurationMinutes, s.IsActive,
                       sc.Name AS CategoryName, sc.CategoryID AS CategoryID
                FROM Services s
                JOIN ServiceCategories sc ON sc.CategoryID = s.CategoryID
                WHERE s.ServiceID = ?
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, serviceID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /**
     * Tất cả ServiceCategories — dùng để hiển thị dropdown "Loại dịch vụ" đầu tiên
     * trong walk-in, trước khi chọn dịch vụ cụ thể.
     */
    public List<ServiceCategory> findAllCategories() throws SQLException {
        String sql = "SELECT CategoryID, Name FROM ServiceCategories ORDER BY CategoryID";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<ServiceCategory> list = new ArrayList<>();
            while (rs.next()) {
                ServiceCategory c = new ServiceCategory();
                c.setCategoryID(rs.getInt("CategoryID"));
                c.setName(rs.getString("Name"));
                list.add(c);
            }
            return list;
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
        try { s.setCategoryName(rs.getString("CategoryName")); } catch (SQLException ignored) {}
        try { s.setCategoryID(rs.getInt("CategoryID")); } catch (SQLException ignored) {}
        return s;
    }

    public static int roleIdForCategory(int categoryId) {
        return categoryId == 3 ? 4 : 3;
    }

    public List<ServiceCategory> findAllCategoriesWithServices() throws SQLException {
        List<ServiceCategory> cats = findAllCategories();
        for (ServiceCategory cat : cats) {
            cat.setServices(findByCategory(cat.getCategoryID()));
        }
        return cats;
    }

    public int countStaffByCategoryId(int categoryId) throws SQLException {
        int roleId = roleIdForCategory(categoryId);
        String sql = "SELECT COUNT(*) FROM Staff WHERE RoleID = ? AND IsActive = 1";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, roleId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Math.max(1, rs.getInt(1)) : 1;
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
}