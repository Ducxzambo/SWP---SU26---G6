package com.petclinic.backend.service;

import com.petclinic.backend.dao.StaffDAO;
import com.petclinic.backend.dao.StaffStatsDAO;
import com.petclinic.backend.model.Role;
import com.petclinic.backend.model.Staff;
import com.petclinic.backend.dto.StaffPerformance;
import com.petclinic.backend.dto.StaffServiceBreakdown;
import com.petclinic.backend.util.PasswordUtil;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

public class StaffService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final StaffDAO staffDAO = new StaffDAO();
    private final StaffStatsDAO staffStatsDAO = new StaffStatsDAO();

     public List<Staff> search(String keyword, String roleName, String statusFilter) throws SQLException {
        return staffDAO.search(keyword, roleName, statusFilter);
    }

    public Staff getForManagement(int staffID) throws SQLException {
        return staffDAO.findByIdForManagement(staffID);
    }

    public List<Role> getAllRoles() throws SQLException {
        return staffDAO.findAllRoles();
    }


    public List<StaffPerformance> getPerformance(LocalDate fromDate, LocalDate toDate,
                                                 String roleName) throws SQLException {
        return staffStatsDAO.getPerformance(fromDate, toDate, roleName, null);
    }

    public StaffPerformance getPerformanceForStaff(int staffID) throws SQLException {
        List<StaffPerformance> rows = staffStatsDAO.getPerformance(null, null, null, staffID);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<StaffServiceBreakdown> getServiceBreakdown(int staffID) throws SQLException {
        return staffStatsDAO.getServiceBreakdown(staffID, null, null);
    }


    public int createStaff(Staff staff, String rawPassword) throws SQLException {
        validateProfile(staff, null);
        if (!PasswordUtil.isStrongPassword(rawPassword)) {
            throw new IllegalArgumentException(
                    "Mật khẩu phải có ít nhất 6 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt.");
        }
        staff.setPasswordHash(PasswordUtil.hashPassword(rawPassword));
        staff.setActive(true);
        if (staff.getHireDate() == null) {
            staff.setHireDate(LocalDate.now());
        }
        return staffDAO.insert(staff);
    }

    public void updateStaff(Staff staff) throws SQLException {
        validateProfile(staff, staff.getStaffID());
        staffDAO.update(staff);
    }

    public void resetPassword(int staffID, String newPassword, String confirmPassword)
            throws SQLException {
        if (newPassword == null || !newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp.");
        }
        if (!PasswordUtil.isStrongPassword(newPassword)) {
            throw new IllegalArgumentException(
                    "Mật khẩu phải có ít nhất 6 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt.");
        }
        staffDAO.updatePassword(staffID, PasswordUtil.hashPassword(newPassword));
    }


    public void setActive(int staffID, boolean active, int actingStaffID) throws SQLException {
        if (!active && staffID == actingStaffID) {
            throw new IllegalArgumentException("Bạn không thể tự vô hiệu hoá tài khoản của chính mình.");
        }
        staffDAO.setActive(staffID, active);
    }


    private void validateProfile(Staff staff, Integer existingStaffID) throws SQLException {
        if (staff.getFullName() == null || staff.getFullName().isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập họ tên.");
        }
        if (staff.getEmail() == null || !EMAIL_PATTERN.matcher(staff.getEmail().trim()).matches()) {
            throw new IllegalArgumentException("Email không hợp lệ.");
        }
        if (staffDAO.emailExists(staff.getEmail(), existingStaffID)) {
            throw new IllegalArgumentException("Email này đã được dùng cho một nhân viên khác.");
        }
        if (staff.getRoleID() <= 0) {
            throw new IllegalArgumentException("Vui lòng chọn vai trò.");
        }
    }
}
