package com.petclinic.backend.model;

import java.math.BigDecimal;

public class StockImportReceiptDetail {
    private int receiptDetailID;
    private int receiptID;
    private String itemType;
    private int itemID;
    private String itemName;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;

    public int getReceiptDetailID() { return receiptDetailID; }
    public void setReceiptDetailID(int value) { receiptDetailID = value; }
    public int getReceiptID() { return receiptID; }
    public void setReceiptID(int value) { receiptID = value; }
    public String getItemType() { return itemType; }
    public void setItemType(String value) { itemType = value; }
    public int getItemID() { return itemID; }
    public void setItemID(int value) { itemID = value; }
    public String getItemName() { return itemName; }
    public void setItemName(String value) { itemName = value; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int value) { quantity = value; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal value) { unitPrice = value; }
    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal value) { lineTotal = value; }
}
