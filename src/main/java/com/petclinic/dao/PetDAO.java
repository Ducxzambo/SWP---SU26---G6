package com.petclinic.dao;

import com.petclinic.model.Pet;
import com.petclinic.util.DBConnection;

import java.math.BigDecimal;
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

    public List<Pet> findByCustomer(int customerId) throws SQLException {
        String sql =
                "SELECT p.*," +
                        " COUNT(a.AppointmentID)                                   AS TotalAppts," +
                        " SUM(CASE WHEN a.Status = 'Done' THEN 1 ELSE 0 END)      AS DoneAppts," +
                        " MAX(CASE WHEN a.Status = 'Done'" +
                        " THEN CONVERT(VARCHAR(10), a.AppointmentDate, 120)" +
                        " ELSE NULL END)                                   AS LastVisit" +
                        " FROM Pets p" +
                        " LEFT JOIN Appointments a ON a.PetID = p.PetID" +
                        " WHERE p.CustomerID = ? AND p.IsDeleted = 0" +
                        " GROUP BY p.PetID, p.CustomerID, p.Name, p.SpeciesName, p.BreedName," +
                        " p.Gender, p.DateOfBirth, p.Weight, p.IsDeleted" +
                        " ORDER BY p.Name"
                ;
        List<Pet> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRowWithStats(rs));
            }
        }
        return list;
    }

    private Pet mapRowWithStats(ResultSet rs) throws SQLException {
        Pet p = mapRow(rs);
        p.setTotalAppointments(rs.getInt("TotalAppts"));
        p.setDoneAppointments(rs.getInt("DoneAppts"));
        p.setLastVisitDate(rs.getString("LastVisit")); // yyyy-MM-dd or null
        return p;
    }

    public void update(Pet pet) throws SQLException {
        String sql = "UPDATE Pets SET Name=?, SpeciesName=?, BreedName=?, Gender=?, "
                + "DateOfBirth=?, Weight=? WHERE PetID=? AND CustomerID=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, pet.getName());
            ps.setString(2, pet.getSpeciesName());
            ps.setString(3, pet.getBreedName());
            ps.setString(4, pet.getGender());
            ps.setDate(5, pet.getDateOfBirth() != null ? Date.valueOf(pet.getDateOfBirth()) : null);
            setBigDecimalOrNull(ps, 6, pet.getWeight());
            ps.setInt(7, pet.getPetID());
            ps.setInt(8, pet.getCustomerID());
            ps.executeUpdate();
        }
    }

    /** Soft delete — sets IsDeleted = 1 */
    public void softDelete(int petId, int customerId) throws SQLException {
        String sql = "UPDATE Pets SET IsDeleted = 1 WHERE PetID = ? AND CustomerID = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, petId);
            ps.setInt(2, customerId);
            ps.executeUpdate();
        }
    }

    private void setBigDecimalOrNull(PreparedStatement ps, int idx, BigDecimal val)
            throws SQLException {
        if (val != null) ps.setBigDecimal(idx, val);
        else             ps.setNull(idx, Types.DECIMAL);
    }
}