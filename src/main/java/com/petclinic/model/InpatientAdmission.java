package com.petclinic.model;

import java.time.LocalDate;

/**
 * Maps exactly to dbo.InpatientAdmissions (updated schema with CageID FK)
 *
 * Columns:
 *   AdmissionID   int IDENTITY PK
 *   RecordID      int NOT NULL  FK → MedicalRecords
 *   PetID         int NOT NULL  FK → Pets
 *   CageID        int NULL      FK → Cages  ← NEW
 *   CageNumber    nvarchar NULL            ← kept for backward compat
 *   AdmitDate     date NOT NULL
 *   DischargeDate date NULL
 *   Status        nvarchar NOT NULL
 *     → 'Admitted' | 'Critical' | 'Discharged'
 */
public class InpatientAdmission {

    // ── DB columns ────────────────────────────────────────────────────────────
    private int       admissionID;
    private int       recordID;
    private int       petID;
    private int       cageID;          // FK → Cages.CageID
    private String    cageNumber;      // kept for display (joined from Cages)
    private LocalDate admitDate;
    private LocalDate dischargeDate;
    private String    status;

    // ── Joined fields ─────────────────────────────────────────────────────────
    private String petName;
    private String ownerName;
    private String ownerEmail;
    private int    customerID;
    private int    appointmentID;
    private String cageType;           // joined from Cages.CageType

    public InpatientAdmission() {}

    // ── Getters & Setters ─────────────────────────────────────────────────────
    public int       getAdmissionID()               { return admissionID; }
    public void      setAdmissionID(int v)          { admissionID = v; }

    public int       getRecordID()                  { return recordID; }
    public void      setRecordID(int v)             { recordID = v; }

    public int       getPetID()                     { return petID; }
    public void      setPetID(int v)                { petID = v; }

    public int       getCageID()                    { return cageID; }
    public void      setCageID(int v)               { cageID = v; }

    public String    getCageNumber()                { return cageNumber; }
    public void      setCageNumber(String v)        { cageNumber = v; }

    public LocalDate getAdmitDate()                 { return admitDate; }
    public void      setAdmitDate(LocalDate v)      { admitDate = v; }

    public LocalDate getDischargeDate()             { return dischargeDate; }
    public void      setDischargeDate(LocalDate v)  { dischargeDate = v; }

    public String    getStatus()                    { return status; }
    public void      setStatus(String v)            { status = v; }

    // joined
    public String    getPetName()                   { return petName; }
    public void      setPetName(String v)           { petName = v; }

    public String    getOwnerName()                 { return ownerName; }
    public void      setOwnerName(String v)         { ownerName = v; }

    public String    getOwnerEmail()                { return ownerEmail; }
    public void      setOwnerEmail(String v)        { ownerEmail = v; }

    public int       getCustomerID()                { return customerID; }
    public void      setCustomerID(int v)           { customerID = v; }

    public int       getAppointmentID()             { return appointmentID; }
    public void      setAppointmentID(int v)        { appointmentID = v; }

    public String    getCageType()                  { return cageType; }
    public void      setCageType(String v)          { cageType = v; }
}