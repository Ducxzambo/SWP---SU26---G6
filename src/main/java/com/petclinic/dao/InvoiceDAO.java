package com.petclinic.dao;

import com.petclinic.model.Appointment;
import com.petclinic.model.Invoice;
import com.petclinic.service.BookingService;
import com.petclinic.util.DBConnection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InvoiceDAO {

    private static final BigDecimal OT_FEE_RATE = new BigDecimal("0.05"); // 5%

    /**
     * Tạo Invoice mới. Status truyền vào thay vì hardcode — 'PartiallyPaid'
     * nếu khoản cọc đã được ghi nhận thanh toán tại thời điểm tạo invoice,
     * 'Unpaid' nếu chưa. (Ở luồng booking hiện tại, invoice được tạo NGAY
     * SAU khi đặt lịch — trước khi khách thanh toán cọc ở bước kế tiếp —
     * nên status truyền vào luôn là 'Unpaid'; tham số này tồn tại để hỗ trợ
     * các luồng tạo invoice khác sau này, ví dụ: thu cọc tại quầy.)
     */
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

    public Invoice findByAppointment(int appointmentId) throws SQLException {
        String sql = "SELECT * FROM Invoices WHERE AppointmentID = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, appointmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Invoice inv = mapInvoice(rs);
                inv.setItems(findItems(inv.getInvoiceID()));
                inv.setPayments(findPayments(inv.getInvoiceID()));
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
                inv.setPayments(findPayments(invoiceId));
                applyOvertimeFeeIfApplicable(inv, inv.getAppointmentID());
                return inv;
            }
        }
    }

    /**
     * Nếu appointment của invoice này đã Done VÀ thuộc slot phụ (OT, bắt đầu
     * 18:30) và chưa từng tính phụ thu, cộng thêm 5% TotalAmount vào
     * TotalAmount và đánh dấu OtherFees='OT Fee' (idempotent — chỉ áp 1 lần
     * nhờ check OtherFees rỗng).
     */
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
            // Không để lỗi tính phụ thu làm hỏng việc hiển thị invoice.
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

    private List<Invoice.InvoiceItem> findItems(int invoiceId) throws SQLException {
        String sql = "SELECT * FROM InvoiceItems WHERE InvoiceID = ?";
        List<Invoice.InvoiceItem> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Invoice.InvoiceItem item = new Invoice.InvoiceItem();
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

    public void insertPayment(int invoiceId, BigDecimal amount, String method)
            throws SQLException {
        String sql = "INSERT INTO Payments (InvoiceID, Amount, Method, PaidAt, ProcessedByID) "
                + "VALUES (?, ?, ?, GETDATE(), 1)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            ps.setBigDecimal(2, amount);
            ps.setString(3, method);
            ps.executeUpdate();
        }
    }

    private List<Invoice.Payment> findPayments(int invoiceId) throws SQLException {
        String sql = "SELECT p.*, s.FullName AS ProcessedByName "
                + "FROM Payments p "
                + "JOIN Staff s ON p.ProcessedByID = s.StaffID "
                + "WHERE p.InvoiceID = ? ORDER BY p.PaidAt DESC";
        List<Invoice.Payment> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Invoice.Payment pay = new Invoice.Payment();
                    pay.setPaymentID(rs.getInt("PaymentID"));
                    pay.setInvoiceID(rs.getInt("InvoiceID"));
                    pay.setAmount(rs.getBigDecimal("Amount"));
                    pay.setMethod(rs.getString("Method"));
                    Timestamp ts = rs.getTimestamp("PaidAt");
                    if (ts != null) pay.setPaidAt(ts.toLocalDateTime());
                    pay.setProcessedByName(rs.getString("ProcessedByName"));
                    list.add(pay);
                }
            }
        }
        return list;
    }

    private Invoice mapInvoice(ResultSet rs) throws SQLException {
        Invoice inv = new Invoice();
        inv.setInvoiceID(rs.getInt("InvoiceID"));
        inv.setAppointmentID(rs.getInt("AppointmentID"));
        inv.setCustomerID(rs.getInt("CustomerID"));
        inv.setTotalAmount(rs.getBigDecimal("TotalAmount"));
        inv.setOtherFees(rs.getString("OtherFees"));
        inv.setStatus(rs.getString("Status"));
        return inv;
    }

    public int confirmPaymentInTransaction(int invoiceId, long amountVnd, boolean isFullPayment) throws Exception {
        String invoiceStatus = isFullPayment ? "Paid" : "PartiallyPaid";
        int apptId = -1;

        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                // 1. Update invoice
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE Invoices SET Status = ? WHERE InvoiceID = ?")) {
                    ps.setString(1, invoiceStatus);
                    ps.setInt(2, invoiceId);
                    ps.executeUpdate();
                }

                // 2. Lấy ApptID
                try (PreparedStatement ps = c.prepareStatement("" +
                        "SELECT AppointmentID FROM Invoices WHERE InvoiceID = ?")) {
                    ps.setInt(1, invoiceId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) apptId = rs.getInt(1);
                    }
                }

                // 3. Update Appt
                if (apptId > 0) {
                    try (PreparedStatement ps = c.prepareStatement("" +
                            "UPDATE Appointments SET Status = 'Confirmed' WHERE AppointmentID = ?")) {
                        ps.setInt(1, apptId);
                        ps.executeUpdate();
                    }
                }

                // 4. Insert Payment
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO Payments (InvoiceID, Amount, Method, PaidAt, ProcessedByID) " +
                                "VALUES (?, ?, 'BankTransfer', GETDATE(), 1)")) {
                    ps.setInt(1, invoiceId);
                    ps.setLong(2, amountVnd);
                    ps.executeUpdate();
                }

                c.commit();
                return apptId;

            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }
}