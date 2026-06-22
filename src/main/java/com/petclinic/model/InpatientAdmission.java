package com.petclinic.model;

import java.time.LocalDate;

/**
 * Maps exactly to dbo.InpatientAdmissions
 *
 * Columns:
 *   AdmissionID  int IDENTITY PK
 *   RecordID     int NOT NULL  FK → MedicalRecords
 *   PetID        int NOT NULL  FK → Pets
 *   AdmitDate    date NOT NULL
 *   DischargeDate date NULL
 *   CageNumber   nvarchar(20) NULL
 *   Status       nvarchar(20) NOT NULL
 *     → 'Admitted' | 'Critical' | 'Discharged'
 */
public class InpatientAdmission {

    // ── DB columns ────────────────────────────────────────────────────────────
    private int       admissionID;
    private int       recordID;
    private int       petID;
    private LocalDate admitDate;
    private LocalDate dischargeDate;   // null until discharged
    private String    cageNumber;
    private String    status;

    // ── Joined fields (populated by DAO, not stored in this table) ────────────
    private String petName;
    private String ownerName;
    private String ownerEmail;
    private int    customerID;
    private int    appointmentID;     // from MedicalRecords.AppointmentID

    public InpatientAdmission() {}

    // ── Getters & Setters ─────────────────────────────────────────────────────
    public int       getAdmissionID()                { return admissionID; }
    public void      setAdmissionID(int v)           { admissionID = v; }

    public int       getRecordID()                   { return recordID; }
    public void      setRecordID(int v)              { recordID = v; }

    public int       getPetID()                      { return petID; }
    public void      setPetID(int v)                 { petID = v; }

    public LocalDate getAdmitDate()                  { return admitDate; }
    public void      setAdmitDate(LocalDate v)       { admitDate = v; }

    public LocalDate getDischargeDate()              { return dischargeDate; }
    public void      setDischargeDate(LocalDate v)   { dischargeDate = v; }

    public String    getCageNumber()                 { return cageNumber; }
    public void      setCageNumber(String v)         { cageNumber = v; }

    public String    getStatus()                     { return status; }
    public void      setStatus(String v)             { status = v; }

    // joined
    public String    getPetName()                    { return petName; }
    public void      setPetName(String v)            { petName = v; }

    public String    getOwnerName()                  { return ownerName; }
    public void      setOwnerName(String v)          { ownerName = v; }

    public String    getOwnerEmail()                 { return ownerEmail; }
    public void      setOwnerEmail(String v)         { ownerEmail = v; }

    public int       getCustomerID()                 { return customerID; }
    public void      setCustomerID(int v)            { customerID = v; }

    public int       getAppointmentID()              { return appointmentID; }
    public void      setAppointmentID(int v)         { appointmentID = v; }
}
