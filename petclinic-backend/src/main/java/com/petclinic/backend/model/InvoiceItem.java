package com.petclinic.backend.model;

import java.math.BigDecimal;

public class InvoiceItem {
    private int         invoiceItemID;
    private int         invoiceID;
    private String      itemType;
    private String      description;
    private BigDecimal  quantity;
    private BigDecimal  unitPrice;
    private BigDecimal  lineTotal;

    public InvoiceItem() {}

    public int        getInvoiceItemID()        { return invoiceItemID; }
    public void       setInvoiceItemID(int v)   { invoiceItemID = v; }
    public int        getInvoiceID()            { return invoiceID; }
    public void       setInvoiceID(int v)       { invoiceID = v; }
    public String     getItemType()             { return itemType; }
    public void       setItemType(String v)     { itemType = v; }
    public String     getDescription()          { return description; }
    public void       setDescription(String v)  { description = v; }
    public BigDecimal getQuantity()              { return quantity; }
    public void       setQuantity(BigDecimal v) { quantity = v; }
    public BigDecimal getUnitPrice()             { return unitPrice; }
    public void       setUnitPrice(BigDecimal v){ unitPrice = v; }
    public BigDecimal getLineTotal()             { return lineTotal; }
    public void       setLineTotal(BigDecimal v){ lineTotal = v; }
}
