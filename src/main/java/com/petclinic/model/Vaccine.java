package com.petclinic.model;

import java.math.BigDecimal;

public class Vaccine {
    private int        vaccineID;
    private String     name;
    private BigDecimal unitPrice;
    private int        stockQty;

    public Vaccine() {}

    public int        getVaccineID()       { return vaccineID; }
    public void       setVaccineID(int v)  { vaccineID = v; }
    public String     getName()            { return name; }
    public void       setName(String v)    { name = v; }
    public BigDecimal getUnitPrice()       { return unitPrice; }
    public void       setUnitPrice(BigDecimal v){ unitPrice = v; }
    public int        getStockQty()        { return stockQty; }
    public void        setStockQty(int v)  { stockQty = v; }
}