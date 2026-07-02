package com.petclinic.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class Appointment {
    private int appointmentID;
    private int customerID;
    private int petID;
    private int serviceID;
    private Integer assignedStaffID;
    private LocalDate appointmentDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
    private Integer slotShift; // 1-4
    private String    notes;
    private String    cancelReason;

    // join fields
    private String customerName;
    private String petName;
    private String serviceName;
    private String    categoryName;
    private String    staffName;
    private Integer recordID; // RecordID của MedicalRecord/GroomingRecord nếu status = Done (nullable)

    public Appointment() {}

    public int       getAppointmentID()          { return appointmentID; }
    public void      setAppointmentID(int v)      { appointmentID = v; }
    public int       getCustomerID()             { return customerID; }
    public void      setCustomerID(int v)         { customerID = v; }
    public int       getPetID()                  { return petID; }
    public void      setPetID(int v)              { petID = v; }
    public int       getServiceID()              { return serviceID; }
    public void      setServiceID(int v)          { serviceID = v; }
    public Integer   getAssignedStaffID()          { return assignedStaffID; }
    public void      setAssignedStaffID(Integer v)  { assignedStaffID = v; }
    public LocalDate getAppointmentDate()        { return appointmentDate; }
    public void      setAppointmentDate(LocalDate v) { appointmentDate = v; }
    public LocalTime getStartTime()              { return startTime; }
    public void      setStartTime(LocalTime v)   { startTime = v; }
    public LocalTime getEndTime()                { return endTime; }
    public void      setEndTime(LocalTime v)     { endTime = v; }
    public String    getStatus()                 { return status; }
    public void      setStatus(String v)          { status = v; }
    public Integer   getSlotShift()              { return slotShift; }
    public void      setSlotShift(Integer v)     { slotShift = v; }
    public String    getNotes()                 { return notes; }
    public void      setNotes(String v)         { notes = v; }
    public String    getCancelReason()          { return cancelReason; }
    public void      setCancelReason(String v)  { cancelReason = v; }

    // join fields
    public String getCustomerName()              { return customerName; }
    public void   setCustomerName(String v)      { customerName = v; }
    public String getPetName()                   { return petName; }
    public void   setPetName(String v)           { petName = v; }
    public String getServiceName()               { return serviceName; }
    public void   setServiceName(String v)       { serviceName = v; }
    public Integer getRecordID()                 { return recordID; }
    public void    setRecordID(Integer v)        { recordID = v; }
    public String    getCategoryName()          { return categoryName; }
    public void      setCategoryName(String v)  { categoryName = v; }
    public String    getStaffName()               { return staffName; }
    public void      setStaffName(String v)       { staffName = v; }

    /**
     * Deadline chung cho cả đổi lịch (reschedule) và huỷ lịch (cancel):
     * 22:00 ngày trước AppointmentDate. Áp dụng như nhau cho cả Pending
     * và Confirmed. Trả về null nếu status khác (không cho chỉnh sửa)
     * hoặc thiếu AppointmentDate.
     */
    public LocalDateTime getModifyDeadline() {
        if (appointmentDate == null) return null;
        if (!"Pending".equals(status) && !"Confirmed".equals(status)) return null;
        return LocalDateTime.of(appointmentDate.minusDays(1), LocalTime.of(22, 0));
    }

    /**
     * Có thể đổi lịch không. Lưu ý: sau khi đổi lịch, Status được GIỮ
     * NGUYÊN (không reset về Pending) — xem AppointmentDAO.updateSlot().
     */
    public boolean canReschedule() {
        LocalDateTime deadline = getModifyDeadline();
        return deadline != null && LocalDateTime.now().isBefore(deadline);
    }

    /**
     * Có thể huỷ lịch không — dùng cùng deadline 22:00 ngày trước với reschedule.
     */
    public boolean canCancel() {
        return canReschedule();
    }

    /**
     * Còn dùng cho UI tổng quát (badge "Có thể chỉnh sửa"): reschedule và
     * cancel nay dùng chung điều kiện nên canModify() == canReschedule().
     */
    public boolean canModify() {
        return canReschedule();
    }

    /**
     * Appointment đã kết thúc (dùng để ẩn/hiện các section bệnh án, hoá đơn).
     */
    public boolean isCompleted() {
        return "Done".equals(status) || "NoShow".equals(status)
                || "Cancelled".equals(status);
    }

    /**
     * Appointment đang active (Pending/Confirmed/InProgress).
     */
    public boolean isActive() {
        return "Pending".equals(status) || "Confirmed".equals(status)
                || "InProgress".equals(status);
    }
}