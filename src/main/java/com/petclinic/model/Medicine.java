package com.petclinic.model;

import java.math.BigDecimal;

public class Medicine {
    private int medicineID;
    private String name;
    private String unit;          // tablet | bottle | ampule ...
    private BigDecimal unitPrice;
    private int stockQty;

    public Medicine() {
    }

    // ── Getters & Setters ────────────────────────────────────────────────────
    public int getMedicineID() {
        return medicineID;
    }

    public void setMedicineID(int v) {
        medicineID = v;
    }

    public String getName() {
        return name;
    }

    public void setName(String v) {
        name = v;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String v) {
        unit = v;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal v) {
        unitPrice = v;
    }

    public int getStockQty() {
        return stockQty;
    }

    public void setStockQty(int v) {
        stockQty = v;
    }

    /**
     * Convenience: display name with unit.
     */
    public String getDisplayName() {
        return name + " (" + unit + ")";
    }
}
