package com.petclinic.dao;

import com.petclinic.util.DBConnection;

import java.sql.*;

/**
 * DAO for dbo.Notifications.
 *
 * Columns (from PetClinicMVP.sql):
 *   NotificationID  int IDENTITY PK
 *   CustomerID      int NULL  FK → Customers
 *   StaffID         int NULL  FK → Staff
 *   Title           nvarchar(200) NOT NULL
 *   Body            nvarchar(1000) NOT NULL
 *   IsRead          bit DEFAULT 0
 *   CreatedAt       datetime2 DEFAULT GETDATE()
 */
public class NotificationDAO {

    /**
     * Create notification for a Customer.
     * StaffID = NULL.
     */
    public void createForCustomer(int customerID,
                                   String title,
                                   String body) throws SQLException {
        String sql =
            "INSERT INTO Notifications (CustomerID, StaffID, Title, Body, IsRead) " +
            "VALUES (?, NULL, ?, ?, 0)";

        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, customerID);
            ps.setString(2, title);
            ps.setString(3, body);
            ps.executeUpdate();
        }
    }

    /**
     * Create notification for a Staff member.
     * CustomerID = NULL.
     */
    public void createForStaff(int staffID,
                                String title,
                                String body) throws SQLException {
        String sql =
            "INSERT INTO Notifications (CustomerID, StaffID, Title, Body, IsRead) " +
            "VALUES (NULL, ?, ?, ?, 0)";

        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, staffID);
            ps.setString(2, title);
            ps.setString(3, body);
            ps.executeUpdate();
        }
    }
}
