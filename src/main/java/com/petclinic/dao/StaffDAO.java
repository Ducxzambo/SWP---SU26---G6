package com.petclinic.dao;

import com.petclinic.model.Staff;
import com.petclinic.util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
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

    public List<Staff> findActiveByRole(int roleId) throws SQLException {
        String sql = "SELECT st.*, r.RoleName FROM Staff st "
                + "JOIN Roles r ON st.RoleID = r.RoleID "
                + "WHERE st.RoleID = ? AND st.IsActive = 1 ORDER BY st.StaffID";
        List<Staff> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, roleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    /** All active veterinarians. */
    public List<Staff> findAllVets() throws SQLException {
        return findAllByRole("Veterinarian");
    }

    /** All active groomers (for receptionist grooming-checkin assignment). */
    public List<Staff> findAllGroomers() throws SQLException {
        return findAllByRole("Groomer");
    }

    public List<Staff> findAllVetsGroomers() throws SQLException {
        return findAllBy2Role("Veterinarian", "Groomer");
    }
    /** Generic role lookup. */
    private List<Staff> findAllByRole(String roleName) throws SQLException {
        String sql = """
                SELECT s.*, r.RoleName
                FROM Staff s
                JOIN Roles r ON r.RoleID = s.RoleID
                WHERE r.RoleName = ? AND s.IsActive = 1
                ORDER BY s.FullName
                """;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, roleName);
            try (ResultSet rs = ps.executeQuery()) {
                List<Staff> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        }
    }
    private List<Staff> findAllBy2Role(String roleName, String roleName2) throws SQLException {
        String sql = """
                SELECT s.*, r.RoleName
                FROM Staff s
                JOIN Roles r ON r.RoleID = s.RoleID
                WHERE r.RoleName = ? OR r.RoleName = ? AND s.IsActive = 1
                ORDER BY s.FullName
                """;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, roleName);
            ps.setString(2, roleName2);

            try (ResultSet rs = ps.executeQuery()) {
                List<Staff> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
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

    public int countAssignedInSlot(int staffId, LocalDate date, int slotShift)
            throws SQLException {
        String sql = "SELECT COUNT(*) FROM Appointments "
                + "WHERE AssignedStaffID = ? AND AppointmentDate = ? AND SlotShift = ? "
                + "AND Status NOT IN ('Cancelled','NoShow')";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, staffId);
            ps.setDate(2, Date.valueOf(date));
            ps.setInt(3, slotShift);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
    public int countAssignedOnDate(int staffId, LocalDate date) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Appointments "
                + "WHERE AssignedStaffID = ? AND AppointmentDate = ? "
                + "AND Status NOT IN ('Cancelled','NoShow')";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, staffId);
            ps.setDate(2, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}