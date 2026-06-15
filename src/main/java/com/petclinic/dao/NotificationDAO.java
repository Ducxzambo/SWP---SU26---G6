package com.petclinic.dao;

import com.petclinic.model.Notification;
import com.petclinic.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAO {

    public List<Notification> findByCustomer(int customerId) throws SQLException {
        String sql = "SELECT * FROM Notifications WHERE CustomerID = ? "
                   + "ORDER BY CreatedAt DESC";
        List<Notification> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public int countUnread(int customerId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Notifications WHERE CustomerID = ? AND IsRead = 0";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public void markAllRead(int customerId) throws SQLException {
        String sql = "UPDATE Notifications SET IsRead = 1 WHERE CustomerID = ? AND IsRead = 0";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ps.executeUpdate();
        }
    }

    private Notification mapRow(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setNotificationID(rs.getInt("NotificationID"));
        int cid = rs.getInt("CustomerID"); if (!rs.wasNull()) n.setCustomerID(cid);
        int sid = rs.getInt("StaffID");    if (!rs.wasNull()) n.setStaffID(sid);
        n.setTitle(rs.getString("Title"));
        n.setBody(rs.getString("Body"));
        n.setRead(rs.getBoolean("IsRead"));
        Timestamp ts = rs.getTimestamp("CreatedAt");
        if (ts != null) n.setCreatedAt(ts.toLocalDateTime());
        return n;
    }
}
