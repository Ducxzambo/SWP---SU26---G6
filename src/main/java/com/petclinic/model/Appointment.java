package com.petclinic.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

public class Appointment {
    private int       appointmentID;
    private int       customerID;
    private int       petID;
    private int       serviceID;
    private Integer   assignedVetID;
    private LocalDate appointmentDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String    status;
    private String    notes;

    // Joined display fields
    private String    petName;
    private String    serviceName;
    private String    categoryName;
    private String    vetName;

    public Appointment() {}

    // ── Business rule: can reschedule/cancel only if > 12h before appointment ─
    public boolean canModify() {
        if (!"Pending".equals(status) && !"Confirmed".equals(status)) return false;
        if (appointmentDate == null || startTime == null) return false;
        LocalDateTime apptDateTime = LocalDateTime.of(appointmentDate, startTime);
        return LocalDateTime.now().plusHours(12).isBefore(apptDateTime);
    }
    // format date to VN
    public String getMonthDisplayVi() {
        if (this.appointmentDate == null) return "";
        return this.appointmentDate.getMonth().getDisplayName(TextStyle.SHORT, new Locale("vi"));
    }

    public String getFormattedAppointmentDate() {
        if (this.appointmentDate == null) return "";
        return this.appointmentDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    // Thêm hàm getter này cho startTime
    public String getFormattedStartTime() {
        if (this.startTime == null) return "";
        return this.startTime.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    // Thêm hàm getter này cho endTime
    public String getFormattedEndTime() {
        if (this.endTime == null) return "";
        return this.endTime.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    public int       getAppointmentID()         { return appointmentID; }
    public void      setAppointmentID(int v)    { appointmentID = v; }
    public int       getCustomerID()            { return customerID; }
    public void      setCustomerID(int v)       { customerID = v; }
    public int       getPetID()                 { return petID; }
    public void      setPetID(int v)            { petID = v; }
    public int       getServiceID()             { return serviceID; }
    public void      setServiceID(int v)        { serviceID = v; }
    public Integer   getAssignedVetID()         { return assignedVetID; }
    public void      setAssignedVetID(Integer v){ assignedVetID = v; }
    public LocalDate getAppointmentDate()       { return appointmentDate; }
    public void      setAppointmentDate(LocalDate v){ appointmentDate = v; }
    public LocalTime getStartTime()             { return startTime; }
    public void      setStartTime(LocalTime v)  { startTime = v; }
    public LocalTime getEndTime()               { return endTime; }
    public void      setEndTime(LocalTime v)    { endTime = v; }
    public String    getStatus()                { return status; }
    public void      setStatus(String v)        { status = v; }
    public String    getNotes()                 { return notes; }
    public void      setNotes(String v)         { notes = v; }
    public String    getPetName()               { return petName; }
    public void      setPetName(String v)       { petName = v; }
    public String    getServiceName()           { return serviceName; }
    public void      setServiceName(String v)   { serviceName = v; }
    public String    getCategoryName()          { return categoryName; }
    public void      setCategoryName(String v)  { categoryName = v; }
    public String    getVetName()               { return vetName; }
    public void      setVetName(String v)       { vetName = v; }
}
