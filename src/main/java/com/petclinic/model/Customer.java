package com.petclinic.model;

import java.time.LocalDateTime;

public class Customer {
    private int           customerID;
    private String        fullName;
    private String        email;
    private String        phone;
    private String        passwordHash;
    private boolean       isActive;
    private LocalDateTime createdAt;
    private String        rememberMeToken;
    private LocalDateTime tokenExpiredTime;

    // ── Constructors ─────────────────────────────────────────────────────────
    public Customer() {}

    public Customer(String fullName, String email, String phone, String passwordHash) {
        this.fullName     = fullName;
        this.email        = email;
        this.phone        = phone;
        this.passwordHash = passwordHash;
        this.isActive     = true;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────
    public int           getCustomerID()   { return customerID; }
    public void          setCustomerID(int v) { customerID = v; }
    public String        getFullName()     { return fullName; }
    public void          setFullName(String v) { fullName = v; }
    public String        getEmail()        { return email; }
    public void          setEmail(String v) { email = v; }
    public String        getPhone()        { return phone; }
    public void          setPhone(String v) { phone = v; }
    public String        getPasswordHash() { return passwordHash; }
    public void          setPasswordHash(String v) { passwordHash = v; }
    public boolean       isActive()        { return isActive; }
    public void          setActive(boolean v) { isActive = v; }
    public LocalDateTime getCreatedAt()    { return createdAt; }
    public void          setCreatedAt(LocalDateTime v) { createdAt = v; }
    public String        getRememberMeToken()           { return rememberMeToken; }
    public void          setRememberMeToken(String v)   { rememberMeToken = v; }
    public LocalDateTime getTokenExpiredTime()          { return tokenExpiredTime; }
    public void          setTokenExpiredTime(LocalDateTime v) { tokenExpiredTime = v; }
}
