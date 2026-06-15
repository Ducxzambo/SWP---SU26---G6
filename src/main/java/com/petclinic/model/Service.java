package com.petclinic.model;

import java.math.BigDecimal;

public class Service {
    private int        serviceID;
    private int        categoryID;
    private String     name;
    private BigDecimal price;
    private int        durationMinutes;
    private boolean    isActive;

    public Service() {}

    public int        getServiceID()       { return serviceID; }
    public void       setServiceID(int v)  { serviceID = v; }
    public int        getCategoryID()      { return categoryID; }
    public void       setCategoryID(int v) { categoryID = v; }
    public String     getName()            { return name; }
    public void       setName(String v)    { name = v; }
    public BigDecimal getPrice()           { return price; }
    public void       setPrice(BigDecimal v){ price = v; }
    public int        getDurationMinutes() { return durationMinutes; }
    public void       setDurationMinutes(int v){ durationMinutes = v; }
    public boolean    isActive()           { return isActive; }
    public void       setActive(boolean v) { isActive = v; }
}
