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
 *   - bo cot PaymentID (khong can luu co dinh 1 PaymentID - AppointmentID la
 *     du de tra ra Invoice roi cac Payment co InvoiceID do bat cu luc nao
 *     can doi chieu)
 *   - them AppointmentID/Status/RequestedAt/TotalAmount/PaidAmount
 *   - noi long ProcessedByID/RefundedAt ve NULL-able
 * Xem file sql/refund_request_schema_update.sql di kem, PHAI chay truoc khi
 * dung DAO nay.
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

    /**
     * Yeu cau hoan tien gan nhat (neu co) cua 1 appointment - dung de kiem
     * tra idempotency (tranh insert trung neu form bi submit lai) va cho cac
     * man hinh hien thi trang thai yeu cau sau nay.
     */
    public Refund findLatestByAppointment(int appointmentId) throws SQLException {
        String sql = "SELECT TOP 1 * FROM Refunds WHERE AppointmentID = ? ORDER BY RequestedAt DESC";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, appointmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapRefund(rs);
            }
        }
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
