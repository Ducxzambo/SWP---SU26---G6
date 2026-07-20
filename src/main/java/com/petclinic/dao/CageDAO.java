package com.petclinic.dao;

import com.petclinic.model.Cage;
import com.petclinic.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for dbo.Cages.
 *
 * Status is computed dynamically via LEFT JOIN InpatientAdmissions —
 * never stored as a column in Cages table.
 */
public class CageDAO {

    // ── Base SELECT with computed Status ──────────────────────────────────────
    private static final String BASE_SELECT =
            "SELECT c.CageID, c.CageNumber, c.CageType, c.IsActive, c.Notes, " +
                    "       CASE " +
                    "           WHEN c.IsActive = 0           THEN 'Maintenance' " +
                    "           WHEN ia.AdmissionID IS NOT NULL THEN 'Occupied' " +
                    "           ELSE                                'Available' " +
                    "       END AS Status, " +
                    "       p.Name     AS CurrentPetName, " +
                    "       CONVERT(NVARCHAR(10), ia.AdmitDate, 120) AS AdmitDate " +
                    "FROM   Cages c " +
                    "LEFT JOIN InpatientAdmissions ia " +
                    "       ON ia.CageID = c.CageID " +
                    "      AND ia.Status IN ('Admitted', 'Critical') " +
                    "LEFT JOIN Pets p ON p.PetID = ia.PetID ";

    // ── Read ──────────────────────────────────────────────────────────────────

    /** All cages with computed status — used by Receptionist cage management page. */
    public List<Cage> findAll() throws SQLException {
        String sql = BASE_SELECT + "ORDER BY c.CageNumber";
        return query(sql);
    }

    /**
     * Only cages that are available (IsActive=1 and no active admission).
     * Used by: admit form dropdown.
     */
    public List<Cage> findAvailable() throws SQLException {
        String sql = BASE_SELECT +
                "WHERE c.IsActive = 1 AND ia.AdmissionID IS NULL " +
                "ORDER BY c.CageNumber";
        return query(sql);
    }

    /** Find single cage by PK — used for validation before admit. */
    public Cage findById(int cageID) throws SQLException {
        String sql = BASE_SELECT + "WHERE c.CageID = ?";
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, cageID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    /**
     * Insert a new cage.
     * @return generated CageID
     */
    public int create(String cageNumber, String cageType,
                      String notes) throws SQLException {
        String sql =
                "INSERT INTO Cages (CageNumber, CageType, IsActive, Notes) " +
                        "VALUES (?, ?, 1, ?)";
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, cageNumber.trim().toUpperCase());
            ps.setString(2, cageType);
            ps.setString(3, notes);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    /**
     * Toggle IsActive (maintenance mode).
     * IsActive = 0 → cage shows as Maintenance, excluded from admit dropdown.
     */
    public void updateActive(int cageID, boolean isActive) throws SQLException {
        String sql = "UPDATE Cages SET IsActive = ? WHERE CageID = ?";
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setBoolean(1, isActive);
            ps.setInt(2, cageID);
            ps.executeUpdate();
        }
    }

    /**
     * Check if a CageNumber already exists (for duplicate validation).
     */
    public boolean existsByCageNumber(String cageNumber) throws SQLException {
        String sql = "SELECT 1 FROM Cages WHERE CageNumber = ?";
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, cageNumber.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<Cage> query(String sql) throws SQLException {
        List<Cage> list = new ArrayList<>();
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    private Cage mapRow(ResultSet rs) throws SQLException {
        Cage c = new Cage();
        c.setCageID(rs.getInt("CageID"));
        c.setCageNumber(rs.getString("CageNumber"));
        c.setCageType(rs.getString("CageType"));
        c.setActive(rs.getBoolean("IsActive"));
        c.setNotes(rs.getString("Notes"));
        c.setStatus(rs.getString("Status"));
        c.setCurrentPetName(rs.getString("CurrentPetName"));
        c.setAdmitDate(rs.getString("AdmitDate"));
        return c;
    }
}