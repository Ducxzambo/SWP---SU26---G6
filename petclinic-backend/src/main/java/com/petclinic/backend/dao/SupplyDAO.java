package com.petclinic.backend.dao;

import com.petclinic.backend.model.Supply;
import com.petclinic.backend.model.SupplyUsageItem;
import com.petclinic.backend.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SupplyDAO {

    // ── Supplies catalog ──────────────────────────────────────────────────────

    public List<Supply> findAllInStock() throws SQLException {
        String sql = "SELECT * FROM Supplies WHERE StockQty > 0 AND IsActive = 1 ORDER BY Name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Supply> list = new ArrayList<>();
            while (rs.next()) list.add(mapSupply(rs));
            return list;
        }
    }

    public Supply findById(int supplyID) throws SQLException {
        String sql = "SELECT * FROM Supplies WHERE SupplyID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, supplyID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapSupply(rs) : null;
            }
        }
    }

    /** Trừ kho — phải dùng chung Connection với transaction của caller. */
    public void deductStock(Connection conn, int supplyID, int qty) throws SQLException {
        String sql = "UPDATE Supplies SET StockQty = StockQty - ? " +
                "WHERE SupplyID = ? AND StockQty >= ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, qty);
            ps.setInt(2, supplyID);
            ps.setInt(3, qty);
            int updated = ps.executeUpdate();
            if (updated == 0)
                throw new SQLException("Insufficient stock for SupplyID=" + supplyID);
        }
    }

    // ── SupplyUsageItems ──────────────────────────────────────────────────────

    /**
     * Lưu toàn bộ vật tư đã dùng trong 1 appointment — transactional:
     * INSERT SupplyUsageItems + deductStock + INSERT StockTransactions.
     */
    public void saveUsageItems(int appointmentID, int staffID,
                               List<SupplyUsageItem> items) throws SQLException {
        if (items == null || items.isEmpty()) return;

        Connection conn = DBConnection.getConnection();
        try {
            conn.setAutoCommit(false);

            for (SupplyUsageItem item : items) {
                item.setAppointmentID(appointmentID);
                item.setStaffID(staffID);

                // snapshot giá nếu chưa có
                if (item.getUnitPrice() == null ||
                        item.getUnitPrice().compareTo(java.math.BigDecimal.ZERO) == 0) {
                    Supply sup = findById(item.getSupplyID());
                    if (sup != null) item.setUnitPrice(sup.getUnitPrice());
                }

                insertUsageItem(conn, item);
                deductStock(conn, item.getSupplyID(), item.getQuantity().intValue());
                insertStockTransaction(conn, item, staffID);
            }

            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
            conn.close();
        }
    }

    public List<SupplyUsageItem> findByAppointmentId(int appointmentID) throws SQLException {
        String sql = """
                SELECT sui.*, s.Name AS SupplyName, s.Unit AS SupplyUnit
                FROM SupplyUsageItems sui
                JOIN Supplies s ON s.SupplyID = sui.SupplyID
                WHERE sui.AppointmentID = ?
                ORDER BY sui.UsedAt
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, appointmentID);
            try (ResultSet rs = ps.executeQuery()) {
                List<SupplyUsageItem> list = new ArrayList<>();
                while (rs.next()) list.add(mapUsageItem(rs));
                return list;
            }
        }
    }

    /** Tổng tiền vật tư của 1 appointment — dùng để cộng vào hóa đơn. */
    public java.math.BigDecimal sumByAppointmentId(int appointmentID) throws SQLException {
        String sql = "SELECT ISNULL(SUM(Quantity * UnitPrice), 0) AS Total " +
                "FROM SupplyUsageItems WHERE AppointmentID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, appointmentID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal("Total") : java.math.BigDecimal.ZERO;
            }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    public void insertUsageItem(Connection conn, SupplyUsageItem item) throws SQLException {
        String sql = "INSERT INTO SupplyUsageItems " +
                "(AppointmentID, SupplyID, StaffID, Quantity, UnitPrice, Notes) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, item.getAppointmentID());
            ps.setInt(2, item.getSupplyID());
            ps.setInt(3, 4);
            ps.setBigDecimal(4, item.getQuantity());
            ps.setBigDecimal(5, item.getUnitPrice());
            ps.setString(6, item.getNotes());
            ps.executeUpdate();
        }
    }

    public void insertStockTransaction(Connection conn, SupplyUsageItem item,
                                        int performedByID) throws SQLException {
        String sql = "INSERT INTO StockTransactions " +
                "(ItemType, ItemID, QuantityChange, Reason, PerformedByID, " +
                "TransactionDate, ProviderID, TransactionType, PurchasePrice) " +
                "VALUES ('Supply', ?, ?, 'Used', ?, GETDATE(), NULL, 'Export', NULL)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, item.getSupplyID());
            ps.setBigDecimal(2, item.getQuantity().negate()); // âm = xuất kho
            ps.setInt(3, performedByID);
            ps.executeUpdate();
        }
    }

    private Supply mapSupply(ResultSet rs) throws SQLException {
        Supply s = new Supply();
        s.setSupplyID(rs.getInt("SupplyID"));
        s.setName(rs.getString("Name"));
        s.setUnit(rs.getString("Unit"));
        s.setUnitPrice(rs.getBigDecimal("UnitPrice"));
        s.setStockQty(rs.getInt("StockQty"));
        s.setActive(rs.getBoolean("IsActive"));
        return s;
    }

    private SupplyUsageItem mapUsageItem(ResultSet rs) throws SQLException {
        SupplyUsageItem i = new SupplyUsageItem();
        i.setUsageID(rs.getInt("UsageID"));
        i.setAppointmentID(rs.getInt("AppointmentID"));
        i.setSupplyID(rs.getInt("SupplyID"));
        i.setStaffID(rs.getInt("StaffID"));
        i.setQuantity(rs.getBigDecimal("Quantity"));
        i.setUnitPrice(rs.getBigDecimal("UnitPrice"));
        i.setNotes(rs.getString("Notes"));
        Timestamp ts = rs.getTimestamp("UsedAt");
        if (ts != null) i.setUsedAt(ts.toLocalDateTime());
        try { i.setSupplyName(rs.getString("SupplyName")); } catch (SQLException ignored) {}
        try { i.setSupplyUnit(rs.getString("SupplyUnit")); } catch (SQLException ignored) {}
        return i;
    }
}