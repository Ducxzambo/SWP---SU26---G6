package com.petclinic.model;

import java.math.BigDecimal;

/**
 * 1 dòng bảng InvoiceItems — 1 mục chi phí cụ thể trong hoá đơn (dịch vụ,
 * vaccine, thuốc, hoặc chi phí khác — phân biệt qua itemType).
 *
 * Quan hệ N-1 với Invoice (nhiều item thuộc đúng 1 invoice), tương tự
 * Payment (nhiều Payment thuộc đúng 1 Invoice) — cả 2 đều tách thành class
 * top-level riêng (thay vì lồng trong Invoice) để dễ tái sử dụng ở nơi khác
 * nếu cần (vd truy vấn item/payment không qua Invoice).
 */
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
