package com.petclinic.dao;

import com.petclinic.model.User;
import com.petclinic.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * JDBC DAO cho bảng Users (+ join Roles).
 * Mọi method đều tự mở / đóng connection.
 * Production: nên dùng connection pool (HikariCP).
 */
public class UserDAO {

    // ── Mapper ────────────────────────────────────────────────────────────────

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("UserID"));
        u.setFullName(rs.getString("FullName"));
        u.setEmail(rs.getString("Email"));
        u.setPhone(rs.getString("Phone"));
        u.setPasswordHash(rs.getString("PasswordHash"));
        u.setAvatarURL(rs.getString("AvatarURL"));
        u.setRoleId(rs.getInt("RoleID"));
        u.setRoleName(rs.getString("RoleName"));
        u.setActive(rs.getBoolean("IsActive"));
        Timestamp ca = rs.getTimestamp("CreatedAt");
        if (ca != null) u.setCreatedAt(ca.toLocalDateTime());
        return u;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /** Tìm user theo email (dùng cho login). */
    public Optional<User> findByEmail(String email) {
        String sql = """
            SELECT u.*, r.RoleName
            FROM Users u
            JOIN Roles r ON u.RoleID = r.RoleID
            WHERE u.Email = ?
            """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByEmail failed", e);
        }
        return Optional.empty();
    }

    /** Kiểm tra email đã tồn tại chưa (dùng cho register). */
    public boolean existsByEmail(String email) {
        String sql = "SELECT 1 FROM Users WHERE Email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("existsByEmail failed", e);
        }
    }

    /** Tạo user mới, trả về UserID được generate. */
    public int insert(User user) {
        String sql = """
            INSERT INTO Users (FullName, Email, Phone, PasswordHash, RoleID, IsActive)
            VALUES (?, ?, ?, ?, ?, 1)
            """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPhone());
            ps.setString(4, user.getPasswordHash());
            // Mặc định role = Customer (RoleID = 5 theo seed data)
            ps.setInt(5, user.getRoleId() > 0 ? user.getRoleId() : 5);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("insert user failed", e);
        }
        return -1;
    }

    // ── Reset Password ────────────────────────────────────────────────────────

    /**
     * Lưu reset token + expiry vào cột mở rộng.
     * NOTE: DB hiện tại chưa có cột này — cần thêm migration:
     *   ALTER TABLE Users ADD ResetToken NVARCHAR(100), ResetTokenExpiry DATETIME2;
     */
    public void saveResetToken(int userId, String token, LocalDateTime expiry) {
        String sql = "UPDATE Users SET ResetToken = ?, ResetTokenExpiry = ?, UpdatedAt = GETDATE() WHERE UserID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setTimestamp(2, Timestamp.valueOf(expiry));
            ps.setInt(3, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("saveResetToken failed", e);
        }
    }

    /** Tìm user theo reset token (chưa hết hạn). */
    public Optional<User> findByValidResetToken(String token) {
        String sql = """
            SELECT u.*, r.RoleName
            FROM Users u
            JOIN Roles r ON u.RoleID = r.RoleID
            WHERE u.ResetToken = ? AND u.ResetTokenExpiry > GETDATE()
            """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByValidResetToken failed", e);
        }
        return Optional.empty();
    }

    /** Cập nhật mật khẩu mới và xóa token. */
    public void updatePassword(int userId, String newPasswordHash) {
        String sql = """
            UPDATE Users
            SET PasswordHash = ?, ResetToken = NULL, ResetTokenExpiry = NULL, UpdatedAt = GETDATE()
            WHERE UserID = ?
            """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPasswordHash);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("updatePassword failed", e);
        }
    }
}
