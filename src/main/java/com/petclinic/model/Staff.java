package com.petclinic.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Staff {
    private int     staffID;
    private String  fullName;
    private String email;
    private String phone;
    private String passwordHash;
    private int     roleID;
    private String specialization;
    private String licenseNumber;
    private LocalDate hireDate;
    private boolean active;
    private LocalDateTime createdAt;

    private String roleName;

    public Staff() {}

    public int     getStaffID()        { return staffID; }
    public void    setStaffID(int v)   { staffID = v; }
    public String  getFullName()       { return fullName; }
    public void    setFullName(String v){ fullName = v; }
    public String  getEmail()       { return email; }
    public void    setEmail(String v){ email = v; }
    public String  getPhone()       { return phone; }
    public void    setPhone(String v){ phone = v; }
    public String  getPasswordHash()       { return passwordHash; }
    public void    setPasswordHash(String v){ passwordHash = v; }
    public int     getRoleID()         { return roleID; }
    public void    setRoleID(int v)    { roleID = v; }
    public String  getSpecialization()  { return specialization; }
    public void    setSpecialization(String v){ specialization = v; }
    public String  getLicenseNumber()  { return licenseNumber; }
    public void    setLicenseNumber(String v){ licenseNumber = v; }
    public LocalDate getHireDate()    { return hireDate; }
    public void    setHireDate(LocalDate v)   { hireDate = v; }
    public boolean isActive()          { return active; }
    public void    setActive(boolean v){ active = v; }
    public LocalDateTime getCreatedAt()      { return createdAt; }
    public void    setCreatedAt(LocalDateTime v)    { createdAt = v; }
    public String  getRoleName()      { return roleName; }
    public void    setRoleName(String v)    { roleName = v; }
}