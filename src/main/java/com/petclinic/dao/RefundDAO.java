package com.petclinic.dao;

import com.petclinic.model.Refund;
import com.petclinic.util.DBConnection;

import java.sql.*;

/**
 * DAO cho bang [Refunds].
 *
 * LUU Y ve schema: bang Refunds ban dau chi phuc vu nghiep vu STAFF DA XU LY
 * xong 1 khoan hoan tien (PaymentID/ProcessedByID/RefundedAt NOT NULL). Tinh
 * nang hien tai (khach hang tick "Yeu cau hoan tien" luc huy lich) chi tao ra
 * 1 YEU CAU - chua co nhan vien nao xu ly - nen migration:
 *   - them AppointmentID/Status/RequestedAt/TotalAmount/PaidAmount
 *   - noi long ProcessedByID/RefundedAt ve NULL-able
 */
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
        Timestamp requested = rs.getTimestamp("RequestedAt");
        if (requested != null) r.setRequestedAt(requested.toLocalDateTime());
        int staffId = rs.getInt("ProcessedByID");
        if (!rs.wasNull()) r.setProcessedByID(staffId);
        Timestamp refunded = rs.getTimestamp("RefundedAt");
        if (refunded != null) r.setRefundedAt(refunded.toLocalDateTime());
        return r;
    }
}
