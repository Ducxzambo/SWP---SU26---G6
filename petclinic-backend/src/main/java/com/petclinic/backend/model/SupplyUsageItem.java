package com.petclinic.backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SupplyUsageItem {
    private int           usageID;
    private int           appointmentID;
    private int           supplyID;
    private int           staffID;
    private BigDecimal    quantity;
    private BigDecimal    unitPrice;     // snapshot
    private LocalDateTime usedAt;
    private String        notes;

    // transient
    private String supplyName;
    private String supplyUnit;

    public SupplyUsageItem() {}

    public int           getUsageID()              { return usageID; }
    public void          setUsageID(int v)         { usageID = v; }
    public int           getAppointmentID()        { return appointmentID; }
    public void          setAppointmentID(int v)   { appointmentID = v; }
    public int           getSupplyID()             { return supplyID; }
    public void          setSupplyID(int v)        { supplyID = v; }
    public int           getStaffID()              { return staffID; }
    public void          setStaffID(int v)         { staffID = v; }
    public BigDecimal    getQuantity()             { return quantity; }
    public void          setQuantity(BigDecimal v) { quantity = v; }
    public BigDecimal    getUnitPrice()            { return unitPrice; }
    public void          setUnitPrice(BigDecimal v){ unitPrice = v; }
    public LocalDateTime getUsedAt()               { return usedAt; }
    public void          setUsedAt(LocalDateTime v){ usedAt = v; }
    public String        getNotes()                { return notes; }
    public void          setNotes(String v)        { notes = v; }
    public String        getSupplyName()           { return supplyName; }
    public void          setSupplyName(String v)   { supplyName = v; }
    public String        getSupplyUnit()           { return supplyUnit; }
    public void          setSupplyUnit(String v)   { supplyUnit = v; }

    public BigDecimal getLineTotal() {
        if (quantity == null || unitPrice == null) return BigDecimal.ZERO;
        return quantity.multiply(unitPrice);
    }
}