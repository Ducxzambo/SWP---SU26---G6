package com.petclinic.backend.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Review {
    private int           reviewID;
    private int           appointmentID;
    private int           customerID;
    private int           rating;          // 1–5
    private String        comment;
    private boolean       isPublic;
    private LocalDateTime createdAt;

    private String  serviceName;
    private String  categoryName;
    private String  staffName;
    private Integer staffID;
    private String  petSpecies;
    private String  formattedDate;

    private String  anonymousLabel;       // "Khách hàng ẩn danh"

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    public Review() {}

    // ── Getters / Setters ─────────────────────────────────────────────────────
    public int     getReviewID()              { return reviewID; }
    public void    setReviewID(int v)         { reviewID = v; }
    public int     getAppointmentID()         { return appointmentID; }
    public void    setAppointmentID(int v)    { appointmentID = v; }
    public int     getCustomerID()            { return customerID; }
    public void    setCustomerID(int v)       { customerID = v; }
    public int     getRating()                { return rating; }
    public void    setRating(int v)           { rating = v; }
    public String  getComment()               { return comment; }
    public void    setComment(String v)       { comment = v; }
    public boolean getIsPublic()                 { return isPublic; }
    public void    setIsPublic(boolean v)       { isPublic = v; }
    public LocalDateTime getCreatedAt()       { return createdAt; }
    public void    setCreatedAt(LocalDateTime v){ createdAt = v; }
    public String  getServiceName()           { return serviceName; }
    public void    setServiceName(String v)   { serviceName = v; }
    public String  getCategoryName()          { return categoryName; }
    public void    setCategoryName(String v)  { categoryName = v; }
    public String  getStaffName()               { return staffName; }
    public void    setStaffName(String v)       { staffName = v; }
    public Integer getStaffID()                 { return staffID; }
    public void    setStaffID(Integer v)        { staffID = v; }
    public String  getPetSpecies()            { return petSpecies; }
    public void    setPetSpecies(String v)    { petSpecies = v; }
    public String  getAnonymousLabel()        { return anonymousLabel; }
    public void    setAnonymousLabel(String v){ anonymousLabel = v; }

    public String getFormattedCreatedAt() {
        return createdAt != null ? createdAt.format(FMT) : "";
    }

    public String getStarsDisplay() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) sb.append(i <= rating ? "★" : "☆");
        return sb.toString();
    }
}