package com.petclinic.dao;

import com.petclinic.model.Invoice;
import com.petclinic.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InvoiceDAO {

    public Invoice findByAppointment(int appointmentId) throws SQLException {
        String sql = "SELECT * FROM Invoices WHERE AppointmentID = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, appointmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Invoice inv = mapInvoice(rs);
                inv.setItems(findItems(inv.getInvoiceID()));
                inv.setPayments(findPayments(inv.getInvoiceID()));
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
                inv.setItems(findItems(inv.getInvoiceID()));
                inv.setPayments(findPayments(inv.getInvoiceID()));
                return inv;
            }
        }
    }

    private List<Invoice.InvoiceItem> findItems(int invoiceId) throws SQLException {
        String sql = "SELECT * FROM InvoiceItems WHERE InvoiceID = ?";
        List<Invoice.InvoiceItem> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Invoice.InvoiceItem item = new Invoice.InvoiceItem();
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

    private List<Invoice.Payment> findPayments(int invoiceId) throws SQLException {
        String sql = "SELECT p.*, s.FullName AS ProcessedByName "
                + "FROM Payments p "
                + "JOIN Staff s ON p.ProcessedByID = s.StaffID "
                + "WHERE p.InvoiceID = ? ORDER BY p.PaidAt DESC";
        List<Invoice.Payment> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Invoice.Payment pay = new Invoice.Payment();
                    pay.setPaymentID(rs.getInt("PaymentID"));
                    pay.setInvoiceID(rs.getInt("InvoiceID"));
                    pay.setAmount(rs.getBigDecimal("Amount"));
                    pay.setMethod(rs.getString("Method"));
                    Timestamp ts = rs.getTimestamp("PaidAt");
                    if (ts != null) pay.setPaidAt(ts.toLocalDateTime());
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
        inv.setStatus(rs.getString("Status"));
        return inv;
    }
}