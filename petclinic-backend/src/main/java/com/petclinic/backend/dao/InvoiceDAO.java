package com.petclinic.backend.dao;

import com.petclinic.backend.model.Appointment;
import com.petclinic.backend.model.Invoice;
import com.petclinic.backend.model.InvoiceItem;
import com.petclinic.backend.model.Payment;
import com.petclinic.backend.service.BookingService;
import com.petclinic.backend.util.DBConnection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InvoiceDAO {

    private static final BigDecimal OT_FEE_RATE = new BigDecimal("0.05"); // 5%

    public int createInvoice(int customerId, int appointmentId, BigDecimal totalAmount, String status) throws Exception {
        String sql = "INSERT INTO Invoices (AppointmentID, CustomerID, TotalAmount, Status) "
                + "VALUES (?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, appointmentId);
            ps.setInt(2, customerId);
            ps.setBigDecimal(3, totalAmount);
            ps.setString(4, status);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    public void addInvoiceItem(int invoiceId, String itemType, String description,
                               BigDecimal quantity, BigDecimal unitPrice) throws Exception {
        String sql = "INSERT INTO InvoiceItems (InvoiceID, ItemType, Description, Quantity, UnitPrice) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            ps.setString(2, itemType);
            ps.setString(3, description);
            ps.setBigDecimal(4, quantity);
            ps.setBigDecimal(5, unitPrice);
            ps.executeUpdate();
        }
    }

    public void addInvoiceItemAndGrowTotal(int invoiceId, String itemType, String description,
                                           BigDecimal quantity, BigDecimal unitPrice) throws SQLException {
        BigDecimal lineTotal = quantity.multiply(unitPrice);
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO InvoiceItems (InvoiceID, ItemType, Description, Quantity, UnitPrice) "
                                + "VALUES (?, ?, ?, ?, ?)")) {
                    ps.setInt(1, invoiceId);
                    ps.setString(2, itemType);
                    ps.setString(3, description);
                    ps.setBigDecimal(4, quantity);
                    ps.setBigDecimal(5, unitPrice);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE Invoices SET TotalAmount = TotalAmount + ? WHERE InvoiceID = ?")) {
                    ps.setBigDecimal(1, lineTotal);
                    ps.setInt(2, invoiceId);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    public Invoice findByAppointment(int appointmentId) throws SQLException {
        String sql = "SELECT * FROM Invoices WHERE AppointmentID = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, appointmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Invoice inv = mapInvoice(rs);
                inv.setItems(findItems(inv.getInvoiceID()));
                inv.setPayments(findPaymentsByInvoice(inv.getInvoiceID()));
                applyOvertimeFeeIfApplicable(inv, appointmentId);
                return inv;
            }
        }
    }

    public Invoice findById(int invoiceId) throws SQLException {
        String sql = "SELECT * FROM Invoices WHERE InvoiceID = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Invoice inv = mapInvoice(rs);
                inv.setItems(findItems(invoiceId));
                inv.setPayments(findPaymentsByInvoice(invoiceId));
                applyOvertimeFeeIfApplicable(inv, inv.getAppointmentID());
                return inv;
            }
        }
    }


    private void applyOvertimeFeeIfApplicable(Invoice inv, int appointmentId) {
        try {
            if (inv == null) return;
            if (inv.getOtherFees() != null && !inv.getOtherFees().isBlank()) return;

            Appointment appt = new AppointmentDAO().findById(appointmentId);
            if (appt == null) return;
            if (!"Done".equals(appt.getStatus())) return;
            if (!BookingService.isOvertimeSlotStart(appt.getStartTime())) return;

            BigDecimal fee = inv.getTotalAmount().multiply(OT_FEE_RATE).setScale(2, RoundingMode.HALF_UP);

            try (Connection c = DBConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "UPDATE Invoices SET OtherFees = ?, TotalAmount = TotalAmount + ? " +
                                 "WHERE InvoiceID = ?")) {
                ps.setString(1, "OT Fee");
                ps.setBigDecimal(2, fee);
                ps.setInt(3, inv.getInvoiceID());
                ps.executeUpdate();
            }

            inv.setOtherFees("OT Fee");
            inv.setTotalAmount(inv.getTotalAmount().add(fee));
        } catch (Exception ignored) {
            // Không để lỗi tính phụ thu làm hỏng việc hiển thị invoice
        }
    }

    public void updateStatus(int invoiceId, String status) throws SQLException {
        String sql = "UPDATE Invoices SET Status = ? WHERE InvoiceID = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, invoiceId);
            ps.executeUpdate();
        }
    }

    private List<InvoiceItem> findItems(int invoiceId) throws SQLException {
        String sql = "SELECT * FROM InvoiceItems WHERE InvoiceID = ?";
        List<InvoiceItem> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    InvoiceItem item = new InvoiceItem();
                    item.setInvoiceItemID(rs.getInt("InvoiceItemID"));
                    item.setInvoiceID(rs.getInt("InvoiceID"));
                    item.setItemType(rs.getString("ItemType"));
                    item.setDescription(rs.getString("Description"));
                    item.setQuantity(rs.getBigDecimal("Quantity"));
                    item.setUnitPrice(rs.getBigDecimal("UnitPrice"));
                    item.setLineTotal(rs.getBigDecimal("LineTotal"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    public void insertPayment(int invoiceId, BigDecimal amount, String method) throws SQLException {
        insertPayment(invoiceId, amount, method, null);
    }

    public void insertPayment(int invoiceId, BigDecimal amount, String method,
                              Integer processedByStaffId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                int paymentId = insertPaymentRow(c, invoiceId, amount, method, processedByStaffId);
                if (paymentId > 0) {
                    recomputeAndUpdateStatus(c, invoiceId);
                }
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    public BigDecimal getAmountPaid(int invoiceId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            return sumAmountPaid(c, invoiceId);
        }
    }

    private List<Payment> findPaymentsByInvoice(int invoiceId) throws SQLException {
        String sql = "SELECT p.PaymentID, p.InvoiceID, p.Amount, p.Method, p.PaidAt, "
                + "p.ProcessedByID, p.PaymentCode, s.FullName AS ProcessedByName "
                + "FROM Payments p "
                + "LEFT JOIN Staff s ON p.ProcessedByID = s.StaffID "
                + "WHERE p.InvoiceID = ? ORDER BY p.PaidAt DESC";
        List<Payment> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Payment pay = new Payment();
                    pay.setPaymentID(rs.getInt("PaymentID"));
                    pay.setInvoiceID(rs.getInt("InvoiceID"));
                    pay.setAmount(rs.getBigDecimal("Amount"));
                    pay.setMethod(rs.getString("Method"));
                    pay.setPaymentCode(rs.getString("PaymentCode"));   // ★ NEW
                    Timestamp ts = rs.getTimestamp("PaidAt");
                    if (ts != null) pay.setPaidAt(ts.toLocalDateTime());
                    int staffId = rs.getInt("ProcessedByID");
                    if (!rs.wasNull()) pay.setProcessedByStaffID(staffId);
                    pay.setProcessedByName(rs.getString("ProcessedByName"));
                    list.add(pay);
                }
            }
        }
        return list;
    }

    // mapInvoice(): thêm 1 dòng
    private Invoice mapInvoice(ResultSet rs) throws SQLException {
        Invoice inv = new Invoice();
        inv.setInvoiceID(rs.getInt("InvoiceID"));
        inv.setInvoiceCode(rs.getString("InvoiceCode"));   // ★ NEW
        inv.setAppointmentID(rs.getInt("AppointmentID"));
        inv.setCustomerID(rs.getInt("CustomerID"));
        inv.setTotalAmount(rs.getBigDecimal("TotalAmount"));
        inv.setOtherFees(rs.getString("OtherFees"));
        inv.setStatus(rs.getString("Status"));
        return inv;
    }

    public int confirmPaymentInTransaction(int invoiceId, long amountVnd, boolean isFullPayment) throws Exception {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                int apptId = -1;
                String apptStatus = null;
                BigDecimal currentTotal = BigDecimal.ZERO;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT i.AppointmentID, i.TotalAmount, a.Status AS ApptStatus " +
                                "FROM Invoices i JOIN Appointments a ON a.AppointmentID = i.AppointmentID " +
                                "WHERE i.InvoiceID = ?")) {
                    ps.setInt(1, invoiceId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            apptId = rs.getInt("AppointmentID");
                            currentTotal = rs.getBigDecimal("TotalAmount");
                            apptStatus = rs.getString("ApptStatus");
                        }
                    }
                }
                if (apptId <= 0) { c.commit(); return -1; }
                if (currentTotal == null) currentTotal = BigDecimal.ZERO;

                BigDecimal amountPaid = sumAmountPaid(c, invoiceId);
                BigDecimal amountDue = currentTotal.subtract(amountPaid);
                if (amountDue.compareTo(BigDecimal.ZERO) <= 0) {
                    c.commit();
                    return -1;
                }

                int paymentId = insertPaymentRow(c, invoiceId, BigDecimal.valueOf(amountVnd), "BankTransfer", null);
                if (paymentId > 0) {
                    recomputeAndUpdateStatus(c, invoiceId);
                }

                if ("Pending".equals(apptStatus)) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "UPDATE Appointments SET Status = 'Confirmed' WHERE AppointmentID = ?")) {
                        ps.setInt(1, apptId);
                        ps.executeUpdate();
                    }
                }

                c.commit();
                return apptId;

            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    private int insertPaymentRow(Connection c, int invoiceId, BigDecimal amount, String method,
                                 Integer processedByStaffId) throws SQLException {
        String paymentCode = generatePaymentCode(c, invoiceId);
        String sql = "INSERT INTO Payments (InvoiceID, Amount, Method, PaidAt, ProcessedByID, PaymentCode) "
                + "VALUES (?, ?, ?, GETDATE(), ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, invoiceId);
            ps.setBigDecimal(2, amount);
            ps.setString(3, method);
            if (processedByStaffId != null) ps.setInt(4, processedByStaffId);
            else ps.setNull(4, Types.INTEGER);
            ps.setString(5, paymentCode);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    private String generatePaymentCode(Connection c, int invoiceId) throws SQLException {
        String invoiceCode;
        try (PreparedStatement ps = c.prepareStatement("SELECT InvoiceCode FROM Invoices WHERE InvoiceID = ?")) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                invoiceCode = rs.next() ? rs.getString(1) : ("INV" + String.format("%06d", invoiceId));
            }
        }
        int seq;
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM Payments WHERE InvoiceID = ?")) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                seq = (rs.next() ? rs.getInt(1) : 0) + 1;
            }
        }
        return "PAY_" + invoiceCode + "_" + String.format("%02d", seq);
    }

    private void recomputeAndUpdateStatus(Connection c, int invoiceId) throws SQLException {
        BigDecimal amountPaid = sumAmountPaid(c, invoiceId);
        BigDecimal currentTotal;
        String apptStatus = null;
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT i.TotalAmount, a.Status AS ApptStatus " +
                        "FROM Invoices i JOIN Appointments a ON a.AppointmentID = i.AppointmentID " +
                        "WHERE i.InvoiceID = ?")) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    currentTotal = rs.getBigDecimal("TotalAmount");
                    apptStatus = rs.getString("ApptStatus");
                } else {
                    currentTotal = BigDecimal.ZERO;
                }
            }
        }
        if (currentTotal == null) currentTotal = BigDecimal.ZERO;

        boolean appointmentDone = "Done".equals(apptStatus);
        String status = deriveStatus(amountPaid, currentTotal, appointmentDone);

        try (PreparedStatement ps = c.prepareStatement("UPDATE Invoices SET Status = ? WHERE InvoiceID = ?")) {
            ps.setString(1, status);
            ps.setInt(2, invoiceId);
            ps.executeUpdate();
        }
    }

    private String deriveStatus(BigDecimal amountPaid, BigDecimal currentTotal, boolean appointmentDone) {
        if (amountPaid == null || amountPaid.compareTo(BigDecimal.ZERO) <= 0) return "Unpaid";
        boolean coversCurrent = amountPaid.compareTo(currentTotal) >= 0;
        if (coversCurrent) return appointmentDone ? "Paid" : "PrePaid";
        return "PrePaid"; // đã trả một phần (đặt cọc) — không rơi về Unpaid
    }

    public void recomputeStatusForAppointment(int appointmentId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            Integer invoiceId = null;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT InvoiceID FROM Invoices WHERE AppointmentID = ?")) {
                ps.setInt(1, appointmentId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) invoiceId = rs.getInt(1);
                }
            }
            if (invoiceId != null) {
                recomputeAndUpdateStatus(c, invoiceId);
            }
        }
    }

    private BigDecimal sumAmountPaid(Connection c, int invoiceId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT ISNULL(SUM(Amount),0) FROM Payments WHERE InvoiceID = ?")) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        }
    }
}
