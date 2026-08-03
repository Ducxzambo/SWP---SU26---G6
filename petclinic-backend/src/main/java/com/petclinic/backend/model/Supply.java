package com.petclinic.backend.model;

import java.math.BigDecimal;

public class Supply {
    private int        supplyID;
    private String     name;
    private String     unit;
    private BigDecimal unitPrice;
    private int        stockQty;
    private boolean    isActive;

    public Supply() {}

    public int        getSupplyID()              { return supplyID; }
    public void       setSupplyID(int v)         { supplyID = v; }
    public String     getName()                  { return name; }
    public void       setName(String v)          { name = v; }
    public String     getUnit()                  { return unit; }
    public void       setUnit(String v)          { unit = v; }
    public BigDecimal getUnitPrice()             { return unitPrice; }
    public void       setUnitPrice(BigDecimal v) { unitPrice = v; }
    public int        getStockQty()              { return stockQty; }
    public void       setStockQty(int v)         { stockQty = v; }
    public boolean    isActive()                 { return isActive; }
    public void       setActive(boolean v)       { isActive = v; }

    public String getDisplayName() { return name + " (" + unit + ")"; }
}