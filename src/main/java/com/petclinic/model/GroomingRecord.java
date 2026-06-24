package com.petclinic.model;

import java.time.LocalDateTime;

public class GroomingRecord {
    private int            recordID;
    private int            appointmentID;
    private int            petID;
    private int            groomerID;
    private String         coatCondition;
    private String         behavior;
    private String         productsUsed;
    private String         notes;
    private boolean        flagForVet;
    private String         flagReason;
    private LocalDateTime  createdAt;

    // Joined display field
    private String         groomerName;

    public GroomingRecord() {}

    public int           getRecordID()              { return recordID; }
    public void          setRecordID(int v)          { recordID = v; }
    public int           getAppointmentID()          { return appointmentID; }
    public void          setAppointmentID(int v)     { appointmentID = v; }
    public int           getPetID()                  { return petID; }
    public void          setPetID(int v)             { petID = v; }
    public int           getGroomerID()               { return groomerID; }
    public void          setGroomerID(int v)          { groomerID = v; }
    public String        getCoatCondition()          { return coatCondition; }
    public void          setCoatCondition(String v)  { coatCondition = v; }
    public String        getBehavior()                { return behavior; }
    public void          setBehavior(String v)        { behavior = v; }
    public String        getProductsUsed()           { return productsUsed; }
    public void          setProductsUsed(String v)   { productsUsed = v; }
    public String        getNotes()                   { return notes; }
    public void          setNotes(String v)           { notes = v; }
    public boolean       isFlagForVet()               { return flagForVet; }
    public void          setFlagForVet(boolean v)     { flagForVet = v; }
    public String        getFlagReason()              { return flagReason; }
    public void          setFlagReason(String v)      { flagReason = v; }
    public LocalDateTime getCreatedAt()               { return createdAt; }
    public void          setCreatedAt(LocalDateTime v){ createdAt = v; }
    public String        getGroomerName()             { return groomerName; }
    public void          setGroomerName(String v)     { groomerName = v; }
}