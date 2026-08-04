package com.petclinic.backend.dao;

import com.petclinic.backend.model.MedicalRecord;
import com.petclinic.backend.model.PrescriptionItem;
import com.petclinic.backend.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MedicalRecordDAO {

    private MedicineDAO medicineDAO = new MedicineDAO();

    /**
     * Find by appointmentID (1-1 relationship).
     */
    public MedicalRecord findByAppointmentId(int appointmentID) throws SQLException {
        String sql = """
                SELECT mr.*,
                       p.Name      AS PetName,
                       c.FullName  AS OwnerName,
                       st.FullName AS StaffName
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

    /** Load medical record cho một appointment */
    public MedicalRecord findByAppointment(int appointmentId) throws SQLException {
        String sql = "SELECT mr.*, s.FullName AS StaffName "
                + "FROM MedicalRecords mr "
                + "JOIN Staff s ON mr.StaffID = s.StaffID "
                + "WHERE mr.AppointmentID = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, appointmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                MedicalRecord mr = mapRecord(rs);
                mr.setPrescriptions(findPrescriptions(mr.getRecordID()));
                return mr;
            }
        }
    }

    public List<MedicalRecord> findByPet(int petId) throws SQLException {
        String sql = "SELECT mr.*, s.FullName AS StaffName "
                + "FROM MedicalRecords mr "
                + "JOIN Staff s ON mr.StaffID = s.StaffID "
                + "WHERE mr.PetID = ? Order by mr.AppointmentID desc";
        List<MedicalRecord> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, petId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRecord(rs));
            }
        }
        return list;
    }

    public List<PrescriptionItem> findPrescriptions(int recordId) throws SQLException {
        String sql = "SELECT pi.*, m.Name AS MedicineName, m.Unit "
                + "FROM PrescriptionItems pi "
                + "JOIN Medicines m ON pi.MedicineID = m.MedicineID "
                + "WHERE pi.RecordID = ?";
        List<PrescriptionItem> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, recordId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PrescriptionItem item = new PrescriptionItem();
                    item.setItemID(rs.getInt("ItemID"));
                    item.setRecordID(rs.getInt("RecordID"));
                    item.setMedicineID(rs.getInt("MedicineID"));
                    item.setMedicineName(rs.getString("MedicineName"));
                    item.setUnit(rs.getString("Unit"));
                    item.setDosage(rs.getString("Dosage"));
                    item.setQuantity(rs.getBigDecimal("Quantity"));
                    item.setUnitPrice(rs.getBigDecimal("UnitPrice"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    /**
     * Cân nặng ở lần khám GẦN NHẤT có ghi nhận cân nặng cho 1 pet - dùng làm
     * fallback khi Pets.Weight null. Trả về null nếu pet chưa có medical record
     * nào ghi nhận cân nặng.
     */
    public java.math.BigDecimal findLatestWeightByPet(int petId) throws SQLException {
        String sql = "SELECT TOP 1 Weight FROM MedicalRecords "
                + "WHERE PetID = ? AND Weight IS NOT NULL "
                + "ORDER BY CreatedAt DESC";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, petId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : null;
            }
        }
    }

    private MedicalRecord mapRecord(ResultSet rs) throws SQLException {
        MedicalRecord mr = new MedicalRecord();
        mr.setRecordID(rs.getInt("RecordID"));
        mr.setAppointmentID(rs.getInt("AppointmentID"));
        mr.setPetID(rs.getInt("PetID"));
        mr.setStaffID(rs.getInt("StaffID"));
        mr.setWeight(rs.getBigDecimal("Weight"));
        mr.setTemperature(rs.getBigDecimal("Temperature"));
        mr.setSymptoms(rs.getString("Symptoms"));
        mr.setDiagnosis(rs.getString("Diagnosis"));
        mr.setTreatmentPlan(rs.getString("TreatmentPlan"));
        Timestamp ts = rs.getTimestamp("CreatedAt");
        if (ts != null) mr.setCreatedAt(ts.toLocalDateTime());
        mr.setStaffName(rs.getString("StaffName"));
        return mr;
    }

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
                    insertStockTransaction(conn, item, record.getStaffID());
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
            ps.setInt(3, r.getStaffID());
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
                                        int performedByStaffID) throws SQLException {
        String sql = """
                INSERT INTO StockTransactions
                    (ItemType, ItemID, QuantityChange, Reason, PerformedByID, TransactionType)
                VALUES ('Medicine', ?, ?, 'Used', ?, 'Export')
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, item.getMedicineID());
            ps.setBigDecimal(2, item.getQuantity().negate()); // negative = stock out
            ps.setInt(3, performedByStaffID);
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
                    p.setUnit(rs.getString("MedicineUnit"));
                    list.add(p);
                }
                return list;
            }
        }
    }
    /**
     * Full history for a pet (for Staff to review before examination).
     */
    public List<MedicalRecord> findHistoryByPetId(int petID) throws SQLException {
        String sql = """
                SELECT mr.*,
                       p.Name      AS PetName,
                       c.FullName  AS OwnerName,
                       st.FullName AS StaffName
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
    public MedicalRecord findById(int recordID) throws SQLException {
        String sql = """
                SELECT mr.*,
                       p.Name      AS PetName,
                       c.FullName  AS OwnerName,
                       st.FullName AS StaffName
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
}
