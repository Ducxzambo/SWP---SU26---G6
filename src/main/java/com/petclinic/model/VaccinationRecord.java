package com.petclinic.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class VaccinationRecord {
    private int       vaccineRecordID;
    private int       petID;
    private int       vaccineID;
    private String    vaccineName;
    private int       staffID;
    private String    staffName;
    private Integer   appointmentID;
    private LocalDate administeredDate;
    private LocalDate nextDueDate;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public VaccinationRecord() {}

    public int       getVaccineRecordID()         { return vaccineRecordID; }
    public void      setVaccineRecordID(int v)    { vaccineRecordID = v; }
    public int       getPetID()                   { return petID; }
    public void      setPetID(int v)              { petID = v; }
    public int       getVaccineID()               { return vaccineID; }
    public void      setVaccineID(int v)          { vaccineID = v; }
    public String    getVaccineName()             { return vaccineName; }
    public void      setVaccineName(String v)     { vaccineName = v; }
    public int       getStaffID()                   { return staffID; }
    public void      setStaffID(int v)              { staffID = v; }
    public String    getStaffName()                 { return staffName; }
    public void      setStaffName(String v)         { staffName = v; }
    public Integer   getAppointmentID()           { return appointmentID; }
    public void      setAppointmentID(Integer v)  { appointmentID = v; }
    public LocalDate getAdministeredDate()        { return administeredDate; }
    public void      setAdministeredDate(LocalDate v){ administeredDate = v; }
    public LocalDate getNextDueDate()             { return nextDueDate; }
    public void      setNextDueDate(LocalDate v)  { nextDueDate = v; }

    public String getFormattedAdministeredDate() {
        return administeredDate != null ? administeredDate.format(FMT) : "";
    }
    public String getFormattedNextDueDate() {
        return nextDueDate != null ? nextDueDate.format(FMT) : "Không cần nhắc";
    }
    /** true nếu vaccine sắp đến hạn trong 30 ngày tới */
    public boolean isDueSoon() {
        if (nextDueDate == null) return false;
        LocalDate today = LocalDate.now();
        return !nextDueDate.isBefore(today) && nextDueDate.isBefore(today.plusDays(30));
    }
    /** true nếu vaccine đã quá hạn */
    public boolean isOverdue() {
        return nextDueDate != null && nextDueDate.isBefore(LocalDate.now());
    }
}
