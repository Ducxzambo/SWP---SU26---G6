package com.petclinic.dao;

import com.petclinic.model.Staff;
import com.petclinic.util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class StaffDAO {

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
}