package com.petclinic.dao;

import com.petclinic.util.DBConnection;

import java.sql.*;

/**
 * DAO for dbo.Invoices.
 *
 * Columns (from PetClinicMVP.sql):
 *   InvoiceID      int IDENTITY PK
 *   AppointmentID  int NOT NULL  FK → Appointments
 *   CustomerID     int NOT NULL  FK → Customers
 *   TotalAmount    decimal(18,2) NOT NULL
 *   Status         nvarchar(30)  NOT NULL
 *     → 'Unpaid' | 'Paid' | 'Refunded' | 'PartiallyRefunded'
 *   CreatedAt      datetime2 DEFAULT GETDATE()
 */
public class InvoiceDAO {

    /**
     * Insert a new invoice.
     * @return generated InvoiceID, or -1 on failure
     */
    public int create(int appointmentID, int customerID,
                      double totalAmount, String status) throws SQLException {
        String sql =
            "INSERT INTO Invoices (AppointmentID, CustomerID, TotalAmount, Status) " +
            "VALUES (?, ?, ?, ?)";

        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(
                 sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, appointmentID);
            ps.setInt(2, customerID);
            ps.setDouble(3, totalAmount);
            ps.setString(4, status);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    /**
     * Update invoice status.
     */
    public void updateStatus(int invoiceID, String status) throws SQLException {
        String sql = "UPDATE Invoices SET Status = ? WHERE InvoiceID = ?";
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, invoiceID);
            ps.executeUpdate();
        }
    }
}
