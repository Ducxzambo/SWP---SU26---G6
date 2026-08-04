package com.petclinic.backend.dao;

import com.petclinic.backend.dto.StaffAttendanceSummary;
import com.petclinic.backend.model.StaffAttendance;
import com.petclinic.backend.util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class StaffAttendanceDAO {

    public List<StaffAttendance> findByDate(LocalDate date) throws SQLException {
        String sql = "SELECT a.*, s.FullName AS StaffName, r.RoleName " +
                "FROM StaffAttendance a " +
                "JOIN Staff s ON s.StaffID = a.StaffID " +
                "JOIN Roles r ON r.RoleID = s.RoleID " +
                "WHERE a.WorkDate = ? " +
                "ORDER BY a.SlotShift, s.FullName";
        List<StaffAttendance> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public StaffAttendance findById(int attendanceId) throws SQLException {
        String sql = "SELECT a.*, s.FullName AS StaffName, r.RoleName " +
                "FROM StaffAttendance a " +
                "JOIN Staff s ON s.StaffID = a.StaffID " +
                "JOIN Roles r ON r.RoleID = s.RoleID " +
                "WHERE a.AttendanceID = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, attendanceId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /** Bản ghi hiện có cho (staff, ngày, ca) — ca có thể null (nghỉ cả ngày). */
    private StaffAttendance findOne(Connection c, int staffId, LocalDate date, Integer shift) throws SQLException {
        String sql = "SELECT a.*, s.FullName AS StaffName, r.RoleName " +
                "FROM StaffAttendance a " +
                "JOIN Staff s ON s.StaffID = a.StaffID " +
                "JOIN Roles r ON r.RoleID = s.RoleID " +
                "WHERE a.StaffID = ? AND a.WorkDate = ? " +
                (shift != null ? "AND a.SlotShift = ?" : "AND a.SlotShift IS NULL");
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, staffId);
            ps.setDate(2, Date.valueOf(date));
            if (shift != null) ps.setInt(3, shift);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public void checkInShift(int staffId, LocalDate date, int shift, String status, String notes) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            StaffAttendance existing = findOne(c, staffId, date, shift);
            if (existing != null) {
                String sql = "UPDATE StaffAttendance SET Status = ?, CheckInTime = SYSDATETIME(), Notes = ? " +
                        "WHERE AttendanceID = ?";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setString(1, status);
                    ps.setString(2, notes);
                    ps.setInt(3, existing.getAttendanceID());
                    ps.executeUpdate();
                }
            } else {
                String sql = "INSERT INTO StaffAttendance (StaffID, WorkDate, SlotShift, CheckInTime, Status, Notes) " +
                        "VALUES (?, ?, ?, SYSDATETIME(), ?, ?)";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setInt(1, staffId);
                    ps.setDate(2, Date.valueOf(date));
                    ps.setInt(3, shift);
                    ps.setString(4, status);
                    ps.setString(5, notes);
                    ps.executeUpdate();
                }
            }
        }
    }

    public int markRangeAbsence(int staffId, LocalDate from, LocalDate to, String status, String notes)
            throws SQLException {
        int count = 0;
        try (Connection c = DBConnection.getConnection()) {
            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                for (int shift = 1; shift <= 4; shift++) {
                    StaffAttendance existing = findOne(c, staffId, d, shift);
                    if (existing != null) {
                        String sql = "UPDATE StaffAttendance SET Status = ?, Notes = ? WHERE AttendanceID = ?";
                        try (PreparedStatement ps = c.prepareStatement(sql)) {
                            ps.setString(1, status);
                            ps.setString(2, notes);
                            ps.setInt(3, existing.getAttendanceID());
                            ps.executeUpdate();
                        }
                    } else {
                        String sql = "INSERT INTO StaffAttendance (StaffID, WorkDate, SlotShift, Status, Notes) " +
                                "VALUES (?, ?, ?, ?, ?)";
                        try (PreparedStatement ps = c.prepareStatement(sql)) {
                            ps.setInt(1, staffId);
                            ps.setDate(2, Date.valueOf(d));
                            ps.setInt(3, shift);
                            ps.setString(4, status);
                            ps.setString(5, notes);
                            ps.executeUpdate();
                        }
                    }
                }
                count++;
            }
        }
        return count;
    }

    public int autoMarkAbsentForShift(int shift, LocalDate date, List<Integer> eligibleStaffIds) throws SQLException {
        int inserted = 0;
        try (Connection c = DBConnection.getConnection()) {
            for (int staffId : eligibleStaffIds) {
                if (findOne(c, staffId, date, shift) != null) continue;
                if (findOne(c, staffId, date, null) != null) continue; // đã có nghỉ cả ngày
                String sql = "INSERT INTO StaffAttendance (StaffID, WorkDate, SlotShift, Status, Notes) " +
                        "VALUES (?, ?, ?, 'Absent', N'Tự động đánh dấu vắng - quá giờ kết thúc ca')";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setInt(1, staffId);
                    ps.setDate(2, Date.valueOf(date));
                    ps.setInt(3, shift);
                    ps.executeUpdate();
                }
                inserted++;
            }
        }
        return inserted;
    }

    public void updateStatus(int attendanceId, String status) throws SQLException {
        String sql = "UPDATE StaffAttendance SET Status = ? WHERE AttendanceID = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, attendanceId);
            ps.executeUpdate();
        }
    }

    public List<StaffAttendance> findByDate(LocalDate date, Integer shift, String keyword) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT a.*, s.FullName AS StaffName, r.RoleName " +
                        "FROM StaffAttendance a " +
                        "JOIN Staff s ON s.StaffID = a.StaffID " +
                        "JOIN Roles r ON r.RoleID = s.RoleID " +
                        "WHERE a.WorkDate = ? ");
        if (shift != null) sql.append("AND a.SlotShift = ? ");
        if (keyword != null && !keyword.isBlank()) sql.append("AND s.FullName LIKE ? ");
        sql.append("ORDER BY a.SlotShift, s.FullName");

        List<StaffAttendance> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setDate(idx++, Date.valueOf(date));
            if (shift != null) ps.setInt(idx++, shift);
            if (keyword != null && !keyword.isBlank()) ps.setString(idx++, "%" + keyword.trim() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<StaffAttendanceSummary> getAttendanceSummary(LocalDate fromDate, LocalDate toDate) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT s.StaffID, s.FullName, r.RoleName, " +
                        "SUM(CASE WHEN a.Status = 'Late' THEN 1 ELSE 0 END) AS LateCount, " +
                        "SUM(CASE WHEN a.Status = 'Absent' THEN 1 ELSE 0 END) AS AbsentCount, " +
                        "SUM(CASE WHEN a.Status = 'OnLeave' THEN 1 ELSE 0 END) AS OnLeaveCount " +
                        "FROM Staff s " +
                        "JOIN Roles r ON r.RoleID = s.RoleID " +
                        "LEFT JOIN StaffAttendance a ON a.StaffID = s.StaffID ");
        List<Object> joinParams = new ArrayList<>();
        if (fromDate != null) { sql.append("AND a.WorkDate >= ? "); joinParams.add(Date.valueOf(fromDate)); }
        if (toDate != null)   { sql.append("AND a.WorkDate <= ? "); joinParams.add(Date.valueOf(toDate)); }
        sql.append("WHERE s.IsActive = 1 GROUP BY s.StaffID, s.FullName, r.RoleName ORDER BY s.FullName");

        List<StaffAttendanceSummary> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < joinParams.size(); i++) ps.setObject(i + 1, joinParams.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StaffAttendanceSummary sum = new StaffAttendanceSummary();
                    sum.setStaffID(rs.getInt("StaffID"));
                    sum.setStaffName(rs.getString("FullName"));
                    sum.setRoleName(rs.getString("RoleName"));
                    sum.setLateCount(rs.getInt("LateCount"));
                    sum.setAbsentCount(rs.getInt("AbsentCount"));
                    sum.setOnLeaveCount(rs.getInt("OnLeaveCount"));
                    list.add(sum);
                }
            }
        }
        return list;
    }

    private StaffAttendance mapRow(ResultSet rs) throws SQLException {
        StaffAttendance a = new StaffAttendance();
        a.setAttendanceID(rs.getInt("AttendanceID"));
        a.setStaffID(rs.getInt("StaffID"));
        Date wd = rs.getDate("WorkDate");
        if (wd != null) a.setWorkDate(wd.toLocalDate());
        int shift = rs.getInt("SlotShift");
        if (!rs.wasNull()) a.setSlotShift(shift);
        Timestamp ci = rs.getTimestamp("CheckInTime");
        if (ci != null) a.setCheckInTime(ci.toLocalDateTime());
        Timestamp co = rs.getTimestamp("CheckOutTime");
        if (co != null) a.setCheckOutTime(co.toLocalDateTime());
        a.setStatus(rs.getString("Status"));
        a.setNotes(rs.getString("Notes"));
        Timestamp ca = rs.getTimestamp("CreatedAt");
        if (ca != null) a.setCreatedAt(ca.toLocalDateTime());
        try { a.setStaffName(rs.getString("StaffName")); } catch (SQLException ignored) {}
        try { a.setRoleName(rs.getString("RoleName")); } catch (SQLException ignored) {}
        return a;
    }
}