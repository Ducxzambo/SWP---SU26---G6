package com.petclinic.model;

import java.math.BigDecimal;

public class Service {
    private int serviceID;
    private String name;
    private BigDecimal price;
    private int durationMinutes;
    private boolean isActive;
    private String categoryName; // transient join field

    public Service() {
    }

    public int getServiceID() {
        return serviceID;
    }

    public void setServiceID(int v) {
        serviceID = v;
    }

    public String getName() {
        return name;
    }

    public void setName(String v) {
        name = v;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal v) {
        price = v;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int v) {
        durationMinutes = v;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean v) {
        isActive = v;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String v) {
        categoryName = v;
    }
}