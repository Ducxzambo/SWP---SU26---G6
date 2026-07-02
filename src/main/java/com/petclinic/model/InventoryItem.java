package com.petclinic.model;

import java.math.BigDecimal;

public class InventoryItem {
    private String itemType;
    private int itemID;
    private String name;
    private String unit;
    private BigDecimal unitPrice;
    private int stockQty;
    private Integer minStockLevel;

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String v) {
        itemType = v;
    }

    public int getItemID() {
        return itemID;
    }

    public void setItemID(int v) {
        itemID = v;
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

    public Integer getMinStockLevel() {
        return minStockLevel;
    }

    public void setMinStockLevel(Integer v) {
        minStockLevel = v;
    }

    public boolean isMedicine() {
        return "Medicine".equals(itemType);
    }

    public boolean isVaccine() {
        return "Vaccine".equals(itemType);
    }

    public int getEffectiveMinStockLevel() {
        return minStockLevel == null ? 10 : minStockLevel;
    }

    public boolean isOutOfStock() {
        return stockQty <= 0;
    }

    public boolean isLowStock() {
        return !isOutOfStock() && stockQty < getEffectiveMinStockLevel();
    }

    public String getDisplayName() {
        return name + (unit == null || unit.isBlank() ? "" : " (" + unit + ")");
    }
}
