package com.petclinic.dao;

import com.petclinic.model.DailyAssessment;
import com.petclinic.model.InpatientAdmission;
import com.petclinic.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for dbo.InpatientAdmissions + dbo.DailyAssessments.
 * Updated: CageNumber (text) → CageID (FK → Cages)
 */
public class InpatientAdmissionDAO {

    // ── InpatientAdmissions ───────────────────────────────────────────────────

    /**
     * Insert new admission using CageID (FK).
     * @return generated AdmissionID
     */
    public int create(int recordID, int petID, int cageID) throws SQLException {
        String sql =
                "INSERT INTO InpatientAdmissions " +
                        "  (RecordID, PetID, CageID, AdmitDate, Status) " +
                        "VALUES (?, ?, ?, CAST(GETDATE() AS date), 'Admitted')";

        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, recordID);
            ps.setInt(2, petID);
            ps.setInt(3, cageID);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    /** Find one admission by PK — joins Cages, Pets, Customers, MedicalRecords. */
    public InpatientAdmission findById(int admissionID) throws SQLException {
        String sql = buildSelect() + "WHERE ia.AdmissionID = ?";
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, admissionID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapAdmission(rs) : null;
            }
        }
    }

    /** All active admissions (Admitted or Critical). */
    public List<InpatientAdmission> findAllActive() throws SQLException {
        String sql = buildSelect() +
                "WHERE ia.Status IN ('Admitted','Critical') " +
                "ORDER BY ia.AdmitDate ASC";
        return query(sql);
    }

    /** Admissions belonging to a specific customer. */
    public List<InpatientAdmission> findByCustomer(int customerID) throws SQLException {
        String sql = buildSelect() +
                "WHERE c.CustomerID = ? " +
                "ORDER BY ia.AdmitDate DESC";
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

    /** Update admission status. Values: Admitted | Critical | Discharged */
    public void updateStatus(int admissionID, String status) throws SQLException {
        String sql = "UPDATE InpatientAdmissions SET Status=? WHERE AdmissionID=?";
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, admissionID);
            ps.executeUpdate();
        }
    }

    /** Discharge: set DischargeDate=today + Status=Discharged. */
    public void discharge(int admissionID) throws SQLException {
        String sql =
                "UPDATE InpatientAdmissions " +
                        "SET DischargeDate = CAST(GETDATE() AS date), Status = 'Discharged' " +
                        "WHERE AdmissionID = ?";
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, admissionID);
            ps.executeUpdate();
        }
    }

    // ── DailyAssessments ──────────────────────────────────────────────────────

    /** Insert today's daily assessment. */
    public int createAssessment(int admissionID, int staffID,
                                String condition,
                                String treatmentToday) throws SQLException {
        String sql =
                "INSERT INTO DailyAssessments " +
                        "  (AdmissionID, StaffID, AssessmentDate, Condition, TreatmentToday) " +
                        "VALUES (?, ?, CAST(GETDATE() AS date), ?, ?)";

        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {
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

    /** All assessments for one admission, newest first. */
    public List<DailyAssessment> findAssessmentsByAdmission(
            int admissionID) throws SQLException {
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

    /** True if today's assessment already exists for this admission. */
    public boolean hasTodayAssessment(int admissionID) throws SQLException {
        String sql =
                "SELECT 1 FROM DailyAssessments " +
                        "WHERE AdmissionID=? AND AssessmentDate=CAST(GETDATE() AS date)";
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, admissionID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Base SELECT with all necessary JOINs including Cages. */
    private String buildSelect() {
        return
                "SELECT ia.AdmissionID, ia.RecordID, ia.PetID, ia.CageID, " +
                        "       ia.AdmitDate, ia.DischargeDate, ia.Status, " +
                        "       cg.CageNumber, cg.CageType, " +
                        "       p.Name          AS PetName, " +
                        "       cu.FullName     AS OwnerName, " +
                        "       cu.Email        AS OwnerEmail, " +
                        "       cu.CustomerID, " +
                        "       mr.AppointmentID " +
                        "FROM   InpatientAdmissions ia " +
                        "LEFT  JOIN Cages        cg ON cg.CageID      = ia.CageID " +
                        "JOIN        Pets         p  ON p.PetID        = ia.PetID " +
                        "JOIN        Customers    cu ON cu.CustomerID  = p.CustomerID " +
                        "JOIN        MedicalRecords mr ON mr.RecordID  = ia.RecordID ";
    }

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
        a.setCageID(rs.getInt("CageID"));
        a.setCageNumber(rs.getString("CageNumber"));
        a.setCageType(rs.getString("CageType"));
        a.setStatus(rs.getString("Status"));
        a.setPetName(rs.getString("PetName"));
        a.setOwnerName(rs.getString("OwnerName"));
        a.setOwnerEmail(rs.getString("OwnerEmail"));
        a.setCustomerID(rs.getInt("CustomerID"));
        a.setAppointmentID(rs.getInt("AppointmentID"));

        Date admit = rs.getDate("AdmitDate");
        if (admit != null) a.setAdmitDate(admit.toLocalDate());

        Date discharge = rs.getDate("DischargeDate");
        if (discharge != null) a.setDischargeDate(discharge.toLocalDate());

        return a;
    }

    private DailyAssessment mapAssessment(ResultSet rs) throws SQLException {
        DailyAssessment d = new DailyAssessment();
        d.setAssessmentID(rs.getInt("AssessmentID"));
        d.setAdmissionID(rs.getInt("AdmissionID"));
        d.setVetID(rs.getInt("StaffID"));
        d.setCondition(rs.getString("Condition"));
        d.setTreatmentToday(rs.getString("TreatmentToday"));
        d.setVetName(rs.getString("VetName"));

        Date date = rs.getDate("AssessmentDate");
        if (date != null) d.setAssessmentDate(date.toLocalDate());
        return d;
    }
}