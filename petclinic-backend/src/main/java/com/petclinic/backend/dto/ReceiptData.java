package com.petclinic.backend.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ReceiptData {
    private int    invoiceIdRaw;
    private String invoiceCode;
    private String documentLabel;     // "HÓA ĐƠN 1" | "BIÊN LAI LẦN 1" | "HÓA ĐƠN" | "BIÊN LAI LẦN 2"
    private String issuedAtDisplay;
    private String staffName;
    private String customerName;

    private List<ReceiptLineItem> items = new ArrayList<>();

    private BigDecimal totalQuantity  = BigDecimal.ZERO;
    private BigDecimal subTotal       = BigDecimal.ZERO;  // Tổng tiền hàng
    private BigDecimal discountAmount = BigDecimal.ZERO;  // Chiết khấu, mặc định 0
    private BigDecimal totalPayable   = BigDecimal.ZERO;  // Tổng phải trả = subTotal - discountAmount

    // "HÓA ĐƠN"
    private BigDecimal prepaidAmount;   // Đã trả trước
    private BigDecimal amountDue;       // Khách còn phải trả
    private BigDecimal changeAmount = BigDecimal.ZERO; // Tiền trả lại

    // "BIÊN LAI"
    private BigDecimal paidAmount = BigDecimal.ZERO;  // Đã trả
    private String     paidStatusLabel;               // "PrePaid" | "Paid"

    // "BIÊN LAI LẦN 1"
    private BigDecimal remainingAmount;   // Còn phải thanh toán - null nếu không áp dụng
    private String     remainingDueDate;  // ngày hẹn hiển thị

    private String note;

    public int        getInvoiceIdRaw()             { return invoiceIdRaw; }
    public void        setInvoiceIdRaw(int v)        { invoiceIdRaw = v; }
    public String      getInvoiceCode()              { return invoiceCode; }
    public void        setInvoiceCode(String v)      { invoiceCode = v; }
    public String      getDocumentLabel()            { return documentLabel; }
    public void        setDocumentLabel(String v)    { documentLabel = v; }
    public String      getIssuedAtDisplay()          { return issuedAtDisplay; }
    public void        setIssuedAtDisplay(String v)  { issuedAtDisplay = v; }
    public String      getStaffName()                { return staffName; }
    public void        setStaffName(String v)        { staffName = v; }
    public String      getCustomerName()             { return customerName; }
    public void        setCustomerName(String v)     { customerName = v; }
    public List<ReceiptLineItem> getItems()          { return items; }
    public void        setItems(List<ReceiptLineItem> v) { items = v; }
    public BigDecimal  getTotalQuantity()             { return totalQuantity; }
    public void        setTotalQuantity(BigDecimal v) { totalQuantity = v; }
    public BigDecimal  getSubTotal()                  { return subTotal; }
    public void        setSubTotal(BigDecimal v)      { subTotal = v; }
    public BigDecimal  getDiscountAmount()            { return discountAmount; }
    public void        setDiscountAmount(BigDecimal v){ discountAmount = v; }
    public BigDecimal  getTotalPayable()              { return totalPayable; }
    public void        setTotalPayable(BigDecimal v)  { totalPayable = v; }
    public BigDecimal  getPrepaidAmount()             { return prepaidAmount; }
    public void        setPrepaidAmount(BigDecimal v) { prepaidAmount = v; }
    public BigDecimal  getAmountDue()                 { return amountDue; }
    public void        setAmountDue(BigDecimal v)     { amountDue = v; }
    public BigDecimal  getChangeAmount()              { return changeAmount; }
    public void        setChangeAmount(BigDecimal v)  { changeAmount = v; }
    public BigDecimal  getPaidAmount()                { return paidAmount; }
    public void        setPaidAmount(BigDecimal v)    { paidAmount = v; }
    public String      getPaidStatusLabel()           { return paidStatusLabel; }
    public void        setPaidStatusLabel(String v)   { paidStatusLabel = v; }
    public BigDecimal  getRemainingAmount()           { return remainingAmount; }
    public void        setRemainingAmount(BigDecimal v){ remainingAmount = v; }
    public String      getRemainingDueDate()          { return remainingDueDate; }
    public void        setRemainingDueDate(String v)  { remainingDueDate = v; }
    public String      getNote()                      { return note; }
    public void        setNote(String v)              { note = v; }
}