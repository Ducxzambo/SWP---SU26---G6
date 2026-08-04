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

    /**
     * Tạo Invoice mới. Status luôn truyền 'Unpaid'
     * status sẽ được tính lại thành 'PrePaid' ngay khi thanh toán trước 100%
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

    /**
     * Thêm 1 InvoiceItem MỚI phát sinh SAU khi invoice đã tồn tại (vd thuốc kê
     * đơn, dịch vụ chẩn đoán/điều trị chọn thêm lúc khám — BP-04/05), đồng thời
     * CỘNG DỒN LineTotal của dòng đó vào Invoices.TotalAmount (không recompute
     * lại từ đầu, để không đè mất phụ thu OT Fee đã cộng riêng trước đó qua
     * applyOvertimeFeeIfApplicable — fee này KHÔNG có dòng InvoiceItems tương ứng).
     */
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

    /**
     * Nếu appointment của invoice này đã Done và thuộc slot phụ (OT, bắt đầu từ 18:30)
     * và chưa từng tính phụ thu, cộng thêm 5% TotalAmount, đánh dấu OtherFees='OT Fee'
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

    // ── Payment recording (Payments 1-N Invoices qua Payments.InvoiceID) ────────

    /**
     * Ghi nhận 1 khoản thanh toán cho invoice — insert Payments (InvoiceID
     * gắn thẳng vào dòng Payment, không còn bảng join riêng).
     */
    public void insertPayment(int invoiceId, BigDecimal amount, String method) throws SQLException {
        insertPayment(invoiceId, amount, method, null);
    }

    /**
     * Overload cho phép ghi nhận NGƯỜI THỰC HIỆN thu tiền (vd lễ tân thu tiền
     * mặt/chuyển khoản tại quầy — BP-04). Truyền null nếu không xác định được
     * (vd webhook tự động của PayOS, không có staff nào đang thao tác).
     */
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

    /** Tổng số tiền đã thu (SUM Payments.Amount) cho 1 invoice — dùng để tính "còn phải thu". */
    public BigDecimal getAmountPaid(int invoiceId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            return sumAmountPaid(c, invoiceId);
        }
    }

    /**
     * Danh sách Payment thuộc về 1 invoice (1-N trực tiếp qua Payments.InvoiceID
     * — không còn bảng join PaymentInvoices/AllocatedAmount).
     */
    private List<Payment> findPaymentsByInvoice(int invoiceId) throws SQLException {
        String sql = "SELECT p.PaymentID, p.InvoiceID, p.Amount, p.Method, p.PaidAt, "
                + "p.ProcessedByID, s.FullName AS ProcessedByName "
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

    /**
     * Xác nhận thanh toán từ webhook PayOS (nguon xac nhan CHINH THUC).
     * Insert Payments (InvoiceID gắn thẳng), tính lại và cập nhật Status
     * invoice (Unpaid/PrePaid/Paid), chuyển Status appointment sang 'Confirmed'.
     *
     * @return AppointmentID để gọi AssignmentService.autoAssign(), hoặc -1
     *         nếu không có gì thay đổi (ví dụ invoice đã Paid/PrePaid trước đó, webhook gọi lại).
     */
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

                // Idempotency dựa trên SỐ TIỀN CÒN PHẢI THU (TotalAmount hiện tại - đã
                // thu), KHÔNG dựa vào nhãn Status — vì 1 invoice
                // có thể phát sinh thêm InvoiceItems (thuốc, chẩn đoán)
                // sau khi đã ở trạng thái PrePaid/Paid từ 1 đợt thu trước, nên vẫn cần
                // ghi nhận đợt thu MỚI cho phần phát sinh. Chỉ bỏ qua khi thực sự không
                // còn gì phải thu (webhook gọi lại cho cùng 1 giao dịch — idempotent).
                BigDecimal amountPaid = sumAmountPaid(c, invoiceId);
                BigDecimal amountDue = currentTotal.subtract(amountPaid);
                if (amountDue.compareTo(BigDecimal.ZERO) <= 0) {
                    c.commit();
                    return -1;
                }

                // processedByStaffId = null: xác nhận tự động qua webhook PayOS,
                // không có nhân viên nào thao tác trực tiếp (khác với thu tiền mặt/
                // chuyển khoản tại quầy do lễ tân bấm).
                int paymentId = insertPaymentRow(c, invoiceId, BigDecimal.valueOf(amountVnd), "BankTransfer", null);
                if (paymentId > 0) {
                    recomputeAndUpdateStatus(c, invoiceId);
                }

                // Chỉ tự động chuyển Appointment sang 'Confirmed' nếu đang ở trạng thái
                // 'Pending' - đây là luồng đặt lịch ONLINE của khách hàng (Pending ->
                // thanh toán -> Confirmed). Với appointment do LỄ TÂN tạo/offline
                // (Arrived/InProgress/Done), KHÔNG được ghi đè trạng thái:
                // trạng thái khám bệnh của những appointment này do luồng check-in/
                // khám riêng của staff quản lý, thanh toán không quyết định việc đó.
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

    /** Insert Payment (InvoiceID bắt buộc — 1-N trực tiếp với Invoice) */
    private int insertPaymentRow(Connection c, int invoiceId, BigDecimal amount, String method, Integer processedByStaffId)
            throws SQLException {
        String sql = "INSERT INTO Payments (InvoiceID, Amount, Method, PaidAt, ProcessedByID) VALUES (?, ?, ?, GETDATE(), ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, invoiceId);
            ps.setBigDecimal(2, amount);
            ps.setString(3, method);
            if (processedByStaffId != null) ps.setInt(4, processedByStaffId);
            else ps.setNull(4, Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    /**
     * Tính lại Status (Unpaid/PrePaid/Paid) cho 1 invoice dựa trên tổng đã
     * thu (SUM Payments.Amount theo InvoiceID) so với TotalAmount ban đầu
     * (SUM InvoiceItems.LineTotal) và TotalAmount hiện tại (Invoices.TotalAmount),
     * rồi UPDATE Status. Chạy trong cùng connection/transaction với bước ghi Payment
     */
    private void recomputeAndUpdateStatus(Connection c, int invoiceId) throws SQLException {
        BigDecimal amountPaid   = sumAmountPaid(c, invoiceId);
        BigDecimal initialTotal = sumInvoiceItems(c, invoiceId);
        BigDecimal currentTotal;
        try (PreparedStatement ps = c.prepareStatement("SELECT TotalAmount FROM Invoices WHERE InvoiceID = ?")) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                currentTotal = rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        }
        if (currentTotal == null) currentTotal = BigDecimal.ZERO;

        String status = deriveStatus(amountPaid, initialTotal, currentTotal);

        try (PreparedStatement ps = c.prepareStatement("UPDATE Invoices SET Status = ? WHERE InvoiceID = ?")) {
            ps.setString(1, status);
            ps.setInt(2, invoiceId);
            ps.executeUpdate();
        }
    }

    private String deriveStatus(BigDecimal amountPaid, BigDecimal initialTotal, BigDecimal currentTotal) {
        if (amountPaid == null || amountPaid.compareTo(BigDecimal.ZERO) <= 0) return "Unpaid";
        boolean coversCurrent = amountPaid.compareTo(currentTotal) >= 0;
        boolean coversInitial = initialTotal != null && amountPaid.compareTo(initialTotal) >= 0;
        boolean totalGrew     = initialTotal != null && currentTotal.compareTo(initialTotal) > 0;
        if (coversCurrent && !totalGrew) return "PrePaid"; // thanh toán 100% khi đặt lịch
        if (coversCurrent) return "Paid";                  // đã thanh toán hết cả phần phát sinh sau này
        if (coversInitial) return "PrePaid";                // đủ tổng ban đầu, nhưng tổng hiện tại đã tăng thêm (còn thiếu phần phát sinh)
        return "Unpaid";
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

    private BigDecimal sumInvoiceItems(Connection c, int invoiceId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT ISNULL(SUM(LineTotal),0) FROM InvoiceItems WHERE InvoiceID = ?")) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        }
    }
}
