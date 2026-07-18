package com.petclinic.dao;

import com.petclinic.model.AppointmentService;
import com.petclinic.util.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

/**
 * DAO cho bảng join N-N AppointmentServices (1 appointment có thể có nhiều
 * dịch vụ, mỗi dịch vụ có UnitPrice snapshot riêng và có thể được gán cho
 * 1 nhân viên phụ trách khác nhau qua AssignedStaffID).
 */
public class AppointmentServiceDAO {

    /** Thêm 1 dịch vụ vào appointment. unitPrice được snapshot tại thời điểm đặt lịch. */
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

    /**
     * Tải hàng loạt (tránh N+1) cho nhiều appointment cùng lúc — dùng khi
     * AppointmentDAO trả về danh sách nhiều appointment (vd lịch sử đặt lịch
     * của khách).
     */
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

    /** Gán 1 nhân viên phụ trách cho 1 dòng dịch vụ cụ thể (dùng bởi AssignmentService). */
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
