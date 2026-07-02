package com.petclinic.dao;

import com.petclinic.model.Customer;
import com.petclinic.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAO {

    // ── Lookup ───────────────────────────────────────────────────────────────

    public Customer findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM Customers WHERE Email = ? AND IsActive = 1";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public Customer findByPhone(String phone) throws SQLException {
        String sql = "SELECT * FROM Customers WHERE Phone = ? AND IsActive = 1";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, phone.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public Customer findById(int id) throws SQLException {
        String sql = "SELECT * FROM Customers WHERE CustomerID = ? AND IsActive = 1";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public boolean existsByEmail(String email) throws SQLException {
        String sql = "SELECT 1 FROM Customers WHERE Email = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean existsByPhone(String phone) throws SQLException {
        String sql = "SELECT 1 FROM Customers WHERE Phone = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, phone.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // ── Write ────────────────────────────────────────────────────────────────

    public int insert(Customer customer) throws SQLException {
        String sql = "INSERT INTO Customers (FullName, Email, Phone, PasswordHash, IsActive) "
                + "VALUES (?, ?, ?, ?, 1)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, customer.getFullName());
            ps.setString(2, customer.getEmail().trim().toLowerCase());
            ps.setString(3, customer.getPhone() != null ? customer.getPhone().trim() : null);
            ps.setString(4, customer.getPasswordHash());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    /**
     * Quick-create a walk-in customer with ONLY phone + full name (no email/password required).
     * Email is auto-generated as a placeholder so the UNIQUE/NOT NULL constraints still hold;
     * the customer can complete registration with a real email later if they choose to.
     * Returns the generated CustomerID.
     */
    public int insertWalkIn(String fullName, String phone) throws SQLException {
        String placeholderEmail = "walkin_" + phone.trim() + "@petclinic.local";
        String randomHash = com.petclinic.util.PasswordUtil.hashPassword(
                java.util.UUID.randomUUID().toString());

        String sql = "INSERT INTO Customers (FullName, Email, Phone, PasswordHash, IsActive) "
                + "VALUES (?, ?, ?, ?, 1)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, fullName.trim());
            ps.setString(2, placeholderEmail);
            ps.setString(3, phone.trim());
            ps.setString(4, randomHash);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    public void updatePassword(int customerId, String newPasswordHash) throws SQLException {
        String sql = "UPDATE Customers SET PasswordHash = ? WHERE CustomerID = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newPasswordHash);
            ps.setInt(2, customerId);
            ps.executeUpdate();
        }
    }

    // ── Remember-Me token ────────────────────────────────────────────────────

    public void saveRememberMeToken(int customerId, String token, LocalDateTime expiredTime)
            throws SQLException {
        String sql = "UPDATE Customers SET RememberMeToken = ?, TokenExpiredTime = ? "
                + "WHERE CustomerID = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setTimestamp(2, Timestamp.valueOf(expiredTime));
            ps.setInt(3, customerId);
            ps.executeUpdate();
        }
    }

    public Customer findByRememberMeToken(String token) throws SQLException {
        String sql = "SELECT * FROM Customers WHERE RememberMeToken = ? AND IsActive = 1";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public void clearRememberMeToken(int customerId) throws SQLException {
        String sql = "UPDATE Customers SET RememberMeToken = NULL, TokenExpiredTime = NULL "
                + "WHERE CustomerID = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ps.executeUpdate();
        }
    }

    // ── Mapping ──────────────────────────────────────────────────────────────

    private Customer mapRow(ResultSet rs) throws SQLException {
        Customer cust = new Customer();
        cust.setCustomerID(rs.getInt("CustomerID"));
        cust.setFullName(rs.getString("FullName"));
        cust.setEmail(rs.getString("Email"));
        cust.setPhone(rs.getString("Phone"));
        cust.setPasswordHash(rs.getString("PasswordHash"));
        cust.setActive(rs.getBoolean("IsActive"));
        Timestamp ts = rs.getTimestamp("CreatedAt");
        if (ts != null) cust.setCreatedAt(ts.toLocalDateTime());
        cust.setRememberMeToken(rs.getString("RememberMeToken"));
        Timestamp tokenExp = rs.getTimestamp("TokenExpiredTime");
        if (tokenExp != null) cust.setTokenExpiredTime(tokenExp.toLocalDateTime());
        return cust;
    }

    public List<Customer> findAllActive(int max) throws SQLException {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT * FROM Customers WHERE IsActive = 1" +
                " GROUP BY customerID, FullName, Email, Phone, PasswordHash, IsActive, CreatedAt, RememberMeToken, TokenExpiredTime" +
                " HAVING COUNT(CustomerId)<=max";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }
}