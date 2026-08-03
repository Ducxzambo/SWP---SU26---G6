package com.petclinic.backend.dao;

import com.petclinic.backend.dao.MedicineDAO;
import com.petclinic.backend.model.MedicalRecord;
import com.petclinic.backend.model.PrescriptionItem;
import com.petclinic.backend.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MedicalRecordDAO {

    private MedicineDAO medicineDAO = new MedicineDAO();

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
        mr.setGeneralConclusion(rs.getString("GeneralConclusion"));
        Timestamp ts = rs.getTimestamp("CreatedAt");
        if (ts != null) mr.setCreatedAt(ts.toLocalDateTime());
        mr.setStaffName(rs.getString("StaffName"));
        return mr;
    }

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

    // helpers
    private int insertRecord(Connection conn, MedicalRecord r) throws SQLException {
        String sql = """
                INSERT INTO MedicalRecords
                    (AppointmentID, PetID, StaffID, Weight, Temperature, Symptoms, Diagnosis, TreatmentPlan, GeneralConclusion)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
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
            ps.setString(9, r.getGeneralConclusion());
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

    private void insertStockTransaction(Connection conn, PrescriptionItem item,
                                        int performedByVetID) throws SQLException {
        String sql = "INSERT INTO StockTransactions " +
                "(ItemType, ItemID, QuantityChange, Reason, PerformedByID, " +
                "TransactionDate, ProviderID, TransactionType, PurchasePrice) " +
                "VALUES ('Medicine', ?, ?, 'Used', ?, GETDATE(), NULL, 'Export', NULL)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, item.getMedicineID());
            ps.setBigDecimal(2, item.getQuantity().negate()); // âm = xuất kho
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
                    p.setUnit(rs.getString("MedicineUnit"));
                    list.add(p);
                }
                return list;
            }
        }
    }

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