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

    /** Count active staff who can perform a service (matched by category → roleId).
     *  Uses a heuristic: staff with RoleID = 3 (Vet) handle all clinical services.
     *  Adjust the JOIN if you have a service-to-role mapping table. */
    public int countStaffForService(int serviceId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Staff WHERE RoleID = 3 AND IsActive = 1";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 1;
        }
    }
}