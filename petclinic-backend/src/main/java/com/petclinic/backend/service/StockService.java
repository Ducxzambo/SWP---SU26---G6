package com.petclinic.backend.service;

import com.petclinic.backend.dao.MedicineDAO;
import com.petclinic.backend.dto.StockMovementReport;
import com.petclinic.backend.dto.StockTransaction;
import com.petclinic.backend.model.InventoryItem;
import com.petclinic.backend.model.Medicine;
import com.petclinic.backend.model.PrescriptionItem;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class StockService {

    private static final int DEFAULT_THRESHOLD = 10;
    private static final int TRANSACTION_HISTORY_LIMIT = 100;

    private final MedicineDAO medicineDAO = new MedicineDAO();

    public List<InventoryItem> searchInventory(String keyword, String itemType,
                                               String stockLevel)
            throws SQLException {
        return medicineDAO.searchInventory(keyword, itemType, stockLevel);
    }

    public List<InventoryItem> getAllInventoryItems() throws SQLException {
        return medicineDAO.searchInventory(null, null, null);
    }

    public List<InventoryItem> getLowStockAlerts() throws SQLException {
        return medicineDAO.findLowStock();
    }

    public List<StockMovementReport> getMovementReport(LocalDate fromDate,
                                                       LocalDate toDate)
            throws SQLException {
        return medicineDAO.getMovementReport(fromDate, toDate);
    }

    public List<StockTransaction> getRecentTransactions(LocalDate fromDate,
                                                        LocalDate toDate,
                                                        String itemType)
            throws SQLException {
        return medicineDAO.findStockTransactions(fromDate, toDate, itemType,
                TRANSACTION_HISTORY_LIMIT);
    }

    public int recordStockIn(String itemType, Integer itemID, String itemName,
                             String unit, BigDecimal unitPrice, int quantity,
                             Integer minStockLevel, int performedByID, Integer providerID)
            throws SQLException {
        validateItemType(itemType);
        if (quantity <= 0) {
            throw new IllegalArgumentException("Số lượng nhập kho phải lớn hơn 0.");
        }

        boolean newItem = itemID == null || itemID <= 0;
        if (newItem) {
            if (isBlank(itemName)) {
                throw new IllegalArgumentException("Vui lòng nhập tên item mới.");
            }
            if ("Medicine".equals(itemType) && isBlank(unit)) {
                throw new IllegalArgumentException("Vui lòng nhập đơn vị thuốc.");
            }
            if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Đơn giá không hợp lệ.");
            }
        }

        // Threshold is only ever set here when creating a brand-new item
        // (Medicine or Vaccine). Adjusting the threshold of an existing
        // item is the Thresholds screen's job, not Stock-In's - keeping
        // that concern in one place avoids a restock accidentally
        // overwriting an alert level someone else configured.
        int threshold = newItem
                ? (minStockLevel == null || minStockLevel <= 0 ? DEFAULT_THRESHOLD : minStockLevel)
                : 0;

        return medicineDAO.stockIn(itemType, itemID, clean(itemName), clean(unit),
                unitPrice, quantity, threshold, performedByID, providerID);
    }

    public void recordStockOut(String itemType, int itemID, int quantity, String reason,
                               int performedByID) throws SQLException {
        validateItemType(itemType);
        if (itemID <= 0 || quantity <= 0) throw new IllegalArgumentException("Invalid item or quantity.");
        medicineDAO.stockOut(itemType, itemID, quantity, clean(reason), performedByID);
    }

    public void updateThreshold(String itemType, int itemID, int minStockLevel)
            throws SQLException {
        validateItemType(itemType);
        if (itemID <= 0) {
            throw new IllegalArgumentException("Item không hợp lệ.");
        }
        if (minStockLevel < 0) {
            throw new IllegalArgumentException("Ngưỡng cảnh báo không được âm.");
        }
        medicineDAO.updateThreshold(itemType, itemID, minStockLevel);
    }

    public List<Medicine> notifyLowStockAfterPrescription(List<PrescriptionItem> items) {
        List<Medicine> lowStock = new ArrayList<>();
        if (items == null || items.isEmpty()) return lowStock;

        Set<Integer> medicineIDs = new LinkedHashSet<>();
        for (PrescriptionItem item : items) {
            if (item != null && item.getMedicineID() > 0) {
                medicineIDs.add(item.getMedicineID());
            }
        }

        for (Integer medicineID : medicineIDs) {
            try {
                Medicine med = medicineDAO.findById(medicineID);
                if (med != null && (med.isLowStock() || med.isOutOfStock())) {
                    lowStock.add(med);
                    System.out.println("[BP-07 LOW STOCK ALERT] " + med.getName()
                            + " current qty=" + med.getStockQty()
                            + ", threshold=" + med.getMinStockLevel());
                }
            } catch (SQLException e) {
                System.err.println("[BP-07 LOW STOCK ALERT FAILED] MedicineID="
                        + medicineID + ": " + e.getMessage());
            }
        }
        return lowStock;
    }

    private void validateItemType(String itemType) {
        if (!"Medicine".equals(itemType) && !"Vaccine".equals(itemType)) {
            throw new IllegalArgumentException("Loại tồn kho không hợp lệ.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }
}
