package com.petclinic.model;

import java.math.BigDecimal;

public class Medicine {
    private int medicineID;
    private String name;
    private String unit;          // tablet | bottle | ampule ...
    private BigDecimal unitPrice;
    private int stockQty;
    private int minStockLevel;

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

    public int getMinStockLevel() {
        return minStockLevel;
    }

    public void setMinStockLevel(int v) {
        minStockLevel = v;
    }

    /**
     * Convenience: display name with unit.
     */
    public String getDisplayName() {
        return name + " (" + unit + ")";
    }

    public boolean isOutOfStock() {
        return stockQty <= 0;
    }

    public boolean isLowStock() {
        return !isOutOfStock() && minStockLevel > 0 && stockQty < minStockLevel;
    }

    public String getStockStatus() {
        if (isOutOfStock()) return "Out of stock";
        if (isLowStock()) return "Low stock";
        return "Available";
    }
}
