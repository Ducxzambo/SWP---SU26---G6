package com.petclinic.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class MedicalRecord {
    private int           recordID;
    private int           appointmentID;
    private int           petID;
    private int           vetID;
    private BigDecimal    weight;
    private BigDecimal    temperature;
    private String        symptoms;
    private String        diagnosis;
    private String        treatmentPlan;
    private LocalDateTime createdAt;

    // Joined
    private String        vetName;
    private List<PrescriptionItem> prescriptions;

    public MedicalRecord() {}

    public int           getRecordID()          { return recordID; }
    public void          setRecordID(int v)     { recordID = v; }
    public int           getAppointmentID()     { return appointmentID; }
    public void          setAppointmentID(int v){ appointmentID = v; }
    public int           getPetID()             { return petID; }
    public void          setPetID(int v)        { petID = v; }
    public int           getVetID()             { return vetID; }
    public void          setVetID(int v)        { vetID = v; }
    public BigDecimal    getWeight()            { return weight; }
    public void          setWeight(BigDecimal v){ weight = v; }
    public BigDecimal    getTemperature()       { return temperature; }
    public void          setTemperature(BigDecimal v){ temperature = v; }
    public String        getSymptoms()          { return symptoms; }
    public void          setSymptoms(String v)  { symptoms = v; }
    public String        getDiagnosis()         { return diagnosis; }
    public void          setDiagnosis(String v) { diagnosis = v; }
    public String        getTreatmentPlan()     { return treatmentPlan; }
    public void          setTreatmentPlan(String v){ treatmentPlan = v; }
    public LocalDateTime getCreatedAt()         { return createdAt; }
    public void          setCreatedAt(LocalDateTime v){ createdAt = v; }
    public String        getVetName()           { return vetName; }
    public void          setVetName(String v)   { vetName = v; }
    public List<PrescriptionItem> getPrescriptions()              { return prescriptions; }
    public void                   setPrescriptions(List<PrescriptionItem> v){ prescriptions = v; }
}