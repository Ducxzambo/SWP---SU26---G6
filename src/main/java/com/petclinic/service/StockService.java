package com.petclinic.service;

import com.petclinic.dao.MedicineDAO;
import com.petclinic.model.InventoryItem;
import com.petclinic.model.Medicine;
import com.petclinic.model.PrescriptionItem;
import com.petclinic.model.StockMovementReport;
import com.petclinic.model.StockTransaction;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Service layer for BP-07: Stock Management.
 */
public class StockService {

    private static final int DEFAULT_THRESHOLD = 10;

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
                                                        LocalDate toDate)
            throws SQLException {
        return medicineDAO.findStockTransactions(fromDate, toDate, 50);
    }

    public int recordStockIn(String itemType, Integer itemID, String itemName,
                             String unit, BigDecimal unitPrice, int quantity,
                             Integer minStockLevel, int performedByID)
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

        int threshold;
        if ("Medicine".equals(itemType)) {
            threshold = newItem
                    ? (minStockLevel == null || minStockLevel <= 0 ? DEFAULT_THRESHOLD : minStockLevel)
                    : (minStockLevel == null ? 0 : minStockLevel);
        } else {
            threshold = 0;
        }

        return medicineDAO.stockIn(itemType, itemID, clean(itemName), clean(unit),
                unitPrice, quantity, threshold, performedByID);
    }

    public void updateThreshold(int medicineID, int minStockLevel)
            throws SQLException {
        if (medicineID <= 0) {
            throw new IllegalArgumentException("Thuốc không hợp lệ.");
        }
        if (minStockLevel < 0) {
            throw new IllegalArgumentException("Ngưỡng cảnh báo không được âm.");
        }
        medicineDAO.updateThreshold(medicineID, minStockLevel);
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
