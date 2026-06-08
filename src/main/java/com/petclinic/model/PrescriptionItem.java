package com.petclinic.model;

import java.math.BigDecimal;

public class PrescriptionItem {
    private int itemID;
    private int recordID;
    private int medicineID;
    private String dosage;       // e.g. "2 times/day, 1 tablet each"
    private BigDecimal quantity;
    private BigDecimal unitPrice;    // snapshot at time of prescription

    // ── Transient join field ─────────────────────────────────────────────────
    private String medicineName;
    private String medicineUnit;

    public PrescriptionItem() {
    }

    // ── Convenience constructor ───────────────────────────────────────────────
    public PrescriptionItem(int recordID, int medicineID, String dosage,
                            BigDecimal quantity, BigDecimal unitPrice) {
        this.recordID = recordID;
        this.medicineID = medicineID;
        this.dosage = dosage;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────
    public int getItemID() {
        return itemID;
    }

    public void setItemID(int v) {
        itemID = v;
    }

    public int getRecordID() {
        return recordID;
    }

    public void setRecordID(int v) {
        recordID = v;
    }

    public int getMedicineID() {
        return medicineID;
    }

    public void setMedicineID(int v) {
        medicineID = v;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String v) {
        dosage = v;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal v) {
        quantity = v;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal v) {
        unitPrice = v;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public void setMedicineName(String v) {
        medicineName = v;
    }

    public String getMedicineUnit() {
        return medicineUnit;
    }

    public void setMedicineUnit(String v) {
        medicineUnit = v;
    }

    /**
     * Line total = quantity * unitPrice.
     */
    public BigDecimal getLineTotal() {
        if (quantity == null || unitPrice == null) return BigDecimal.ZERO;
        return quantity.multiply(unitPrice);
    }
}
