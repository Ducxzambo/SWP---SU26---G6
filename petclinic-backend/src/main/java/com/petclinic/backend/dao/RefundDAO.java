package com.petclinic.backend.dao;

import com.petclinic.backend.model.Refund;
import com.petclinic.backend.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RefundDAO {

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

    private static final String JOINED_SELECT = """
            SELECT r.*, a.AppointmentDate, a.Status AS ApptStatus,
                   c.FullName AS CustomerName, c.Phone AS CustomerPhone,
                   p.Name AS PetName
            FROM Refunds r
            JOIN Appointments a ON r.AppointmentID = a.AppointmentID
            JOIN Customers c ON a.CustomerID = c.CustomerID
            LEFT JOIN Pets p ON a.PetID = p.PetID
            """;
    public List<Refund> search(String statusFilter, String sortBy) throws SQLException {
        StringBuilder sql = new StringBuilder(JOINED_SELECT).append(" WHERE 1 = 1");
        List<Object> params = new ArrayList<>();

        if (statusFilter != null && !statusFilter.isBlank()) {
            sql.append(" AND r.Status = ?");
            params.add(statusFilter.trim());
        }

        sql.append(" ORDER BY CASE WHEN r.Status = 'Requested' THEN 0 ELSE 1 END, ");
        sql.append(switch (sortBy == null ? "" : sortBy) {
            case "date_desc " -> "r.RequestedAt DESC";
            case "amount_desc" -> "r.PaidAmount DESC";
            case "amount_asc" -> "r.PaidAmount ASC";
            default -> "r.RequestedAt ASC";
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
