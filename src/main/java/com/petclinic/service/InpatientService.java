package com.petclinic.service;

import com.petclinic.dao.CageDAO;
import com.petclinic.dao.InpatientAdmissionDAO;
import com.petclinic.dao.InvoiceDAO;
import com.petclinic.dao.NotificationDAO;
import com.petclinic.model.Cage;
import com.petclinic.model.DailyAssessment;
import com.petclinic.model.InpatientAdmission;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Business logic for BP-04: Inpatient.
 * Updated: admitPet() now uses cageID (int FK) instead of cageNumber (String).
 */
public class InpatientService {

    private static final double DAILY_RATE = 200_000.0; // VND per day

    private final InpatientAdmissionDAO dao       = new InpatientAdmissionDAO();
    private final CageDAO               cageDAO   = new CageDAO();
    private final InvoiceDAO            invoiceDAO = new InvoiceDAO();
    private final NotificationDAO       notifDAO   = new NotificationDAO();

    // ── Receptionist ──────────────────────────────────────────────────────────

    /**
     * Get all available cages for the admit form dropdown.
     * Only returns IsActive=1 cages with no active admission.
     */
    public List<Cage> getCagesForAdmit() throws SQLException {
        return cageDAO.findAvailable();
    }

    /**
     * Get all cages with status — for Receptionist cage management page.
     */
    public List<Cage> getAllCages() throws SQLException {
        return cageDAO.findAll();
    }

    /**
     * Admit a pet:
     *   1. Validate cageID exists and is available
     *   2. Create InpatientAdmissions row with CageID FK
     * @return new admissionID
     */
    public int admitPet(int recordID, int petID, int cageID)
            throws SQLException {
        // validate cage
        Cage cage = cageDAO.findById(cageID);
        if (cage == null) {
            throw new IllegalArgumentException(
                    "Cage not found: ID " + cageID);
        }
        if (!cage.isActive()) {
            throw new IllegalStateException(
                    "Cage " + cage.getCageNumber() + " is under maintenance.");
        }
        if ("Occupied".equals(cage.getStatus())) {
            throw new IllegalStateException(
                    "Cage " + cage.getCageNumber() + " is already occupied.");
        }
        return dao.create(recordID, petID, cageID);
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
            throw new IllegalArgumentException(
                    "Admission not found: " + admissionID);

        dao.discharge(admissionID);

        LocalDate admitDate = a.getAdmitDate() != null
                ? a.getAdmitDate() : LocalDate.now();
        long days   = Math.max(1, ChronoUnit.DAYS.between(admitDate, LocalDate.now()));
        double total = DAILY_RATE * days;

        int invoiceID = invoiceDAO.create(
                a.getAppointmentID(), a.getCustomerID(), total, "Unpaid");

        notifDAO.createForCustomer(
                a.getCustomerID(),
                "Your pet " + a.getPetName() + " has been discharged",
                "Stay: " + days + " day(s). Fee: "
                        + String.format("%,.0f", total) + " VND. Please proceed to payment."
        );
        return invoiceID;
    }

    /** Add a new cage (Receptionist). */
    public int addCage(String cageNumber, String cageType,
                       String notes) throws SQLException {
        if (cageDAO.existsByCageNumber(cageNumber)) {
            throw new IllegalStateException(
                    "Cage number " + cageNumber + " already exists.");
        }
        return cageDAO.create(cageNumber, cageType, notes);
    }

    /** Toggle cage maintenance mode (Receptionist). */
    public void toggleCageMaintenance(int cageID,
                                      boolean isActive) throws SQLException {
        Cage cage = cageDAO.findById(cageID);
        if (cage == null)
            throw new IllegalArgumentException("Cage not found: " + cageID);
        if (!isActive && "Occupied".equals(cage.getStatus()))
            throw new IllegalStateException(
                    "Cannot set to maintenance — cage is currently occupied.");
        cageDAO.updateActive(cageID, isActive);
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

    // ── Veterinarian ──────────────────────────────────────────────────────────

    public void saveDailyAssessment(int admissionID, int staffID,
                                    String condition, String treatment)
            throws SQLException {
        if (dao.hasTodayAssessment(admissionID)) {
            throw new IllegalStateException(
                    "Today's assessment has already been submitted.");
        }
        dao.createAssessment(admissionID, staffID,
                condition  != null ? condition.trim()  : "",
                treatment  != null ? treatment.trim()  : "");

        InpatientAdmission a = dao.findById(admissionID);
        if (a != null) {
            notifDAO.createForCustomer(
                    a.getCustomerID(),
                    "Daily update for " + a.getPetName(),
                    "Today's condition: " + condition);
        }
    }

    public void markCritical(int admissionID) throws SQLException {
        dao.updateStatus(admissionID, "Critical");
        InpatientAdmission a = dao.findById(admissionID);
        if (a != null) {
            notifDAO.createForCustomer(
                    a.getCustomerID(),
                    "⚠️ URGENT: " + a.getPetName() + " is in Critical Condition",
                    "Please contact the clinic immediately.");
        }
    }

    public List<DailyAssessment> getAssessments(int admissionID)
            throws SQLException {
        return dao.findAssessmentsByAdmission(admissionID);
    }

    public boolean hasTodayAssessment(int admissionID) throws SQLException {
        return dao.hasTodayAssessment(admissionID);
    }


    public List<String> getOccupiedCages() throws SQLException {
        return cageDAO.findAll().stream()
                .filter(cage -> "Occupied".equals(cage.getStatus()))
                .map(Cage::getCageNumber)
                .toList();
    }
}