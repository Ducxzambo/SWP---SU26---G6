package com.petclinic.backend.dao;

import com.petclinic.backend.model.AppointmentService;
import com.petclinic.backend.util.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class AppointmentServiceDAO {

    public int insert(int appointmentId, int serviceId, BigDecimal unitPrice) throws SQLException {
        String sql = "INSERT INTO AppointmentServices (AppointmentID, ServiceID, UnitPrice) VALUES (?, ?, ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, appointmentId);
            ps.setInt(2, serviceId);
            ps.setBigDecimal(3, unitPrice);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    public Map<Integer, List<AppointmentService>> findByAppointmentIds(List<Integer> appointmentIds) throws SQLException {
        Map<Integer, List<AppointmentService>> result = new LinkedHashMap<>();
        if (appointmentIds == null || appointmentIds.isEmpty()) return result;

        StringBuilder sql = new StringBuilder(
                "SELECT asvc.*, s.Name AS ServiceName, s.CategoryID AS SvcCategoryID, "
                        + "sc.Name AS CategoryName, st.FullName AS StaffName "
                        + "FROM AppointmentServices asvc "
                        + "JOIN Services s ON asvc.ServiceID = s.ServiceID "
                        + "JOIN ServiceCategories sc ON s.CategoryID = sc.CategoryID "
                        + "LEFT JOIN Staff st ON asvc.AssignedStaffID = st.StaffID "
                        + "WHERE asvc.AppointmentID IN (");
        for (int i = 0; i < appointmentIds.size(); i++) {
            sql.append(i == 0 ? "?" : ",?");
        }
        sql.append(") ORDER BY asvc.AppointmentServiceID");

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < appointmentIds.size(); i++) ps.setInt(i + 1, appointmentIds.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AppointmentService item = mapRow(rs);
                    result.computeIfAbsent(item.getAppointmentID(), k -> new ArrayList<>()).add(item);
                }
            }
        }
        return result;
    }

    public void assignStaff(int appointmentServiceId, int staffId) throws SQLException {
        String sql = "UPDATE AppointmentServices SET AssignedStaffID = ? WHERE AppointmentServiceID = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, staffId);
            ps.setInt(2, appointmentServiceId);
            ps.executeUpdate();
        }
    }

    private AppointmentService mapRow(ResultSet rs) throws SQLException {
        AppointmentService item = new AppointmentService();
        item.setAppointmentServiceID(rs.getInt("AppointmentServiceID"));
        item.setAppointmentID(rs.getInt("AppointmentID"));
        item.setServiceID(rs.getInt("ServiceID"));
        item.setUnitPrice(rs.getBigDecimal("UnitPrice"));
        int staffId = rs.getInt("AssignedStaffID");
        if (!rs.wasNull()) item.setAssignedStaffID(staffId);
        item.setServiceName(rs.getString("ServiceName"));
        item.setCategoryID(rs.getInt("SvcCategoryID"));
        item.setCategoryName(rs.getString("CategoryName"));
        item.setStaffName(rs.getString("StaffName"));
        return item;
    }
}
