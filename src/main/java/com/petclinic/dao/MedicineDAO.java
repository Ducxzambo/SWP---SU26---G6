package com.petclinic.dao;

import com.petclinic.model.InventoryItem;
import com.petclinic.model.Medicine;
import com.petclinic.model.StockMovementReport;
import com.petclinic.model.StockTransaction;
import com.petclinic.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for Medicines plus BP-07 stock inventory.
 */
public class MedicineDAO {

    private static final int DEFAULT_VACCINE_THRESHOLD = 10;

    public List<Medicine> findAllInStock() throws SQLException {
        String sql = "SELECT * FROM Medicines WHERE StockQty > 0 ORDER BY Name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Medicine> list = new ArrayList<>();
            while (rs.next()) list.add(mapMedicine(rs));
            return list;
        }
    }

    public List<Medicine> findAll() throws SQLException {
        String sql = "SELECT * FROM Medicines ORDER BY Name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Medicine> list = new ArrayList<>();
            while (rs.next()) list.add(mapMedicine(rs));
            return list;
        }
    }

    public Medicine findById(int medicineID) throws SQLException {
        String sql = "SELECT * FROM Medicines WHERE MedicineID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, medicineID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapMedicine(rs) : null;
            }
        }
    }

    public List<InventoryItem> searchInventory(String keyword, String itemType,
                                               String stockLevel)
            throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT *
                FROM (
                    SELECT 'Medicine' AS ItemType,
                           MedicineID AS ItemID,
                           Name,
                           Unit,
                           UnitPrice,
                           StockQty,
                           MinStockLevel
                    FROM Medicines
                    UNION ALL
                    SELECT 'Vaccine' AS ItemType,
                           VaccineID AS ItemID,
                           Name,
                           CAST(NULL AS NVARCHAR(30)) AS Unit,
                           UnitPrice,
                           StockQty,
                           CAST(? AS INT) AS MinStockLevel
                    FROM Vaccines
                ) inv
                WHERE 1 = 1
                """);
        List<Object> params = new ArrayList<>();
        params.add(DEFAULT_VACCINE_THRESHOLD);

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND inv.Name LIKE ?");
            params.add("%" + keyword.trim() + "%");
        }
        if (itemType != null && !itemType.isBlank()) {
            sql.append(" AND inv.ItemType = ?");
            params.add(itemType.trim());
        }
        if (stockLevel != null && !stockLevel.isBlank()) {
            switch (stockLevel) {
                case "available":
                    sql.append(" AND inv.StockQty > 0");
                    break;
                case "low":
                    sql.append(" AND (inv.StockQty <= 0 OR inv.StockQty < inv.MinStockLevel)");
                    break;
                case "out":
                    sql.append(" AND inv.StockQty <= 0");
                    break;
                default:
                    break;
            }
        }

        sql.append("""
                 ORDER BY
                   CASE
                     WHEN inv.StockQty <= 0 THEN 0
                     WHEN inv.StockQty < inv.MinStockLevel THEN 1
                     ELSE 2
                   END,
                   inv.ItemType,
                   inv.Name
                """);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<InventoryItem> list = new ArrayList<>();
                while (rs.next()) list.add(mapInventoryItem(rs));
                return list;
            }
        }
    }

    public List<InventoryItem> findLowStock() throws SQLException {
        return searchInventory(null, null, "low");
    }

    public int stockIn(String itemType, Integer itemID, String itemName, String unit,
                       BigDecimal unitPrice, int quantity, int minStockLevel,
                       int performedByID) throws SQLException {
        Connection conn = DBConnection.getConnection();
        try {
            conn.setAutoCommit(false);

            int savedItemID;
            if ("Medicine".equals(itemType)) {
                savedItemID = stockInMedicine(conn, itemID, itemName, unit, unitPrice,
                        quantity, minStockLevel);
            } else if ("Vaccine".equals(itemType)) {
                savedItemID = stockInVaccine(conn, itemID, itemName, unitPrice, quantity);
            } else {
                throw new SQLException("Unsupported stock item type: " + itemType);
            }

            insertStockTransaction(conn, itemType, savedItemID,
                    BigDecimal.valueOf(quantity), "Purchase", performedByID);

            conn.commit();
            return savedItemID;
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
            conn.close();
        }
    }

    public void updateThreshold(int medicineID, int minStockLevel) throws SQLException {
        String sql = "UPDATE Medicines SET MinStockLevel = ? WHERE MedicineID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, minStockLevel);
            ps.setInt(2, medicineID);
            ps.executeUpdate();
        }
    }

    public List<StockTransaction> findStockTransactions(LocalDate fromDate,
                                                        LocalDate toDate,
                                                        int limit)
            throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT TOP (?) st.TransactionID,
                       st.ItemType,
                       st.ItemID,
                       COALESCE(m.Name, v.Name) AS ItemName,
                       st.QuantityChange,
                       st.Reason,
                       st.PerformedByID,
                       staff.FullName AS PerformedByName,
                       st.TransactionDate
                FROM StockTransactions st
                LEFT JOIN Medicines m ON st.ItemType = 'Medicine' AND st.ItemID = m.MedicineID
                LEFT JOIN Vaccines v ON st.ItemType = 'Vaccine' AND st.ItemID = v.VaccineID
                LEFT JOIN Staff staff ON staff.StaffID = st.PerformedByID
                WHERE st.ItemType IN ('Medicine', 'Vaccine')
                """);
        List<Object> params = new ArrayList<>();
        params.add(limit);
        appendDateFilters(sql, params, fromDate, toDate);
        sql.append(" ORDER BY st.TransactionDate DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<StockTransaction> list = new ArrayList<>();
                while (rs.next()) list.add(mapTransaction(rs));
                return list;
            }
        }
    }

    public List<StockMovementReport> getMovementReport(LocalDate fromDate,
                                                       LocalDate toDate)
            throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT st.ItemType,
                       st.ItemID,
                       COALESCE(m.Name, v.Name) AS ItemName,
                       SUM(CASE WHEN st.QuantityChange > 0 THEN st.QuantityChange ELSE 0 END) AS TotalStockIn,
                       SUM(CASE WHEN st.QuantityChange < 0 THEN ABS(st.QuantityChange) ELSE 0 END) AS TotalStockOut,
                       SUM(st.QuantityChange) AS NetChange
                FROM StockTransactions st
                LEFT JOIN Medicines m ON st.ItemType = 'Medicine' AND st.ItemID = m.MedicineID
                LEFT JOIN Vaccines v ON st.ItemType = 'Vaccine' AND st.ItemID = v.VaccineID
                WHERE st.ItemType IN ('Medicine', 'Vaccine')
                """);
        List<Object> params = new ArrayList<>();
        appendDateFilters(sql, params, fromDate, toDate);
        sql.append("""
                 GROUP BY st.ItemType, st.ItemID, COALESCE(m.Name, v.Name)
                 ORDER BY st.ItemType, ItemName
                """);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<StockMovementReport> list = new ArrayList<>();
                while (rs.next()) list.add(mapMovementReport(rs));
                return list;
            }
        }
    }

    public void deductStock(Connection conn, int medicineID, int qty) throws SQLException {
        String sql = "UPDATE Medicines SET StockQty = StockQty - ? WHERE MedicineID = ? AND StockQty >= ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, qty);
            ps.setInt(2, medicineID);
            ps.setInt(3, qty);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new SQLException("Insufficient stock for MedicineID=" + medicineID);
            }
        }
    }

    public boolean isBelowThreshold(Connection conn, int medicineID) throws SQLException {
        String sql = "SELECT StockQty, MinStockLevel FROM Medicines WHERE MedicineID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, medicineID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("StockQty") < rs.getInt("MinStockLevel");
                }
            }
        }
        return false;
    }

    private int stockInMedicine(Connection conn, Integer itemID, String itemName,
                                String unit, BigDecimal unitPrice, int quantity,
                                int minStockLevel) throws SQLException {
        if (itemID != null && itemID > 0) {
            String sql = """
                    UPDATE Medicines
                    SET StockQty = StockQty + ?,
                        Unit = COALESCE(NULLIF(?, ''), Unit),
                        UnitPrice = COALESCE(?, UnitPrice),
                        MinStockLevel = CASE WHEN ? > 0 THEN ? ELSE MinStockLevel END
                    WHERE MedicineID = ?
                    """;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, quantity);
                ps.setString(2, unit == null ? "" : unit.trim());
                if (unitPrice != null) ps.setBigDecimal(3, unitPrice);
                else ps.setNull(3, Types.DECIMAL);
                ps.setInt(4, minStockLevel);
                ps.setInt(5, minStockLevel);
                ps.setInt(6, itemID);
                if (ps.executeUpdate() == 0) {
                    throw new SQLException("Medicine not found: " + itemID);
                }
            }
            return itemID;
        }

        String sql = """
                INSERT INTO Medicines (Name, Unit, UnitPrice, StockQty, MinStockLevel)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, itemName);
            ps.setString(2, unit);
            ps.setBigDecimal(3, unitPrice);
            ps.setInt(4, quantity);
            ps.setInt(5, minStockLevel > 0 ? minStockLevel : 10);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Failed to retrieve generated MedicineID.");
    }

    private int stockInVaccine(Connection conn, Integer itemID, String itemName,
                               BigDecimal unitPrice, int quantity) throws SQLException {
        if (itemID != null && itemID > 0) {
            String sql = """
                    UPDATE Vaccines
                    SET StockQty = StockQty + ?,
                        UnitPrice = COALESCE(?, UnitPrice)
                    WHERE VaccineID = ?
                    """;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, quantity);
                if (unitPrice != null) ps.setBigDecimal(2, unitPrice);
                else ps.setNull(2, Types.DECIMAL);
                ps.setInt(3, itemID);
                if (ps.executeUpdate() == 0) {
                    throw new SQLException("Vaccine not found: " + itemID);
                }
            }
            return itemID;
        }

        String sql = "INSERT INTO Vaccines (Name, UnitPrice, StockQty) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, itemName);
            ps.setBigDecimal(2, unitPrice);
            ps.setInt(3, quantity);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Failed to retrieve generated VaccineID.");
    }

    private void insertStockTransaction(Connection conn, String itemType, int itemID,
                                        BigDecimal quantityChange, String reason,
                                        int performedByID) throws SQLException {
        String sql = """
                INSERT INTO StockTransactions (ItemType, ItemID, QuantityChange, Reason, PerformedByID)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, itemType);
            ps.setInt(2, itemID);
            ps.setBigDecimal(3, quantityChange);
            ps.setString(4, reason);
            ps.setInt(5, performedByID);
            ps.executeUpdate();
        }
    }

    private Medicine mapMedicine(ResultSet rs) throws SQLException {
        Medicine m = new Medicine();
        m.setMedicineID(rs.getInt("MedicineID"));
        m.setName(rs.getString("Name"));
        m.setUnit(rs.getString("Unit"));
        m.setUnitPrice(rs.getBigDecimal("UnitPrice"));
        m.setStockQty(rs.getInt("StockQty"));
        m.setMinStockLevel(rs.getInt("MinStockLevel"));
        return m;
    }

    private InventoryItem mapInventoryItem(ResultSet rs) throws SQLException {
        InventoryItem item = new InventoryItem();
        item.setItemType(rs.getString("ItemType"));
        item.setItemID(rs.getInt("ItemID"));
        item.setName(rs.getString("Name"));
        item.setUnit(rs.getString("Unit"));
        item.setUnitPrice(rs.getBigDecimal("UnitPrice"));
        item.setStockQty(rs.getInt("StockQty"));
        item.setMinStockLevel(rs.getInt("MinStockLevel"));
        return item;
    }

    private StockTransaction mapTransaction(ResultSet rs) throws SQLException {
        StockTransaction t = new StockTransaction();
        t.setTransactionID(rs.getInt("TransactionID"));
        t.setItemType(rs.getString("ItemType"));
        t.setItemID(rs.getInt("ItemID"));
        t.setItemName(rs.getString("ItemName"));
        t.setQuantityChange(rs.getBigDecimal("QuantityChange"));
        t.setReason(rs.getString("Reason"));
        int staffID = rs.getInt("PerformedByID");
        t.setPerformedByID(rs.wasNull() ? null : staffID);
        t.setPerformedByName(rs.getString("PerformedByName"));
        Timestamp transactionDate = rs.getTimestamp("TransactionDate");
        if (transactionDate != null) t.setTransactionDate(transactionDate.toLocalDateTime());
        return t;
    }

    private StockMovementReport mapMovementReport(ResultSet rs) throws SQLException {
        StockMovementReport r = new StockMovementReport();
        r.setItemType(rs.getString("ItemType"));
        r.setItemID(rs.getInt("ItemID"));
        r.setItemName(rs.getString("ItemName"));
        r.setTotalStockIn(rs.getBigDecimal("TotalStockIn"));
        r.setTotalStockOut(rs.getBigDecimal("TotalStockOut"));
        r.setNetChange(rs.getBigDecimal("NetChange"));
        return r;
    }

    private void appendDateFilters(StringBuilder sql, List<Object> params,
                                   LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null) {
            sql.append(" AND st.TransactionDate >= ?");
            params.add(Timestamp.valueOf(fromDate.atStartOfDay()));
        }
        if (toDate != null) {
            sql.append(" AND st.TransactionDate < ?");
            params.add(Timestamp.valueOf(toDate.plusDays(1).atStartOfDay()));
        }
    }

    private void bindParams(PreparedStatement ps, List<Object> params)
            throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object value = params.get(i);
            if (value instanceof Integer) ps.setInt(i + 1, (Integer) value);
            else if (value instanceof Timestamp) ps.setTimestamp(i + 1, (Timestamp) value);
            else ps.setObject(i + 1, value);
        }
    }
}
