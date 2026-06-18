package com.petclinic.dao;

import com.petclinic.model.Service;
import com.petclinic.model.ServiceCategory;
import com.petclinic.util.DBConnection;

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

    public ServiceCategory findCategoryById(int id) throws SQLException {
        String sql = "SELECT * FROM ServiceCategories WHERE CategoryID = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapCategory(rs) : null;
            }
        }
    }

    // ── Services ──────────────────────────────────────────────────────────────

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

    /**
     * Count active staff who can perform services in the given category.
     * Groomer (roleId=4) → categoryId=1 only.
     * Vet    (roleId=3) → all other categories.
     */
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

    /**
     * Count staff for a specific service (looks up the service's categoryId first).
     * Used by BookingService for capacity calculation.
     */
    public int countStaffForService(int serviceId) throws SQLException {
        String sql = "SELECT CategoryID FROM Services WHERE ServiceID = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, serviceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return 1;
                return countStaffByCategoryId(rs.getInt("CategoryID"));
            }
        }
    }


    /** Retrieve categoryId for a given serviceId. Returns -1 if not found. */
    public int findCategoryIdByServiceId(int serviceId) throws SQLException {
        String sql = "SELECT CategoryID FROM Services WHERE ServiceID = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, serviceId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("CategoryID") : -1;
            }
        }
    }
}