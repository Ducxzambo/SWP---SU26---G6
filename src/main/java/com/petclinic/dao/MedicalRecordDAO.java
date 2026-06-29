package com.petclinic.dao;

import com.petclinic.model.MedicalRecord;
import com.petclinic.model.PrescriptionItem;
import com.petclinic.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for MedicalRecords and PrescriptionItems.
 * Save is transactional: MedicalRecord + PrescriptionItems + stock deduction in one TX.
 */
public class MedicalRecordDAO {

    private final MedicineDAO medicineDAO = new MedicineDAO();

    // ── Read ──────────────────────────────────────────────────────────────────

    /**
     * Find by appointmentID (1-1 relationship).
     */
    public MedicalRecord findByAppointmentId(int appointmentID) throws SQLException {
        String sql = """
                SELECT mr.*,
                       p.Name      AS PetName,
                       c.FullName  AS OwnerName,
                       st.FullName AS VetName
                FROM MedicalRecords mr
                JOIN Pets     p  ON p.PetID       = mr.PetID
                JOIN Customers c  ON c.CustomerID  = (SELECT CustomerID FROM Pets WHERE PetID = mr.PetID)
                JOIN Staff    st ON st.StaffID     = mr.StaffID
                WHERE mr.AppointmentID = ?
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, appointmentID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    MedicalRecord rec = mapRecord(rs);
                    rec.setPrescriptionItems(findPrescriptionItems(conn, rec.getRecordID()));
                    return rec;
                }
                return null;
            }
        }
    }

    public MedicalRecord findById(int recordID) throws SQLException {
        String sql = """
                SELECT mr.*,
                       p.Name      AS PetName,
                       c.FullName  AS OwnerName,
                       st.FullName AS VetName
                FROM MedicalRecords mr
                JOIN Pets     p  ON p.PetID       = mr.PetID
                JOIN Customers c  ON c.CustomerID  = (SELECT CustomerID FROM Pets WHERE PetID = mr.PetID)
                JOIN Staff    st ON st.StaffID     = mr.StaffID
                WHERE mr.RecordID = ?
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, recordID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    MedicalRecord rec = mapRecord(rs);
                    rec.setPrescriptionItems(findPrescriptionItems(conn, rec.getRecordID()));
                    return rec;
                }
                return null;
            }
        }
    }

    /**
     * Full history for a pet (for vet to review before examination).
     */
    public List<MedicalRecord> findHistoryByPetId(int petID) throws SQLException {
        String sql = """
                SELECT mr.*,
                       p.Name      AS PetName,
                       c.FullName  AS OwnerName,
                       st.FullName AS VetName
                FROM MedicalRecords mr
                JOIN Pets     p  ON p.PetID       = mr.PetID
                JOIN Customers c ON c.CustomerID  = (SELECT CustomerID FROM Pets WHERE PetID = mr.PetID)
                JOIN Staff    st ON st.StaffID     = mr.StaffID
                WHERE mr.PetID = ?
                ORDER BY mr.CreatedAt DESC
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, petID);
            try (ResultSet rs = ps.executeQuery()) {
                List<MedicalRecord> list = new ArrayList<>();
                while (rs.next()) {
                    MedicalRecord rec = mapRecord(rs);
                    rec.setPrescriptionItems(findPrescriptionItems(conn, rec.getRecordID()));
                    list.add(rec);
                }
                return list;
            }
        }
    }

    // ── Write (transactional) ─────────────────────────────────────────────────

    /**
     * Save a new medical record with its prescription items in one transaction.
     * Also deducts medicine stock and records StockTransactions.
     * Returns the generated RecordID.
     *
     * @throws SQLException if stock is insufficient for any prescribed medicine.
     */
    public int save(MedicalRecord record, List<PrescriptionItem> items) throws SQLException {
        Connection conn = DBConnection.getConnection();
        try {
            conn.setAutoCommit(false);

            // 1. Insert MedicalRecord
            int recordID = insertRecord(conn, record);
            record.setRecordID(recordID);

            // 2. Insert PrescriptionItems + deduct stock + log StockTransaction
            if (items != null) {
                for (PrescriptionItem item : items) {
                    item.setRecordID(recordID);
                    insertPrescriptionItem(conn, item);
                    int qty = item.getQuantity().intValue();
                    medicineDAO.deductStock(conn, item.getMedicineID(), qty);
                    insertStockTransaction(conn, item, record.getVetID());
                }
            }

            conn.commit();
            return recordID;

        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
            conn.close();
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private int insertRecord(Connection conn, MedicalRecord r) throws SQLException {
        String sql = """
                INSERT INTO MedicalRecords
                    (AppointmentID, PetID, StaffID, Weight, Temperature, Symptoms, Diagnosis, TreatmentPlan)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getAppointmentID());
            ps.setInt(2, r.getPetID());
            ps.setInt(3, r.getVetID());
            if (r.getWeight() != null) ps.setBigDecimal(4, r.getWeight());
            else ps.setNull(4, Types.DECIMAL);
            if (r.getTemperature() != null) ps.setBigDecimal(5, r.getTemperature());
            else ps.setNull(5, Types.DECIMAL);
            ps.setString(6, r.getSymptoms());
            ps.setString(7, r.getDiagnosis());
            ps.setString(8, r.getTreatmentPlan());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Failed to retrieve generated RecordID.");
    }

    private void insertPrescriptionItem(Connection conn, PrescriptionItem item) throws SQLException {
        String sql = """
                INSERT INTO PrescriptionItems (RecordID, MedicineID, Dosage, Quantity, UnitPrice)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, item.getRecordID());
            ps.setInt(2, item.getMedicineID());
            ps.setString(3, item.getDosage());
            ps.setBigDecimal(4, item.getQuantity());
            ps.setBigDecimal(5, item.getUnitPrice());
            ps.executeUpdate();
        }
    }

    /**
     * Append a stock-out transaction for audit trail (mirrors StockTransactions table).
     */
    private void insertStockTransaction(Connection conn, PrescriptionItem item,
                                        int performedByVetID) throws SQLException {
        String sql = """
                INSERT INTO StockTransactions (ItemType, ItemID, QuantityChange, Reason, PerformedByID)
                VALUES ('Medicine', ?, ?, 'Used', ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, item.getMedicineID());
            ps.setBigDecimal(2, item.getQuantity().negate()); // negative = stock out
            ps.setInt(3, performedByVetID);
            ps.executeUpdate();
        }
    }

    private List<PrescriptionItem> findPrescriptionItems(Connection conn, int recordID)
            throws SQLException {
        String sql = """
                SELECT pi.*, m.Name AS MedicineName, m.Unit AS MedicineUnit
                FROM PrescriptionItems pi
                JOIN Medicines m ON m.MedicineID = pi.MedicineID
                WHERE pi.RecordID = ?
                ORDER BY pi.ItemID
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, recordID);
            try (ResultSet rs = ps.executeQuery()) {
                List<PrescriptionItem> list = new ArrayList<>();
                while (rs.next()) {
                    PrescriptionItem p = new PrescriptionItem();
                    p.setItemID(rs.getInt("ItemID"));
                    p.setRecordID(rs.getInt("RecordID"));
                    p.setMedicineID(rs.getInt("MedicineID"));
                    p.setDosage(rs.getString("Dosage"));
                    p.setQuantity(rs.getBigDecimal("Quantity"));
                    p.setUnitPrice(rs.getBigDecimal("UnitPrice"));
                    p.setMedicineName(rs.getString("MedicineName"));
                    p.setMedicineUnit(rs.getString("MedicineUnit"));
                    list.add(p);
                }
                return list;
            }
        }
    }

    private MedicalRecord mapRecord(ResultSet rs) throws SQLException {
        MedicalRecord r = new MedicalRecord();
        r.setRecordID(rs.getInt("RecordID"));
        r.setAppointmentID(rs.getInt("AppointmentID"));
        r.setPetID(rs.getInt("PetID"));
        r.setVetID(rs.getInt("StaffID"));
        r.setWeight(rs.getBigDecimal("Weight"));
        r.setTemperature(rs.getBigDecimal("Temperature"));
        r.setSymptoms(rs.getString("Symptoms"));
        r.setDiagnosis(rs.getString("Diagnosis"));
        r.setTreatmentPlan(rs.getString("TreatmentPlan"));
        Timestamp ts = rs.getTimestamp("CreatedAt");
        if (ts != null) r.setCreatedAt(ts.toLocalDateTime());
        try {
            r.setPetName(rs.getString("PetName"));
        } catch (SQLException ignored) {
        }
        try {
            r.setOwnerName(rs.getString("OwnerName"));
        } catch (SQLException ignored) {
        }
        try {
            r.setVetName(rs.getString("VetName"));
        } catch (SQLException ignored) {
        }
        return r;
    }
}