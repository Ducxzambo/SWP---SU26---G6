package com.petclinic.dao;

import com.petclinic.model.Pet;
import com.petclinic.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PetDAO {

    public List<Pet> findByCustomer(int customerId) throws SQLException {
        String sql = "SELECT * FROM Pets WHERE CustomerID = ? AND IsDeleted = 0 ORDER BY Name";
        List<Pet> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public Pet findById(int petId) throws SQLException {
        String sql = "SELECT * FROM Pets WHERE PetID = ? AND IsDeleted = 0";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, petId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    private Pet mapRow(ResultSet rs) throws SQLException {
        Pet p = new Pet();
        p.setPetID(rs.getInt("PetID"));
        p.setCustomerID(rs.getInt("CustomerID"));
        p.setName(rs.getString("Name"));
        p.setSpeciesName(rs.getString("SpeciesName"));
        p.setBreedName(rs.getString("BreedName"));
        p.setGender(rs.getString("Gender"));
        Date dob = rs.getDate("DateOfBirth");
        if (dob != null) p.setDateOfBirth(dob.toLocalDate());
        p.setWeight(rs.getBigDecimal("Weight"));
        p.setDeleted(rs.getBoolean("IsDeleted"));
        return p;
    }
}
