package com.petclinic.backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class StockImportReceipt {
    private int receiptID;
    private String receiptCode;
    private int providerID;
    private String providerName;
    private int performedByID;
    private String performedByName;
    private LocalDateTime importedAt;
    private BigDecimal totalAmount;
    private List<StockImportReceiptDetail> details = new ArrayList<>();

    public int getReceiptID() { return receiptID; }
    public void setReceiptID(int value) { receiptID = value; }
    public String getReceiptCode() { return receiptCode; }
    public void setReceiptCode(String value) { receiptCode = value; }
    public int getProviderID() { return providerID; }
    public void setProviderID(int value) { providerID = value; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String value) { providerName = value; }
    public int getPerformedByID() { return performedByID; }
    public void setPerformedByID(int value) { performedByID = value; }
    public String getPerformedByName() { return performedByName; }
    public void setPerformedByName(String value) { performedByName = value; }
    public LocalDateTime getImportedAt() { return importedAt; }
    public void setImportedAt(LocalDateTime value) { importedAt = value; }
    public String getImportedAtDisplay() { return importedAt == null ? "-" : importedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")); }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal value) { totalAmount = value; }
    public List<StockImportReceiptDetail> getDetails() { return details; }
    public void setDetails(List<StockImportReceiptDetail> value) { details = value; }
}
