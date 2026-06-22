package com.petclinic.service;

import com.petclinic.dao.InpatientAdmissionDAO;
import com.petclinic.dao.InvoiceDAO;
import com.petclinic.dao.NotificationDAO;
import com.petclinic.model.DailyAssessment;
import com.petclinic.model.InpatientAdmission;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Business logic for BP-04: Inpatient.
 * Servlets call this class only — never touch DAO directly from servlet.
 */
public class InpatientService {

    private static final double DAILY_RATE = 200_000.0; // VND per day

    private final InpatientAdmissionDAO dao      = new InpatientAdmissionDAO();
    private final InvoiceDAO            invoiceDAO = new InvoiceDAO();
    private final NotificationDAO       notifDAO  = new NotificationDAO();

    // ── Receptionist ──────────────────────────────────────────────────────────

    /**
     * Admit a pet:
     *   1. Validate cage not already occupied
     *   2. Create InpatientAdmissions row
     * @return new admissionID
     */
    public int admitPet(int recordID, int petID, String cageNumber)
            throws SQLException {
        List<String> occupied = dao.findOccupiedCages();
        if (occupied.contains(cageNumber.trim())) {
            throw new IllegalStateException(
                "Cage " + cageNumber + " is already occupied.");
        }
        return dao.create(recordID, petID, cageNumber.trim());
    }

    /**
     * Discharge a pet:
     *   1. Set DischargeDate = today, Status = Discharged
     *   2. Calculate fee = DAILY_RATE × days stayed (min 1)
     *   3. Create Invoice (Unpaid)
     *   4. Notify customer
     * @return new invoiceID
     */
    public int dischargePet(int admissionID) throws SQLException {
        InpatientAdmission a = dao.findById(admissionID);
        if (a == null)
            throw new IllegalArgumentException("Admission not found: " + admissionID);

        // 1. mark discharged
        dao.discharge(admissionID);

        // 2. fee calculation
        LocalDate admitDate = a.getAdmitDate() != null
            ? a.getAdmitDate() : LocalDate.now();
        long days = Math.max(1, ChronoUnit.DAYS.between(admitDate, LocalDate.now()));
        double total = DAILY_RATE * days;

        // 3. create invoice
        int invoiceID = invoiceDAO.create(
            a.getAppointmentID(),
            a.getCustomerID(),
            total,
            "Unpaid"
        );

        // 4. notify owner
        notifDAO.createForCustomer(
            a.getCustomerID(),
            "Your pet " + a.getPetName() + " has been discharged",
            "Stay duration: " + days + " day(s). "
            + "Total fee: " + String.format("%,.0f", total) + " VND. "
            + "Please proceed to payment."
        );

        return invoiceID;
    }

    public List<InpatientAdmission> getActiveAdmissions() throws SQLException {
        return dao.findAllActive();
    }

    public List<InpatientAdmission> getAdmissionsForCustomer(int customerID)
            throws SQLException {
        return dao.findByCustomer(customerID);
    }

    public InpatientAdmission getAdmission(int admissionID) throws SQLException {
        return dao.findById(admissionID);
    }

    public List<String> getOccupiedCages() throws SQLException {
        return dao.findOccupiedCages();
    }

    // ── Veterinarian ──────────────────────────────────────────────────────────

    /**
     * Save daily assessment:
     *   1. Prevent duplicate (one per day per admission)
     *   2. Insert DailyAssessments row
     *   3. Notify customer of today's update
     */
    public void saveDailyAssessment(int admissionID, int vetID,
                                     String condition, String treatment)
            throws SQLException {
        if (dao.hasTodayAssessment(admissionID)) {
            throw new IllegalStateException(
                "Today's assessment has already been submitted for this admission.");
        }
        dao.createAssessment(admissionID, vetID,
            condition  != null ? condition.trim()  : "",
            treatment  != null ? treatment.trim()  : "");

        // notify owner
        InpatientAdmission a = dao.findById(admissionID);
        if (a != null) {
            notifDAO.createForCustomer(
                a.getCustomerID(),
                "Daily update for " + a.getPetName(),
                "Today's condition: " + condition
            );
        }
    }

    /**
     * Mark pet as Critical — triggers urgent notification to owner.
     */
    public void markCritical(int admissionID) throws SQLException {
        dao.updateStatus(admissionID, "Critical");
        InpatientAdmission a = dao.findById(admissionID);
        if (a != null) {
            notifDAO.createForCustomer(
                a.getCustomerID(),
                "⚠️ URGENT: " + a.getPetName() + " is in Critical Condition",
                "Please contact the clinic immediately."
            );
        }
    }

    public List<DailyAssessment> getAssessments(int admissionID)
            throws SQLException {
        return dao.findAssessmentsByAdmission(admissionID);
    }

    public boolean hasTodayAssessment(int admissionID) throws SQLException {
        return dao.hasTodayAssessment(admissionID);
    }
}
