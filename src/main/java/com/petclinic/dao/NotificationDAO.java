package com.petclinic.dao;

import com.petclinic.model.Notification;
import com.petclinic.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Full NotificationDAO — thay thế hoàn toàn bản cũ.
 */
public class NotificationDAO {

    // ── Read ──────────────────────────────────────────────────────────────────

    /** Lấy tất cả thông báo của 1 customer, mới nhất lên đầu. */
    public List<Notification> findByCustomer(int customerId) throws SQLException {
        String sql = "SELECT * FROM Notifications "
                + "WHERE CustomerID = ? "
                + "AND (ExpiresAt IS NULL OR ExpiresAt > GETDATE()) "
                + "ORDER BY CreatedAt DESC";
        return query(sql, ps -> ps.setInt(1, customerId));
    }

    /** Lọc theo type category (REMINDER, PAYMENT, EXAM_RESULT, CARE_TIP, SUPPORT, INFO). */
    public List<Notification> findByCustomerAndType(int customerId, String type)
            throws SQLException {
        String sql = "SELECT * FROM Notifications "
                + "WHERE CustomerID = ? AND Type = ? "
                + "AND (ExpiresAt IS NULL OR ExpiresAt > GETDATE()) "
                + "ORDER BY CreatedAt DESC";
        return query(sql, ps -> { ps.setInt(1, customerId); ps.setString(2, type); });
    }

    /** Chỉ lấy chưa đọc. */
    public List<Notification> findUnread(int customerId) throws SQLException {
        String sql = "SELECT * FROM Notifications "
                + "WHERE CustomerID = ? AND IsRead = 0 "
                + "AND (ExpiresAt IS NULL OR ExpiresAt > GETDATE()) "
                + "ORDER BY CreatedAt DESC";
        return query(sql, ps -> ps.setInt(1, customerId));
    }

    /** Đếm chưa đọc (dùng cho badge header). */
    public int countUnread(int customerId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Notifications "
                + "WHERE CustomerID = ? AND IsRead = 0 "
                + "AND (ExpiresAt IS NULL OR ExpiresAt > GETDATE())";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    /** Tạo 1 thông báo. Returns generated ID. */
    public int create(Notification n) throws SQLException {
        String sql = "INSERT INTO Notifications "
                + "(CustomerID, StaffID, Title, Body, Type, ActionUrl, IsRead, ExpiresAt) "
                + "VALUES (?, ?, ?, ?, ?, ?, 0, ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setNullableInt(ps, 1, n.getCustomerID());
            setNullableInt(ps, 2, n.getStaffID());
            ps.setString(3, n.getTitle());
            ps.setString(4, n.getBody());
            ps.setString(5, n.getType() != null ? n.getType() : Notification.TYPE_INFO);
            ps.setString(7, n.getActionUrl());
            if (n.getExpiresAt() != null)
                ps.setTimestamp(8, Timestamp.valueOf(n.getExpiresAt()));
            else
                ps.setNull(8, Types.TIMESTAMP);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                return k.next() ? k.getInt(1) : -1;
            }
        }
    }

    /** Đánh dấu 1 thông báo đã đọc. */
    public void markRead(int notificationId) throws SQLException {
        String sql = "UPDATE Notifications SET IsRead = 1 WHERE NotificationID = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, notificationId);
            ps.executeUpdate();
        }
    }

    /** Đánh dấu tất cả đã đọc cho 1 customer. */
    public void markAllRead(int customerId) throws SQLException {
        String sql = "UPDATE Notifications SET IsRead = 1 "
                + "WHERE CustomerID = ? AND IsRead = 0";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ps.executeUpdate();
        }
    }

    /** Đánh dấu tất cả theo type đã đọc. */
    public void markTypeRead(int customerId, String type) throws SQLException {
        String sql = "UPDATE Notifications SET IsRead = 1 "
                + "WHERE CustomerID = ? AND Type = ? AND IsRead = 0";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ps.setString(2, type);
            ps.executeUpdate();
        }
    }

    // ── Factory helpers (gọi từ NotificationService) ─────────────────────────

    public int createForCustomer(int customerId, String type, String title,
                                 String body, String actionUrl) throws SQLException {
        Notification n = new Notification();
        n.setCustomerID(customerId);
        n.setType(type);
        n.setTitle(title);
        n.setBody(body);
        n.setActionUrl(actionUrl);
        return create(n);
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    @FunctionalInterface
    interface PsSetter { void set(PreparedStatement ps) throws SQLException; }

    private List<Notification> query(String sql, PsSetter setter) throws SQLException {
        List<Notification> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            setter.set(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    private Notification mapRow(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setNotificationID(rs.getInt("NotificationID"));
        int cid = rs.getInt("CustomerID"); if (!rs.wasNull()) n.setCustomerID(cid);
        int sid = rs.getInt("StaffID");    if (!rs.wasNull()) n.setStaffID(sid);
        n.setTitle(rs.getString("Title"));
        n.setBody(rs.getString("Body"));
        n.setRead(rs.getBoolean("IsRead"));

        // New columns — graceful fallback if column doesn't exist yet
        try { n.setType(rs.getString("Type")); }         catch (Exception e) { n.setType(Notification.TYPE_INFO); }
        try { n.setActionUrl(rs.getString("ActionUrl")); } catch (Exception ignored) {}
        try {
            Timestamp exp = rs.getTimestamp("ExpiresAt");
            if (exp != null) n.setExpiresAt(exp.toLocalDateTime());
        } catch (Exception ignored) {}

        Timestamp ts = rs.getTimestamp("CreatedAt");
        if (ts != null) n.setCreatedAt(ts.toLocalDateTime());
        return n;
    }

    private void setNullableInt(PreparedStatement ps, int idx, Integer val) throws SQLException {
        if (val != null) ps.setInt(idx, val); else ps.setNull(idx, Types.INTEGER);
    }
}
