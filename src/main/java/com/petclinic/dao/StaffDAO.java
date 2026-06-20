package com.petclinic.dao;

import com.petclinic.model.Staff;
import com.petclinic.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for the Staff + Roles tables.
 */
public class StaffDAO {

    // ── Lookup ────────────────────────────────────────────────────────────────

    public Staff findByEmail(String email) throws SQLException {
        String sql = """
                SELECT s.*, r.RoleName
                FROM Staff s
                JOIN Roles r ON r.RoleID = s.RoleID
                WHERE s.Email = ? AND s.IsActive = 1
                """;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public Staff findById(int staffID) throws SQLException {
        String sql = """
                SELECT s.*, r.RoleName
                FROM Staff s
                JOIN Roles r ON r.RoleID = s.RoleID
                WHERE s.StaffID = ? AND s.IsActive = 1
                """;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, staffID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /**
     * All active veterinarians (for receptionist vet assignment drop-down).
     */
    public List<Staff> findAllVets() throws SQLException {
        String sql = """
                SELECT s.*, r.RoleName
                FROM Staff s
                JOIN Roles r ON r.RoleID = s.RoleID
                WHERE r.RoleName = 'Veterinarian' AND s.IsActive = 1
                ORDER BY s.FullName
                """;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Staff> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;
        }
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private Staff mapRow(ResultSet rs) throws SQLException {
        Staff s = new Staff();
        s.setStaffID(rs.getInt("StaffID"));
        s.setFullName(rs.getString("FullName"));
        s.setEmail(rs.getString("Email"));
        s.setPhone(rs.getString("Phone"));
        s.setPasswordHash(rs.getString("PasswordHash"));
        s.setRoleID(rs.getInt("RoleID"));
        s.setRoleName(rs.getString("RoleName"));
        s.setSpecialization(rs.getString("Specialization"));
        s.setLicenseNumber(rs.getString("LicenseNumber"));
        s.setActive(rs.getBoolean("IsActive"));
        return s;
    }

    public List<Staff> findAllGroomers() throws SQLException{
        String sql = """
                SELECT s.*, r.RoleName
                FROM Staff s
                JOIN Roles r ON r.RoleID = s.RoleID
                WHERE r.RoleName = 'Groomer' AND s.IsActive = 1
                ORDER BY s.FullName
                """;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Staff> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;
        }
    }
}
