package com.petclinic.backend.dao;

import com.petclinic.backend.model.Refund;
import com.petclinic.backend.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RefundDAO {

    /**
     * Tao 1 yeu cau hoan tien moi (Status mac dinh 'Requested' - xem DEFAULT
     * constraint trong migration). Goi tu AppointmentServlet.handleCancel()
     * TRUOC KHI appointment duoc chuyen sang Cancelled trong cung request -
     * neu insert loi (vd mat ket noi DB) thi exception se duoc nem len va
     * appointment SE KHONG bi huy, tranh tinh trang huy lich nhung mat yeu
     * cau hoan tien.
     */
    public int createRequest(Refund r) throws SQLException {
        String sql = "INSERT INTO Refunds "
                + "(AppointmentID, TotalAmount, PaidAmount, Reason, BankCode, AccountNumber, AccountName) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getAppointmentID());
            ps.setBigDecimal(2, r.getTotalAmount());
            ps.setBigDecimal(3, r.getPaidAmount());
            ps.setString(4, r.getReason());
            ps.setString(5, r.getBankCode());
            ps.setString(6, r.getAccountNumber());
            ps.setString(7, r.getAccountName());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }


    // ── Staff-side review queue (list/detail) ───────────────────────────────────

    private static final String JOINED_SELECT = """
            SELECT r.*, a.AppointmentDate, a.Status AS ApptStatus,
                   c.FullName AS CustomerName, c.Phone AS CustomerPhone,
                   p.Name AS PetName
            FROM Refunds r
            JOIN Appointments a ON r.AppointmentID = a.AppointmentID
            JOIN Customers c ON a.CustomerID = c.CustomerID
            LEFT JOIN Pets p ON a.PetID = p.PetID
            """;

    /**
     * Danh sach yeu cau hoan tien cho man hinh quan ly cua staff. Cac dong
     * Status='Requested' LUON duoc xep truoc (bat ke sortBy la gi), vi day
     * la hang doi can xu ly - "sort" chi quyet dinh thu tu TRONG TUNG NHOM
     * (Requested truoc, con lai sau).
     *
     * @param statusFilter null/blank = tat ca; hoac "Requested"/"Processed"/"Rejected"
     * @param sortBy       "date_desc" (mac dinh) | "date_asc" | "amount_desc" | "amount_asc"
     */
    public List<Refund> search(String statusFilter, String sortBy) throws SQLException {
        StringBuilder sql = new StringBuilder(JOINED_SELECT).append(" WHERE 1 = 1");
        List<Object> params = new ArrayList<>();

        if (statusFilter != null && !statusFilter.isBlank()) {
            sql.append(" AND r.Status = ?");
            params.add(statusFilter.trim());
        }

        sql.append(" ORDER BY CASE WHEN r.Status = 'Requested' THEN 0 ELSE 1 END, ");
        sql.append(switch (sortBy == null ? "" : sortBy) {
            case "date_asc" -> "r.RequestedAt ASC";
            case "amount_desc" -> "r.PaidAmount DESC";
            case "amount_asc" -> "r.PaidAmount ASC";
            default -> "r.RequestedAt DESC";
        });

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                List<Refund> list = new ArrayList<>();
                while (rs.next()) list.add(mapJoinedRefund(rs));
                return list;
            }
        }
    }

    /** Chi tiet 1 yeu cau hoan tien, kem cac truong hien thi tu Appointment/Customer/Pet. */
    public Refund findById(int refundId) throws SQLException {
        String sql = JOINED_SELECT + " WHERE r.RefundID = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, refundId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapJoinedRefund(rs) : null;
            }
        }
    }

    /**
     * Xac nhan da chuyen khoan hoan tien. Guard "AND Status = 'Requested'"
     * trong WHERE de tranh xu ly trung neu nut duoc bam 2 lan (double-submit)
     * hoac 2 tab cung xu ly 1 yeu cau - executeUpdate() tra ve 0 neu request
     * da bi xu ly truoc do, RefundService se dua vao gia tri nay de bao loi
     * ro rang thay vi am tham ghi de.
     */
    public boolean markProcessed(int refundId, int processedByStaffId) throws SQLException {
        String sql = "UPDATE Refunds SET Status = 'Processed', ProcessedByID = ?, RefundedAt = GETDATE() "
                + "WHERE RefundID = ? AND Status = 'Requested'";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, processedByStaffId);
            ps.setInt(2, refundId);
            return ps.executeUpdate() > 0;
        }
    }

    /** Tu choi yeu cau - cung guard idempotency nhu markProcessed(). */
    public boolean markRejected(int refundId, int processedByStaffId, String rejectReason) throws SQLException {
        String sql = "UPDATE Refunds SET Status = 'Rejected', ProcessedByID = ?, RejectReason = ? "
                + "WHERE RefundID = ? AND Status = 'Requested'";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, processedByStaffId);
            ps.setString(2, rejectReason);
            ps.setInt(3, refundId);
            return ps.executeUpdate() > 0;
        }
    }

    private Refund mapJoinedRefund(ResultSet rs) throws SQLException {
        Refund r = mapRefund(rs);
        Date apptDate = rs.getDate("AppointmentDate");
        if (apptDate != null) r.setAppointmentDate(apptDate.toLocalDate());
        r.setAppointmentStatus(rs.getString("ApptStatus"));
        r.setCustomerName(rs.getString("CustomerName"));
        r.setCustomerPhone(rs.getString("CustomerPhone"));
        r.setPetName(rs.wasNull() ? null : rs.getString("PetName"));
        return r;
    }

    private Refund mapRefund(ResultSet rs) throws SQLException {
        Refund r = new Refund();
        r.setRefundID(rs.getInt("RefundID"));
        r.setAppointmentID(rs.getInt("AppointmentID"));
        r.setTotalAmount(rs.getBigDecimal("TotalAmount"));
        r.setPaidAmount(rs.getBigDecimal("PaidAmount"));
        r.setReason(rs.getString("Reason"));
        r.setBankCode(rs.getString("BankCode"));
        r.setAccountNumber(rs.getString("AccountNumber"));
        r.setAccountName(rs.getString("AccountName"));
        r.setStatus(rs.getString("Status"));
        r.setRejectReason(rs.getString("RejectReason"));
        Timestamp requested = rs.getTimestamp("RequestedAt");
        if (requested != null) r.setRequestedAt(requested.toLocalDateTime());
        int staffId = rs.getInt("ProcessedByID");
        if (!rs.wasNull()) r.setProcessedByID(staffId);
        Timestamp refunded = rs.getTimestamp("RefundedAt");
        if (refunded != null) r.setRefundedAt(refunded.toLocalDateTime());
        return r;
    }
}
