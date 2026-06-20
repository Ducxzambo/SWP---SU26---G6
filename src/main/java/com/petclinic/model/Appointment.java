package com.petclinic.model;

import com.petclinic.dao.AppointmentDAO;

import java.time.LocalDate;
import java.time.LocalTime;

public class Appointment {
    private int appointmentID;
    private int customerID;
    private int petID;
    private int serviceID;
    private Integer assignedVetID;
    private LocalDate appointmentDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
    private Integer slotShift; // 1-4

    // join fields
    private String customerName;
    private String petName;
    private String serviceName;
    private String vetName;
    private String groomerName;
    private Integer assignedGroomerID;

    public Appointment() {
    }

    public int getAppointmentID() {
        return appointmentID;
    }

    public void setAppointmentID(int v) {
        appointmentID = v;
    }

    public int getCustomerID() {
        return customerID;
    }

    public void setCustomerID(int v) {
        customerID = v;
    }

    public int getPetID() {
        return petID;
    }

    public void setPetID(int v) {
        petID = v;
    }

    public int getServiceID() {
        return serviceID;
    }

    public void setServiceID(int v) {
        serviceID = v;
    }

    public Integer getAssignedVetID() {
        return assignedVetID;
    }

    public void setAssignedVetID(Integer v) {
        assignedVetID = v;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(LocalDate v) {
        appointmentDate = v;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime v) {
        startTime = v;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime v) {
        endTime = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String v) {
        status = v;
    }

    public Integer getSlotShift() {
        return slotShift;
    }

    public void setSlotShift(Integer v) {
        slotShift = v;
    }

    // join fields
    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String v) {
        customerName = v;
    }

    public String getPetName() {
        return petName;
    }

    public void setPetName(String v) {
        petName = v;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String v) {
        serviceName = v;
    }

    public String getVetName() {
        return vetName;
    }

    public void setVetName(String v) {
        vetName = v;
    }

    public String getSlotLabel() {
        return AppointmentDAO.shiftLabel(slotShift != null ? slotShift : -1);
    }

    public void setGroomerName(String groomerName) {
        this.groomerName = groomerName;
    }

    public String getGroomerName() {
        return groomerName;
    }

    public Integer getAssignedGroomerID() {
        return assignedGroomerID;
    }

    public void setAssignedGroomerID(Integer assignedGroomerID) {
        this.assignedGroomerID = assignedGroomerID;
    }
}