package com.petclinic.backend.dao;

import com.petclinic.backend.model.ShiftCapacity;
import com.petclinic.backend.util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public class ShiftCapacityDAO {

    public ShiftCapacity findByDateAndShift(LocalDate date, int shift) throws SQLException {
        String sql = "SELECT * FROM ShiftCapacity WHERE CapacityDate = ? AND SlotShift = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            ps.setInt(2, shift);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public Map<LocalDate, Map<Integer, ShiftCapacity>> findByDateRange(LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT * FROM ShiftCapacity WHERE CapacityDate BETWEEN ? AND ? ORDER BY CapacityDate, SlotShift";
        Map<LocalDate, Map<Integer, ShiftCapacity>> result = new LinkedHashMap<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ShiftCapacity sc = mapRow(rs);
                    result.computeIfAbsent(sc.getCapacityDate(), k -> new LinkedHashMap<>())
                            .put(sc.getSlotShift(), sc);
                }
            }
        }
        return result;
    }

    public void upsert(LocalDate date, int shift, int groomCap, int vetCap, Integer staffId) throws SQLException {
        String updateSql = "UPDATE ShiftCapacity SET GroomCap = ?, VetCap = ?, UpdatedByID = ?, UpdatedAt = SYSDATETIME() " +
                "WHERE CapacityDate = ? AND SlotShift = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(updateSql)) {
            ps.setInt(1, groomCap);
            ps.setInt(2, vetCap);
            if (staffId != null) ps.setInt(3, staffId); else ps.setNull(3, Types.INTEGER);
            ps.setDate(4, Date.valueOf(date));
            ps.setInt(5, shift);
            if (ps.executeUpdate() > 0) return;
        }
        String insertSql = "INSERT INTO ShiftCapacity (CapacityDate, SlotShift, GroomCap, VetCap, UpdatedByID) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(insertSql)) {
            ps.setDate(1, Date.valueOf(date));
            ps.setInt(2, shift);
            ps.setInt(3, groomCap);
            ps.setInt(4, vetCap);
            if (staffId != null) ps.setInt(5, staffId); else ps.setNull(5, Types.INTEGER);
            ps.executeUpdate();
        }
    }

    private ShiftCapacity mapRow(ResultSet rs) throws SQLException {
        ShiftCapacity sc = new ShiftCapacity();
        sc.setShiftCapacityID(rs.getInt("ShiftCapacityID"));
        Date d = rs.getDate("CapacityDate");
        if (d != null) sc.setCapacityDate(d.toLocalDate());
        sc.setSlotShift(rs.getInt("SlotShift"));
        sc.setGroomCap(rs.getInt("GroomCap"));
        sc.setVetCap(rs.getInt("VetCap"));
        int uid = rs.getInt("UpdatedByID");
        if (!rs.wasNull()) sc.setUpdatedByID(uid);
        Timestamp ts = rs.getTimestamp("UpdatedAt");
        if (ts != null) sc.setUpdatedAt(ts.toLocalDateTime());
        return sc;
    }
}