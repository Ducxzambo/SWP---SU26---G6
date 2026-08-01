package com.petclinic.backend.dto;

import java.math.BigDecimal;

public class ReceiptLineItem {
    private String     name;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal discount = BigDecimal.ZERO;
    private BigDecimal lineTotal;

    public String     getName()             { return name; }
    public void       setName(String v)     { name = v; }
    public BigDecimal getQuantity()         { return quantity; }
    public void       setQuantity(BigDecimal v) { quantity = v; }
    public BigDecimal getUnitPrice()        { return unitPrice; }
    public void       setUnitPrice(BigDecimal v){ unitPrice = v; }
    public BigDecimal getDiscount()         { return discount; }
    public void       setDiscount(BigDecimal v) { discount = v; }
    public BigDecimal getLineTotal()        { return lineTotal; }
    public void       setLineTotal(BigDecimal v){ lineTotal = v; }
}