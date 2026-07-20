package com.petclinic.model;

/**
 * Maps exactly to dbo.Cages
 *
 * Columns:
 *   CageID      int IDENTITY PK
 *   CageNumber  nvarchar(20) NOT NULL UNIQUE
 *   CageType    nvarchar(50) NULL   -- 'Small' | 'Medium' | 'Large' | 'ICU'
 *   IsActive    bit          NOT NULL DEFAULT 1
 *   Notes       nvarchar(200) NULL
 *
 * Status (NOT stored in DB — computed from InpatientAdmissions):
 *   'Available'   → isActive=1, no active admission
 *   'Occupied'    → has active admission (Admitted/Critical)
 *   'Maintenance' → isActive=0
 */
public class Cage {

    // ── DB columns ────────────────────────────────────────────────
    private int     cageID;
    private String  cageNumber;
    private String  cageType;
    private boolean isActive;
    private String  notes;

    // ── Computed/Joined fields (from LEFT JOIN InpatientAdmissions) ──
    private String  status;         // Available | Occupied | Maintenance
    private String  currentPetName; // null if available
    private String  admitDate;      // null if available

    public Cage() {}

    // ── Getters & Setters ─────────────────────────────────────────
    public int     getCageID()                  { return cageID; }
    public void    setCageID(int v)             { cageID = v; }

    public String  getCageNumber()              { return cageNumber; }
    public void    setCageNumber(String v)      { cageNumber = v; }

    public String  getCageType()                { return cageType; }
    public void    setCageType(String v)        { cageType = v; }

    public boolean isActive()                   { return isActive; }
    public void    setActive(boolean v)         { isActive = v; }

    public String  getNotes()                   { return notes; }
    public void    setNotes(String v)           { notes = v; }

    // computed
    public String  getStatus()                  { return status; }
    public void    setStatus(String v)          { status = v; }

    public String  getCurrentPetName()          { return currentPetName; }
    public void    setCurrentPetName(String v)  { currentPetName = v; }

    public String  getAdmitDate()               { return admitDate; }
    public void    setAdmitDate(String v)       { admitDate = v; }

    /** Convenience: true if cage can accept a new pet */
    public boolean isAvailable() {
        return isActive && "Available".equals(status);
    }
}