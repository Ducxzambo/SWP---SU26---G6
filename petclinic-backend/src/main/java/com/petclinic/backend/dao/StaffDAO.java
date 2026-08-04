package com.petclinic.backend.dao;

import com.petclinic.backend.model.Role;
import com.petclinic.backend.model.Staff;
import com.petclinic.backend.util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
     * Same as findById but does NOT filter out inactive staff - the Staff
     * Management screens need to be able to view (and reactivate) someone
     * who has already been deactivated, unlike the login-time lookups above.
     */
    public Staff findByIdForManagement(int staffID) throws SQLException {
        String sql = """
                SELECT s.*, r.RoleName
                FROM Staff s
                JOIN Roles r ON r.RoleID = s.RoleID
                WHERE s.StaffID = ?
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

    public List<Role> findAllRoles() throws SQLException {
        String sql = "SELECT RoleID, RoleName FROM Roles ORDER BY RoleName";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Role> roles = new ArrayList<>();
            while (rs.next()) {
                Role r = new Role();
                r.setRoleID(rs.getInt("RoleID"));
                r.setRoleName(rs.getString("RoleName"));
                roles.add(r);
            }
            return roles;
        }
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

    /** Số appointment (chưa Cancelled/NoShow) đang gán cho staff này, đúng ca (SlotShift) trong ngày. */
    public int countAssignedInSlot(int staffId, LocalDate date, int slotShift)
            throws SQLException {
        String sql = "SELECT COUNT(DISTINCT a.AppointmentID) FROM Appointments a "
                + "JOIN AppointmentServices asvc ON asvc.AppointmentID = a.AppointmentID "
                + "WHERE asvc.AssignedStaffID = ? AND a.AppointmentDate = ? AND a.SlotShift = ? "
                + "AND a.Status NOT IN ('Cancelled','NoShow')";
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

    /** Số appointment (chưa Cancelled/NoShow) đang gán cho staff này trong CẢ NGÀY (mọi slot). */
    public int countAssignedOnDate(int staffId, LocalDate date) throws SQLException {
        String sql = "SELECT COUNT(DISTINCT a.AppointmentID) FROM Appointments a "
                + "JOIN AppointmentServices asvc ON asvc.AppointmentID = a.AppointmentID "
                + "WHERE asvc.AssignedStaffID = ? AND a.AppointmentDate = ? "
                + "AND a.Status NOT IN ('Cancelled','NoShow')";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, staffId);
            ps.setDate(2, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private Staff mapRow(ResultSet rs) throws SQLException {
        Staff s = new Staff();
        s.setStaffID(rs.getInt("StaffID"));
        s.setFullName(rs.getString("FullName"));
        s.setEmail(rs.getString("Email"));
        s.setPhone(rs.getString("Phone"));
        s.setPasswordHash(rs.getString("PasswordHash"));
        s.setRoleID(rs.getInt("RoleID"));
        s.setSpecialization(rs.getString("Specialization"));
        s.setLicenseNumber(rs.getString("LicenseNumber"));
        Date hd = rs.getDate("HireDate");
        if (hd != null) s.setHireDate(hd.toLocalDate());
        s.setActive(rs.getBoolean("IsActive"));
        Timestamp ts = rs.getTimestamp("CreatedAt");
        if (ts != null) s.setCreatedAt(ts.toLocalDateTime());
        try { s.setRoleName(rs.getString("RoleName")); } catch (SQLException ignored) {
        }
        return s;
    }

    /**
     * Search/browse for the Staff Management list screen.
     *
     * @param keyword      matched against FullName/Email (nullable/blank = no filter)
     * @param roleName     exact role match, e.g. "Veterinarian" (nullable/blank = all roles)
     * @param statusFilter "active" | "inactive" (nullable/blank = both)
     */
    public List<Staff> search(String keyword, String roleName, String statusFilter) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT s.*, r.RoleName
                FROM Staff s
                JOIN Roles r ON r.RoleID = s.RoleID
                WHERE 1 = 1
                """);
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (s.FullName LIKE ? OR s.Email LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
        }
        if (roleName != null && !roleName.isBlank()) {
            sql.append(" AND r.RoleName = ?");
            params.add(roleName.trim());
        }
        if ("active".equals(statusFilter)) {
            sql.append(" AND s.IsActive = 1");
        } else if ("inactive".equals(statusFilter)) {
            sql.append(" AND s.IsActive = 0");
        }
        sql.append(" ORDER BY s.IsActive DESC, s.FullName");

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<Staff> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        }
    }

    private void bindParams(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    /**
     * Inserts a brand-new staff account. staff.getPasswordHash() must already
     * hold a hashed password (see StaffService/PasswordUtil) - this DAO never
     * hashes anything itself.
     */
    public int insert(Staff staff) throws SQLException {
        String sql = """
                INSERT INTO Staff (FullName, Email, Phone, PasswordHash, RoleID,
                                    Specialization, LicenseNumber, HireDate, IsActive, CreatedAt)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, SYSUTCDATETIME())
                """;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, staff.getFullName());
            ps.setString(2, staff.getEmail().trim().toLowerCase());
            ps.setString(3, staff.getPhone());
            ps.setString(4, staff.getPasswordHash());
            ps.setInt(5, staff.getRoleID());
            ps.setString(6, staff.getSpecialization());
            ps.setString(7, staff.getLicenseNumber());
            LocalDate hireDate = staff.getHireDate() != null ? staff.getHireDate() : LocalDate.now();
            ps.setDate(8, Date.valueOf(hireDate));
            ps.setBoolean(9, staff.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Failed to retrieve generated StaffID.");
    }

    /**
     * Updates profile fields only. Password changes go through updatePassword()
     * so a routine profile edit can never accidentally touch credentials.
     */
    public void update(Staff staff) throws SQLException {
        String sql = """
                UPDATE Staff
                SET FullName = ?, Email = ?, Phone = ?, RoleID = ?,
                    Specialization = ?, LicenseNumber = ?, HireDate = ?
                WHERE StaffID = ?
                """;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, staff.getFullName());
            ps.setString(2, staff.getEmail().trim().toLowerCase());
            ps.setString(3, staff.getPhone());
            ps.setInt(4, staff.getRoleID());
            ps.setString(5, staff.getSpecialization());
            ps.setString(6, staff.getLicenseNumber());
            if (staff.getHireDate() != null) {
                ps.setDate(7, Date.valueOf(staff.getHireDate()));
            } else {
                ps.setNull(7, Types.DATE);
            }
            ps.setInt(8, staff.getStaffID());
            if (ps.executeUpdate() == 0) {
                throw new SQLException("Staff not found: " + staff.getStaffID());
            }
        }
    }

    public void updatePassword(int staffID, String newPasswordHash) throws SQLException {
        String sql = "UPDATE Staff SET PasswordHash = ? WHERE StaffID = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newPasswordHash);
            ps.setInt(2, staffID);
            if (ps.executeUpdate() == 0) {
                throw new SQLException("Staff not found: " + staffID);
            }
        }
    }

    public void setActive(int staffID, boolean active) throws SQLException {
        String sql = "UPDATE Staff SET IsActive = ? WHERE StaffID = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setInt(2, staffID);
            if (ps.executeUpdate() == 0) {
                throw new SQLException("Staff not found: " + staffID);
            }
        }
    }

    public boolean emailExists(String email, Integer excludeStaffID) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Staff WHERE Email = ?"
                + (excludeStaffID != null ? " AND StaffID <> ?" : "");
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase());
            if (excludeStaffID != null) ps.setInt(2, excludeStaffID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
}