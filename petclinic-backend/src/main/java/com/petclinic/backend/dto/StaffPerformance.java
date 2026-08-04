package com.petclinic.backend.dto;

import java.math.BigDecimal;

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

    public int getTotalAssigned() {
        return completedCases + cancelledCases + noShowCases;
    }

    public double getCompletionRate() {
        int total = getTotalAssigned();
        return total == 0 ? 0.0 : (completedCases * 100.0) / total;
    }
}
