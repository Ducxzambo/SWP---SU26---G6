package com.petclinic.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * -- Type enum values:
 * -- REMINDER_48H, REMINDER_18H, BOOKING_CONFIRMED, BOOKING_CANCELLED,
 * -- PAYMENT_SUCCESS, PAYMENT_PENDING, EXAM_RESULT, VACCINE_DUE,
 * -- CARE_TIP, SUPPORT, INFO
 */
public class Notification {

    // Notification types (maps to DB column Type)
    public static final String TYPE_REMINDER_48H      = "REMINDER_48H";
    public static final String TYPE_REMINDER_18H      = "REMINDER_18H";
    public static final String TYPE_BOOKING_CONFIRMED = "BOOKING_CONFIRMED";
    public static final String TYPE_BOOKING_CANCELLED = "BOOKING_CANCELLED";
    public static final String TYPE_PAYMENT_SUCCESS   = "PAYMENT_SUCCESS";
    public static final String TYPE_PAYMENT_PENDING   = "PAYMENT_PENDING";
    public static final String TYPE_EXAM_RESULT       = "EXAM_RESULT";
    public static final String TYPE_VACCINE_DUE       = "VACCINE_DUE";
    public static final String TYPE_CARE_TIP          = "CARE_TIP";
    public static final String TYPE_SUPPORT           = "SUPPORT";
    public static final String TYPE_INFO              = "INFO";

    private int           notificationID;
    private Integer       customerID;
    private Integer       staffID;
    private String        title;
    private String        body;
    private String        type;        // one of TYPE_* constants above
    private String        actionUrl;   // optional deep-link (e.g. /appointments/detail?id=5)
    private boolean       isRead;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;    // null --> never expire

    public Notification() {}

    // ── Getters / Setters ─────────────────────────────────────────────────────
    public int           getNotificationID()       { return notificationID; }
    public void          setNotificationID(int v)  { notificationID = v; }
    public Integer       getCustomerID()           { return customerID; }
    public void          setCustomerID(Integer v)  { customerID = v; }
    public Integer       getStaffID()              { return staffID; }
    public void          setStaffID(Integer v)     { staffID = v; }
    public String        getTitle()                { return title; }
    public void          setTitle(String v)        { title = v; }
    public String        getBody()                 { return body; }
    public void          setBody(String v)         { body = v; }
    public String        getType()                 { return type; }
    public void          setType(String v)         { type = v; }
    public String        getActionUrl()            { return actionUrl; }
    public void          setActionUrl(String v)    { actionUrl = v; }
    public boolean       isRead()                  { return isRead; }
    public void          setRead(boolean v)        { isRead = v; }
    public LocalDateTime getCreatedAt()            { return createdAt; }
    public void          setCreatedAt(LocalDateTime v){ createdAt = v; }
    public LocalDateTime getExpiresAt()            { return expiresAt; }
    public void          setExpiresAt(LocalDateTime v){ expiresAt = v; }

    // ── Display helpers ───────────────────────────────────────────────────────


    /** CSS class cho màu accent border-left theo loại. */
    public String getTypeColor() {
        if (type == null) return "notif-info";
        switch (type) {
            case TYPE_REMINDER_48H:
            case TYPE_REMINDER_18H:      return "notif-reminder";
            case TYPE_BOOKING_CONFIRMED: return "notif-success";
            case TYPE_BOOKING_CANCELLED: return "notif-danger";
            case TYPE_PAYMENT_SUCCESS:   return "notif-success";
            case TYPE_PAYMENT_PENDING:   return "notif-warning";
            case TYPE_EXAM_RESULT:       return "notif-medical";
            case TYPE_VACCINE_DUE:       return "notif-warning";
            case TYPE_CARE_TIP:
            case TYPE_SUPPORT:           return "notif-tip";
            default:                     return "notif-info";
        }
    }

    /** Category label hiển thị trên UI filter. */
    public String getCategoryLabel() {
        if (type == null) return "Thông báo";
        switch (type) {
            case TYPE_REMINDER_48H:
            case TYPE_REMINDER_18H:      return "Nhắc lịch";
            case TYPE_BOOKING_CONFIRMED:
            case TYPE_BOOKING_CANCELLED: return "Lịch hẹn";
            case TYPE_PAYMENT_SUCCESS:
            case TYPE_PAYMENT_PENDING:   return "Thanh toán";
            case TYPE_EXAM_RESULT:       return "Kết quả khám";
            case TYPE_VACCINE_DUE:       return "Vaccine";
            case TYPE_CARE_TIP:          return "Chăm sóc";
            case TYPE_SUPPORT:           return "Hỗ trợ";
            default:                     return "Thông báo";
        }
    }

    /** Relative time: "5 phút trước", "2 giờ trước", "3 ngày trước". */
    public String getRelativeTime() {
        if (createdAt == null) return "";
        long minutes = ChronoUnit.MINUTES.between(createdAt, LocalDateTime.now());
        if (minutes < 1)   return "Vừa xong";
        if (minutes < 60)  return minutes + " phút trước";
        long hours = ChronoUnit.HOURS.between(createdAt, LocalDateTime.now());
        if (hours  < 24)   return hours + " giờ trước";
        long days  = ChronoUnit.DAYS.between(createdAt, LocalDateTime.now());
        if (days   <  7)   return days + " ngày trước";
        return createdAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    public String getFormattedCreatedAt() {
        if (createdAt == null) return "";
        return createdAt.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}
