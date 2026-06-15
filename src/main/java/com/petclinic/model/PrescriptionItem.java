package com.petclinic.model;

import java.math.BigDecimal;

public class PrescriptionItem {
    private int        itemID;
    private int        recordID;
    private int        medicineID;
    private String     medicineName;
    private String     unit;
    private String     dosage;
    private BigDecimal quantity;
    private BigDecimal unitPrice;

    public PrescriptionItem() {}

    public int        getItemID()          { return itemID; }
    public void       setItemID(int v)     { itemID = v; }
    public int        getRecordID()        { return recordID; }
    public void       setRecordID(int v)   { recordID = v; }
    public int        getMedicineID()      { return medicineID; }
    public void       setMedicineID(int v) { medicineID = v; }
    public String     getMedicineName()    { return medicineName; }
    public void       setMedicineName(String v){ medicineName = v; }
    public String     getUnit()            { return unit; }
    public void       setUnit(String v)    { unit = v; }
    public String     getDosage()          { return dosage; }
    public void       setDosage(String v)  { dosage = v; }
    public BigDecimal getQuantity()        { return quantity; }
    public void       setQuantity(BigDecimal v){ quantity = v; }
    public BigDecimal getUnitPrice()       { return unitPrice; }
    public void       setUnitPrice(BigDecimal v){ unitPrice = v; }

    public BigDecimal getLineTotal() {
        if (quantity == null || unitPrice == null) return BigDecimal.ZERO;
        return quantity.multiply(unitPrice);
    }
}