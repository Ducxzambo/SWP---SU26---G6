package com.petclinic.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class StockTransaction {
    private int transactionID;
    private String itemType;
    private int itemID;
    private String itemName;
    private BigDecimal quantityChange;
    private String reason;
    private Integer performedByID;
    private String performedByName;
    private LocalDateTime transactionDate;

    public int getTransactionID() {
        return transactionID;
    }

    public void setTransactionID(int v) {
        transactionID = v;
    }

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

    public BigDecimal getQuantityChange() {
        return quantityChange;
    }

    public void setQuantityChange(BigDecimal v) {
        quantityChange = v;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String v) {
        reason = v;
    }

    public Integer getPerformedByID() {
        return performedByID;
    }

    public void setPerformedByID(Integer v) {
        performedByID = v;
    }

    public String getPerformedByName() {
        return performedByName;
    }

    public void setPerformedByName(String v) {
        performedByName = v;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime v) {
        transactionDate = v;
    }

    public LocalDateTime getCreatedAt() {
        return transactionDate;
    }

    public boolean isStockIn() {
        return quantityChange != null && quantityChange.compareTo(BigDecimal.ZERO) > 0;
    }

    public BigDecimal getAbsoluteQuantity() {
        if (quantityChange == null) return BigDecimal.ZERO;
        return quantityChange.abs();
    }
}
