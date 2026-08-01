package com.petclinic.backend.dto;

import java.math.BigDecimal;

public class StaffServiceBreakdown {
    private String serviceName;
    private int completedCount;
    private BigDecimal revenue;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String v) {
        serviceName = v;
    }

    public int getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(int v) {
        completedCount = v;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal v) {
        revenue = v;
    }
}
