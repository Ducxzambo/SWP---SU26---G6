package com.petclinic.backend.dao;

import com.petclinic.backend.model.Vaccine;
import com.petclinic.backend.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VaccineDAO {

    public List<Vaccine> findAvailable() throws SQLException {
        String sql = "SELECT * FROM Vaccines WHERE StockQty >= MinStockLevel ORDER BY Name";
        List<Vaccine> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public Vaccine findById(int id) throws SQLException {
        String sql = "SELECT * FROM Vaccines WHERE VaccineID = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }


    public Vaccine findByName(Connection c, String name) throws SQLException {
        String sql = "SELECT * FROM Vaccines WHERE Name = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public void deductStock(Connection c, int vaccineId, int qty) throws SQLException {
        String sql = "UPDATE Vaccines SET StockQty = StockQty - ? WHERE VaccineID = ? AND StockQty >= ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, qty);
            ps.setInt(2, vaccineId);
            ps.setInt(3, qty);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new SQLException("Insufficient stock for VaccineID=" + vaccineId);
            }
        }
    }

    private Vaccine mapRow(ResultSet rs) throws SQLException {
        Vaccine v = new Vaccine();
        v.setVaccineID(rs.getInt("VaccineID"));
        v.setName(rs.getString("Name"));
        v.setUnitPrice(rs.getBigDecimal("UnitPrice"));
        v.setStockQty(rs.getInt("StockQty"));
        v.setMinStockLevel(rs.getInt("MinStockLevel"));
        return v;
    }
}