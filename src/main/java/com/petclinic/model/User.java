package com.petclinic.model;

import java.time.LocalDateTime;

public class User {
    private int userId;
    private String fullName;
    private String email;
    private String phone;
    private String passwordHash;
    private String avatarURL;
    private int roleId;
    private String roleName;      // joined from Roles
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // reset password
    private String resetToken;
    private LocalDateTime resetTokenExpiry;

    public User() {}

    // ---- Getters & Setters ----
    public int getUserId()                        { return userId; }
    public void setUserId(int userId)             { this.userId = userId; }

    public String getFullName()                   { return fullName; }
    public void setFullName(String fullName)      { this.fullName = fullName; }

    public String getEmail()                      { return email; }
    public void setEmail(String email)            { this.email = email; }

    public String getPhone()                      { return phone; }
    public void setPhone(String phone)            { this.phone = phone; }

    public String getPasswordHash()               { return passwordHash; }
    public void setPasswordHash(String ph)        { this.passwordHash = ph; }

    public String getAvatarURL()                  { return avatarURL; }
    public void setAvatarURL(String url)          { this.avatarURL = url; }

    public int getRoleId()                        { return roleId; }
    public void setRoleId(int roleId)             { this.roleId = roleId; }

    public String getRoleName()                   { return roleName; }
    public void setRoleName(String roleName)      { this.roleName = roleName; }

    public boolean isActive()                     { return isActive; }
    public void setActive(boolean active)         { isActive = active; }

    public LocalDateTime getCreatedAt()           { return createdAt; }
    public void setCreatedAt(LocalDateTime t)     { this.createdAt = t; }

    public LocalDateTime getUpdatedAt()           { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t)     { this.updatedAt = t; }

    public String getResetToken()                 { return resetToken; }
    public void setResetToken(String t)           { this.resetToken = t; }

    public LocalDateTime getResetTokenExpiry()    { return resetTokenExpiry; }
    public void setResetTokenExpiry(LocalDateTime t) { this.resetTokenExpiry = t; }
}
