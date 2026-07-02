package com.petclinic.model;

import java.math.BigDecimal;

public class StockMovementReport {
    private String itemType;
    private int itemID;
    private String itemName;
    private BigDecimal totalStockIn = BigDecimal.ZERO;
    private BigDecimal totalStockOut = BigDecimal.ZERO;
    private BigDecimal netChange = BigDecimal.ZERO;

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

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String v) {
        itemName = v;
    }

    public BigDecimal getTotalStockIn() {
        return totalStockIn;
    }

    public void setTotalStockIn(BigDecimal v) {
        totalStockIn = v == null ? BigDecimal.ZERO : v;
    }

    public BigDecimal getTotalStockOut() {
        return totalStockOut;
    }

    public void setTotalStockOut(BigDecimal v) {
        totalStockOut = v == null ? BigDecimal.ZERO : v;
    }

    public BigDecimal getNetChange() {
        return netChange;
    }

    public void setNetChange(BigDecimal v) {
        netChange = v == null ? BigDecimal.ZERO : v;
    }
}
