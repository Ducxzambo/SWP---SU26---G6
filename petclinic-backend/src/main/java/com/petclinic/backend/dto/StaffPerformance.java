package com.petclinic.backend.dto;

import java.math.BigDecimal;

/**
 * Per-staff performance summary, derived from AppointmentServices joined to
 * Appointments within a date range (see StaffStatsDAO). "Case" here means
 * one assigned service line on an appointment - a vet consult, a grooming
 * session, etc.
 */
public class StaffPerformance {
    private int staffID;
    private String fullName;
    private String roleName;
    private int completedCases;
    private int cancelledCases;
    private int noShowCases;
    private BigDecimal revenue;

    public int getStaffID() {
        return staffID;
    }

    public void setStaffID(int v) {
        staffID = v;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String v) {
        fullName = v;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String v) {
        roleName = v;
    }

    public int getCompletedCases() {
        return completedCases;
    }

    public void setCompletedCases(int v) {
        completedCases = v;
    }

    public int getCancelledCases() {
        return cancelledCases;
    }

    public void setCancelledCases(int v) {
        cancelledCases = v;
    }

    public int getNoShowCases() {
        return noShowCases;
    }

    public void setNoShowCases(int v) {
        noShowCases = v;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal v) {
        revenue = v;
    }

    /** All cases ever assigned to this staff in range, regardless of outcome. */
    public int getTotalAssigned() {
        return completedCases + cancelledCases + noShowCases;
    }

    /** % of assigned cases that were actually completed (0 when nothing assigned yet). */
    public double getCompletionRate() {
        int total = getTotalAssigned();
        return total == 0 ? 0.0 : (completedCases * 100.0) / total;
    }
}
