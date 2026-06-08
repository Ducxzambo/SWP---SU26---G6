package com.petclinic.dao;

import com.petclinic.model.Medicine;
import com.petclinic.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for the Medicines table.
 */
public class MedicineDAO {

    /**
     * All active medicines with stock > 0 (for prescription drop-down).
     */
    public List<Medicine> findAllInStock() throws SQLException {
        String sql = "SELECT * FROM Medicines WHERE StockQty > 0 ORDER BY Name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Medicine> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;
        }
    }

    /**
     * All medicines regardless of stock (admin view).
     */
    public List<Medicine> findAll() throws SQLException {
        String sql = "SELECT * FROM Medicines ORDER BY Name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Medicine> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;
        }
    }

    public Medicine findById(int medicineID) throws SQLException {
        String sql = "SELECT * FROM Medicines WHERE MedicineID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, medicineID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /**
     * Deduct quantity from stock after prescription is saved.
     * Called inside the same logical transaction as MedicalRecord save.
     */
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

    /**
     * Returns true if stock for the given medicine is below threshold (default 10).
     */
    public boolean isBelowThreshold(Connection conn, int medicineID) throws SQLException {
        String sql = "SELECT StockQty, MinStockLevel FROM Medicines WHERE MedicineID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, medicineID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int stockQty = rs.getInt("StockQty");
                    int minStockLevel = rs.getInt("MinStockLevel");
                    return stockQty < minStockLevel;
                }
            }
        }
        return false;
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private Medicine mapRow(ResultSet rs) throws SQLException {
        Medicine m = new Medicine();
        m.setMedicineID(rs.getInt("MedicineID"));
        m.setName(rs.getString("Name"));
        m.setUnit(rs.getString("Unit"));
        m.setUnitPrice(rs.getBigDecimal("UnitPrice"));
        m.setStockQty(rs.getInt("StockQty"));
        return m;
    }
}