package com.petclinic.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class MedicalRecord {
    private int recordID;
    private int appointmentID;
    private int petID;
    private int staffID;
    private BigDecimal weight;
    private BigDecimal temperature;
    private String symptoms;
    private String diagnosis;
    private String treatmentPlan;
    private LocalDateTime createdAt;

    // ── Transient fields ─────────────────────────────────────────────────────
    private String petName;
    private String staffName;
    private String ownerName;
    private List<PrescriptionItem> prescriptionItems;

    public MedicalRecord() {
    }

    // ── Getters & Setters ────────────────────────────────────────────────────
    public int getRecordID() {
        return recordID;
    }

    public void setRecordID(int v) {
        recordID = v;
    }

    public int getAppointmentID() {
        return appointmentID;
    }

    public void setAppointmentID(int v) {
        appointmentID = v;
    }

    public int getPetID() {
        return petID;
    }

    public void setPetID(int v) {
        petID = v;
    }

    public int getStaffID() {
        return staffID;
    }

    public void setStaffID(int v) {
        staffID = v;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal v) {
        weight = v;
    }

    public BigDecimal getTemperature() {
        return temperature;
    }

    public void setTemperature(BigDecimal v) {
        temperature = v;
    }

    public String getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(String v) {
        symptoms = v;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String v) {
        diagnosis = v;
    }

    public String getTreatmentPlan() {
        return treatmentPlan;
    }

    public void setTreatmentPlan(String v) {
        treatmentPlan = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime v) {
        createdAt = v;
    }

    // transient
    public String getPetName() {
        return petName;
    }

    public void setPetName(String v) {
        petName = v;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String v) {
        staffName = v;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String v) {
        ownerName = v;
    }

    public List<PrescriptionItem> getPrescriptionItems() {
        return prescriptionItems;
    }

    public void setPrescriptionItems(List<PrescriptionItem> v) {
        prescriptionItems = v;
    }

    /**
     * True if at least one prescription item exists.
     */
    public boolean hasPrescription() {
        return prescriptionItems != null && !prescriptionItems.isEmpty();
    }
}
