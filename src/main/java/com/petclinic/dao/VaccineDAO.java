package com.petclinic.dao;

import com.petclinic.model.Vaccine;
import com.petclinic.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VaccineDAO {

    /** Chỉ trả Vaccine còn đủ tồn kho (StockQty >= minStockLevel) — theo yêu cầu ẩn loại sắp hết hàng. */
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