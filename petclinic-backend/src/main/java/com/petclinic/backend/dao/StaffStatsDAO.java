package com.petclinic.backend.dao;

import com.petclinic.backend.dto.StaffPerformance;
import com.petclinic.backend.dto.StaffServiceBreakdown;
import com.petclinic.backend.util.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class StaffStatsDAO {

    public List<StaffPerformance> getPerformance(LocalDate fromDate, LocalDate toDate,
                                                 String roleName, Integer staffID)
            throws SQLException {
        StringBuilder joinCondition = new StringBuilder(" a.AppointmentID = aps.AppointmentID");
        List<Object> joinParams = new ArrayList<>();
        if (fromDate != null) {
            joinCondition.append(" AND a.AppointmentDate >= ?");
            joinParams.add(Date.valueOf(fromDate));
        }
        if (toDate != null) {
            joinCondition.append(" AND a.AppointmentDate <= ?");
            joinParams.add(Date.valueOf(toDate));
        }

        StringBuilder sql = new StringBuilder("""
                SELECT s.StaffID,
                       s.FullName,
                       r.RoleName,
                       SUM(CASE WHEN a.Status = 'Done' THEN 1 ELSE 0 END) AS CompletedCases,
                       SUM(CASE WHEN a.Status = 'Cancelled' THEN 1 ELSE 0 END) AS CancelledCases,
                       SUM(CASE WHEN a.Status = 'NoShow' THEN 1 ELSE 0 END) AS NoShowCases,
                       SUM(CASE WHEN a.Status = 'Done' THEN aps.UnitPrice ELSE 0 END) AS Revenue
                FROM Staff s
                JOIN Roles r ON r.RoleID = s.RoleID
                LEFT JOIN AppointmentServices aps ON aps.AssignedStaffID = s.StaffID
                LEFT JOIN Appointments a ON""").append(" ").append(joinCondition).append(" ").append("""
        WHERE 1 = 1
        """);
        List<Object> params = new ArrayList<>(joinParams);

        if (staffID != null) {
            sql.append(" AND s.StaffID = ?");
            params.add(staffID);
        } else {
            sql.append(" AND s.IsActive = 1");
        }
        if (roleName != null && !roleName.isBlank()) {
            sql.append(" AND r.RoleName = ?");
            params.add(roleName.trim());
        }
        sql.append("""
                 GROUP BY s.StaffID, s.FullName, r.RoleName
                 ORDER BY CompletedCases DESC, s.FullName
                """);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<StaffPerformance> list = new ArrayList<>();
                while (rs.next()) {
                    StaffPerformance p = new StaffPerformance();
                    p.setStaffID(rs.getInt("StaffID"));
                    p.setFullName(rs.getString("FullName"));
                    p.setRoleName(rs.getString("RoleName"));
                    p.setCompletedCases(rs.getInt("CompletedCases"));
                    p.setCancelledCases(rs.getInt("CancelledCases"));
                    p.setNoShowCases(rs.getInt("NoShowCases"));
                    BigDecimal revenue = rs.getBigDecimal("Revenue");
                    p.setRevenue(revenue == null ? BigDecimal.ZERO : revenue);
                    list.add(p);
                }
                return list;
            }
        }
    }

    public List<StaffServiceBreakdown> getServiceBreakdown(int staffID, LocalDate fromDate,
                                                            LocalDate toDate) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT sv.Name AS ServiceName,
                       COUNT(*) AS CompletedCount,
                       SUM(aps.UnitPrice) AS Revenue
                FROM AppointmentServices aps
                JOIN Appointments a ON a.AppointmentID = aps.AppointmentID
                JOIN Services sv ON sv.ServiceID = aps.ServiceID
                WHERE aps.AssignedStaffID = ?
                  AND a.Status = 'Done'
                """);
        List<Object> params = new ArrayList<>();
        params.add(staffID);
        if (fromDate != null) {
            sql.append(" AND a.AppointmentDate >= ?");
            params.add(Date.valueOf(fromDate));
        }
        if (toDate != null) {
            sql.append(" AND a.AppointmentDate <= ?");
            params.add(Date.valueOf(toDate));
        }
        sql.append(" GROUP BY sv.Name ORDER BY CompletedCount DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<StaffServiceBreakdown> list = new ArrayList<>();
                while (rs.next()) {
                    StaffServiceBreakdown b = new StaffServiceBreakdown();
                    b.setServiceName(rs.getString("ServiceName"));
                    b.setCompletedCount(rs.getInt("CompletedCount"));
                    BigDecimal revenue = rs.getBigDecimal("Revenue");
                    b.setRevenue(revenue == null ? BigDecimal.ZERO : revenue);
                    list.add(b);
                }
                return list;
            }
        }
    }

    private void bindParams(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object value = params.get(i);
            if (value instanceof Integer) ps.setInt(i + 1, (Integer) value);
            else if (value instanceof Date) ps.setDate(i + 1, (Date) value);
            else ps.setObject(i + 1, value);
        }
    }
}
