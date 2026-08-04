package com.petclinic.backend.service;

import com.petclinic.backend.dao.*;
import com.petclinic.backend.model.*;
import com.petclinic.backend.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class InvoiceSyncService {

    private static final Logger LOG = Logger.getLogger(InvoiceSyncService.class.getName());

    private final AppointmentDAO appointmentDAO = new AppointmentDAO();
    private final ServiceDAO     serviceDAO     = new ServiceDAO();
    private final MedicineDAO    medicineDAO    = new MedicineDAO();
    private final SupplyDAO      supplyDAO      = new SupplyDAO();
    private final VaccineDAO     vaccineDAO     = new VaccineDAO();
    private final InvoiceDAO     invoiceDAO     = new InvoiceDAO();
    private final PaymentService paymentService = new PaymentService();

    public void syncAfterExamination(int appointmentId, int staffId,
                                     List<Integer> selectedServiceIds,
                                     List<PrescriptionItem> prescriptionItems, List<SupplyUsageItem> supplyUsageItems) {
        try {
            Appointment appt = appointmentDAO.findById(appointmentId);
            if (appt == null) return;

            Invoice invoice = getOrCreateInvoice(appt);
            if (invoice == null) return;

            addNewlySelectedServices(appointmentId, staffId, appt, invoice, selectedServiceIds);
            addPrescriptionItems(invoice, prescriptionItems);
            addSupplyItems(invoice, supplyUsageItems);
            deductVaccineStockIfNeeded(appointmentId, staffId, invoice);

        } catch (Exception e) {
            LOG.warning("syncAfterExamination thất bại cho appointment " + appointmentId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void syncAfterGrooming(int appointmentId, int staffId) {
        try {
            Appointment appt = appointmentDAO.findById(appointmentId);
            if (appt == null) return;

            Invoice invoice = getOrCreateInvoice(appt);
            if (invoice == null) return;

            deductVaccineStockIfNeeded(appointmentId, staffId, invoice);

        } catch (Exception e) {
            LOG.warning("syncAfterGrooming thất bại cho appointment " + appointmentId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addNewlySelectedServices(int appointmentId, int staffId, Appointment appt,
                                          Invoice invoice, List<Integer> selectedServiceIds) throws Exception {
        if (selectedServiceIds == null || selectedServiceIds.isEmpty()) return;

        Set<Integer> existingServiceIds = new LinkedHashSet<>();
        for (AppointmentService s : appt.getServices()) existingServiceIds.add(s.getServiceID());

        for (Integer sid : selectedServiceIds) {
            if (sid == null || sid <= 0 || existingServiceIds.contains(sid)) continue;

            Service svc = serviceDAO.findById(sid);
            if (svc == null) continue;

            BigDecimal price = svc.getPrice() != null ? svc.getPrice() : BigDecimal.ZERO;
            appointmentDAO.addServiceToAppointment(appointmentId, sid, price, staffId);
            invoiceDAO.addInvoiceItemAndGrowTotal(invoice.getInvoiceID(), "Service", svc.getName(),
                    BigDecimal.ONE, price);

            existingServiceIds.add(sid); // tránh thêm trùng nếu labTestID[]/treatmentID[] có ID lặp
        }
    }

    private void addPrescriptionItems(Invoice invoice, List<PrescriptionItem> items) throws SQLException {
        if (items == null || items.isEmpty()) return;

        for (PrescriptionItem item : items) {
            if (item.getMedicineID() <= 0) continue;

            BigDecimal qty   = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ONE;
            BigDecimal price = item.getUnitPrice();
            String     name  = item.getMedicineName();

            if (price == null || name == null) {
                Medicine med = medicineDAO.findById(item.getMedicineID());
                if (med != null) {
                    if (price == null) price = med.getUnitPrice();
                    if (name == null)  name  = med.getName();
                }
            }
            if (price == null) price = BigDecimal.ZERO;
            if (name == null)  name  = "Thuốc #" + item.getMedicineID();

            invoiceDAO.addInvoiceItemAndGrowTotal(invoice.getInvoiceID(), "Medicine", name, qty, price);
        }
    }

    private void addSupplyItems(Invoice invoice, List<SupplyUsageItem> items) throws SQLException {
        if (items == null || items.isEmpty()) return;

        for (SupplyUsageItem item : items) {
            if (item.getSupplyID() <= 0) continue;

            BigDecimal qty   = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ONE;
            BigDecimal price = item.getUnitPrice();
            String     name  = item.getSupplyName();

            if (price == null || name == null) {
                Supply supply = supplyDAO.findById(item.getSupplyID());
                if (supply != null) {
                    if (price == null) price = supply.getUnitPrice();
                    if (name == null)  name  = supply.getName();
                }
            }
            if (price == null) price = BigDecimal.ZERO;
            if (name == null)  name  = "Vật tư #" + item.getSupplyID();

            invoiceDAO.addInvoiceItemAndGrowTotal(invoice.getInvoiceID(), "Supply", name, qty, price);
        }
    }

    private void deductVaccineStockIfNeeded(int appointmentId, int staffId, Invoice invoice) throws SQLException {
        if (invoice.getItems() == null) return;

        Set<String> processedVaccineNames = new LinkedHashSet<>();
        for (InvoiceItem item : invoice.getItems()) {
            if (!"Vaccine".equals(item.getItemType())) continue;
            String vaccineName = item.getDescription();
            if (vaccineName == null || !processedVaccineNames.add(vaccineName)) continue;

            try (Connection c = DBConnection.getConnection()) {
                Vaccine v = vaccineDAO.findByName(c, vaccineName);
                if (v == null) continue;

                String reason = "Used - AppointmentID=" + appointmentId + " - VaccineID=" + v.getVaccineID();
                if (stockTransactionExists(c, reason)) continue;

                vaccineDAO.deductStock(c, v.getVaccineID(), 1);
                insertVaccineStockTransaction(c, v.getVaccineID(), staffId, reason);
            }
        }
    }

    private boolean stockTransactionExists(Connection c, String reason) throws SQLException {
        String sql = "SELECT COUNT(*) FROM StockTransactions WHERE ItemType = 'Vaccine' AND Reason = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, reason);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private void insertVaccineStockTransaction(Connection c, int vaccineId, int staffId, String reason)
            throws SQLException {
        String sql = "INSERT INTO StockTransactions (ItemType, ItemID, QuantityChange, Reason, PerformedByID, TransactionType) " +
                "VALUES ('Vaccine', ?, -1, ?, ?, 'Export')";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, vaccineId);
            ps.setString(2, reason);
            ps.setInt(3, staffId);
            ps.executeUpdate();
        }
    }


    private Invoice getOrCreateInvoice(Appointment appt) throws Exception {
        Invoice invoice = invoiceDAO.findByAppointment(appt.getAppointmentID());
        if (invoice != null) return invoice;

        int invoiceId = paymentService.createInvoice(appt.getCustomerID(), appt.getAppointmentID(), BigDecimal.ZERO);
        if (invoiceId <= 0) return null;
        return invoiceDAO.findById(invoiceId);
    }
}
