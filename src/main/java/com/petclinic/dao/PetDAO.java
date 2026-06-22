package com.petclinic.dao;

import com.petclinic.model.Pet;
import com.petclinic.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PetDAO {

    // ── Lookup ────────────────────────────────────────────────────────────────

    /** All active (non-deleted) pets belonging to a customer. */
    public List<Pet> findByCustomerId(int customerID) throws SQLException {
        String sql = "SELECT * FROM Pets WHERE CustomerID = ? AND IsDeleted = 0 ORDER BY Name";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerID);
            try (ResultSet rs = ps.executeQuery()) {
                List<Pet> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        }
    }

    public Pet findById(int petID) throws SQLException {
        String sql = "SELECT * FROM Pets WHERE PetID = ? AND IsDeleted = 0";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, petID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    /** Quick-create a pet with minimal info (used by walk-in flow). Returns generated PetID. */
    public int insert(Pet pet) throws SQLException {
        String sql = """
                INSERT INTO Pets (CustomerID, Name, SpeciesName, BreedName, Gender)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, pet.getCustomerID());
            ps.setString(2, pet.getName());
            ps.setString(3, pet.getSpeciesName() != null ? pet.getSpeciesName() : "Chưa rõ");
            ps.setString(4, pet.getBreedName() != null ? pet.getBreedName() : "Chưa rõ");
            ps.setString(5, pet.getGender());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

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