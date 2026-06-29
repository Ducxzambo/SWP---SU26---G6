package com.petclinic.dao;

import com.petclinic.model.DailyAssessment;
import com.petclinic.model.InpatientAdmission;
import com.petclinic.util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for dbo.InpatientAdmissions + dbo.DailyAssessments.
 *
 * All column names match PetClinicMVP.sql exactly:
 *   InpatientAdmissions: AdmissionID, RecordID, PetID,
 *                        AdmitDate, DischargeDate, CageNumber, Status
 *   DailyAssessments:    AssessmentID, AdmissionID, VetID,
 *                        AssessmentDate, Condition, TreatmentToday
 */
public class InpatientAdmissionDAO {

    // ══════════════════════════════════════════════════════════════════════════
    // InpatientAdmissions
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Insert a new admission record. Status defaults to 'Admitted'.
     * @return generated AdmissionID, or -1 on failure
     */
    public int create(int recordID, int petID, String cageNumber) throws SQLException {
        String sql =
                "INSERT INTO InpatientAdmissions (RecordID, PetID, AdmitDate, CageNumber, Status) " +
                        "VALUES (?, ?, CAST(GETDATE() AS date), ?, 'Admitted')";

        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, recordID);
            ps.setInt(2, petID);
            ps.setString(3, cageNumber);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    /**
     * Find one admission by PK, joined with Pet + Customer + MedicalRecord.
     */
    public InpatientAdmission findById(int admissionID) throws SQLException {
        String sql =
                "SELECT ia.AdmissionID, ia.RecordID, ia.PetID, ia.AdmitDate, " +
                        "       ia.DischargeDate, ia.CageNumber, ia.Status, " +
                        "       p.Name          AS PetName, " +
                        "       c.FullName      AS OwnerName, " +
                        "       c.Email         AS OwnerEmail, " +
                        "       c.CustomerID, " +
                        "       mr.AppointmentID " +
                        "FROM   InpatientAdmissions ia " +
                        "JOIN   Pets           p  ON p.PetID        = ia.PetID " +
                        "JOIN   Customers      c  ON c.CustomerID   = p.CustomerID " +
                        "JOIN   MedicalRecords mr ON mr.RecordID    = ia.RecordID " +
                        "WHERE  ia.AdmissionID = ?";

        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, admissionID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapAdmission(rs) : null;
            }
        }
    }

    /**
     * All active admissions (Status IN 'Admitted','Critical').
     * Used by: Receptionist and Vet dashboards.
     */
    public List<InpatientAdmission> findAllActive() throws SQLException {
        String sql =
                "SELECT ia.AdmissionID, ia.RecordID, ia.PetID, ia.AdmitDate, " +
                        "       ia.DischargeDate, ia.CageNumber, ia.Status, " +
                        "       p.Name          AS PetName, " +
                        "       c.FullName      AS OwnerName, " +
                        "       c.Email         AS OwnerEmail, " +
                        "       c.CustomerID, " +
                        "       mr.AppointmentID " +
                        "FROM   InpatientAdmissions ia " +
                        "JOIN   Pets           p  ON p.PetID        = ia.PetID " +
                        "JOIN   Customers      c  ON c.CustomerID   = p.CustomerID " +
                        "JOIN   MedicalRecords mr ON mr.RecordID    = ia.RecordID " +
                        "WHERE  ia.Status IN ('Admitted', 'Critical') " +
                        "ORDER  BY ia.AdmitDate ASC";

        return query(sql);
    }

    /**
     * Admissions for a specific customer (own pets only).
     * Used by: Customer portal.
     */
    public List<InpatientAdmission> findByCustomer(int customerID) throws SQLException {
        String sql =
                "SELECT ia.AdmissionID, ia.RecordID, ia.PetID, ia.AdmitDate, " +
                        "       ia.DischargeDate, ia.CageNumber, ia.Status, " +
                        "       p.Name          AS PetName, " +
                        "       c.FullName      AS OwnerName, " +
                        "       c.Email         AS OwnerEmail, " +
                        "       c.CustomerID, " +
                        "       mr.AppointmentID " +
                        "FROM   InpatientAdmissions ia " +
                        "JOIN   Pets           p  ON p.PetID        = ia.PetID " +
                        "JOIN   Customers      c  ON c.CustomerID   = p.CustomerID " +
                        "JOIN   MedicalRecords mr ON mr.RecordID    = ia.RecordID " +
                        "WHERE  c.CustomerID = ? " +
                        "ORDER  BY ia.AdmitDate DESC";

        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, customerID);
            try (ResultSet rs = ps.executeQuery()) {
                List<InpatientAdmission> list = new ArrayList<>();
                while (rs.next()) list.add(mapAdmission(rs));
                return list;
            }
        }
    }

    /**
     * List currently occupied cage numbers.
     * Used by: Admit form to show which cages are taken.
     */
    public List<String> findOccupiedCages() throws SQLException {
        String sql =
                "SELECT CageNumber FROM InpatientAdmissions " +
                        "WHERE  Status IN ('Admitted', 'Critical') " +
                        "AND    CageNumber IS NOT NULL";

        List<String> list = new ArrayList<>();
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(rs.getString("CageNumber"));
        }
        return list;
    }

    /**
     * Update status only. Valid: 'Admitted' | 'Critical' | 'Discharged'
     */
    public void updateStatus(int admissionID, String status) throws SQLException {
        String sql = "UPDATE InpatientAdmissions SET Status = ? WHERE AdmissionID = ?";
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, admissionID);
            ps.executeUpdate();
        }
    }

    /**
     * Set DischargeDate = today and Status = 'Discharged'.
     * Called by DischargeServlet.
     */
    public void discharge(int admissionID) throws SQLException {
        String sql =
                "UPDATE InpatientAdmissions " +
                        "SET    DischargeDate = CAST(GETDATE() AS date), Status = 'Discharged' " +
                        "WHERE  AdmissionID = ?";
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, admissionID);
            ps.executeUpdate();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DailyAssessments
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Insert today's assessment.
     * Column: StaffID (updated schema — was VetID before)
     * @return generated AssessmentID
     */
    public int createAssessment(int admissionID, int staffID,
                                String condition, String treatmentToday) throws SQLException {
        String sql =
                "INSERT INTO DailyAssessments " +
                        "  (AdmissionID, StaffID, AssessmentDate, Condition, TreatmentToday) " +
                        "VALUES (?, ?, CAST(GETDATE() AS date), ?, ?)";

        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, admissionID);
            ps.setInt(2, staffID);
            ps.setString(3, condition);
            ps.setString(4, treatmentToday);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    /**
     * All assessments for one admission, newest first.
     * JOIN Staff on StaffID (updated schema).
     */
    public List<DailyAssessment> findAssessmentsByAdmission(int admissionID) throws SQLException {
        String sql =
                "SELECT da.AssessmentID, da.AdmissionID, da.StaffID, " +
                        "       da.AssessmentDate, da.Condition, da.TreatmentToday, " +
                        "       s.FullName AS VetName " +
                        "FROM   DailyAssessments da " +
                        "JOIN   Staff s ON s.StaffID = da.StaffID " +
                        "WHERE  da.AdmissionID = ? " +
                        "ORDER  BY da.AssessmentDate DESC";

        List<DailyAssessment> list = new ArrayList<>();
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, admissionID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapAssessment(rs));
            }
        }
        return list;
    }

    /**
     * Returns true if an assessment for today already exists for this admission.
     * Prevents duplicate daily entries.
     */
    public boolean hasTodayAssessment(int admissionID) throws SQLException {
        String sql =
                "SELECT 1 FROM DailyAssessments " +
                        "WHERE  AdmissionID   = ? " +
                        "AND    AssessmentDate = CAST(GETDATE() AS date)";
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, admissionID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════════════════

    private List<InpatientAdmission> query(String sql) throws SQLException {
        List<InpatientAdmission> list = new ArrayList<>();
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapAdmission(rs));
        }
        return list;
    }

    private InpatientAdmission mapAdmission(ResultSet rs) throws SQLException {
        InpatientAdmission a = new InpatientAdmission();
        a.setAdmissionID(rs.getInt("AdmissionID"));
        a.setRecordID(rs.getInt("RecordID"));
        a.setPetID(rs.getInt("PetID"));
        a.setStatus(rs.getString("Status"));
        a.setCageNumber(rs.getString("CageNumber"));

        Date admit = rs.getDate("AdmitDate");
        if (admit != null) a.setAdmitDate(admit.toLocalDate());

        Date discharge = rs.getDate("DischargeDate");
        if (discharge != null) a.setDischargeDate(discharge.toLocalDate());

        // joined
        a.setPetName(rs.getString("PetName"));
        a.setOwnerName(rs.getString("OwnerName"));
        a.setOwnerEmail(rs.getString("OwnerEmail"));
        a.setCustomerID(rs.getInt("CustomerID"));
        a.setAppointmentID(rs.getInt("AppointmentID"));
        return a;
    }

    private DailyAssessment mapAssessment(ResultSet rs) throws SQLException {
        DailyAssessment d = new DailyAssessment();
        d.setAssessmentID(rs.getInt("AssessmentID"));
        d.setAdmissionID(rs.getInt("AdmissionID"));
        d.setVetID(rs.getInt("StaffID"));   // column renamed StaffID in new schema
        d.setCondition(rs.getString("Condition"));
        d.setTreatmentToday(rs.getString("TreatmentToday"));
        d.setVetName(rs.getString("VetName"));

        Date date = rs.getDate("AssessmentDate");
        if (date != null) d.setAssessmentDate(date.toLocalDate());
        return d;
    }
}