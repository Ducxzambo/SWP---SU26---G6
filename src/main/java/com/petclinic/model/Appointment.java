package com.petclinic.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class Appointment {
    private int appointmentID;
    private int customerID;
    private int petID;
    private int serviceID;
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

    public List<AppointmentServiceItem> getServices() {
        return services;
    }

    public void setServices(List<AppointmentServiceItem> services) {
        this.services = services;
    }

    private List<AppointmentServiceItem> services = new ArrayList<>();

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

    public String getServiceNamesJoined() {
        if (services == null || services.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < services.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(services.get(i).getServiceName());
        }
        return sb.toString();
    }

    /** Tổng tiền dịch vụ (chưa gồm thuốc) — cộng UnitPrice mọi dòng AppointmentServices. */
    public java.math.BigDecimal getServicesTotalPrice() {
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        if (services != null) {
            for (AppointmentServiceItem s : services) {
                if (s.getUnitPrice() != null) total = total.add(s.getUnitPrice());
            }
        }
        return total;
    }

    /** Tên nhân viên phụ trách các dịch vụ thuộc 1 category, nối dấu phẩy nếu nhiều người khác nhau. */
    public String getStaffNamesByCategory(String categoryName) {
        if (services == null) return "";
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (AppointmentServiceItem s : services) {
            if (categoryName.equals(s.getCategoryName()) && s.getStaffName() != null) {
                names.add(s.getStaffName());
            }
        }
        return String.join(", ", names);
    }

    /** True nếu ÍT NHẤT 1 dịch vụ thuộc category chỉ định CHƯA có staff phụ trách. */
    public boolean hasUnassignedServiceInCategory(String categoryName) {
        if (services == null) return false;
        for (AppointmentServiceItem s : services) {
            if (categoryName.equals(s.getCategoryName()) && s.getAssignedStaffID() == null) return true;
        }
        return false;
    }

    /** Danh sách dịch vụ thuộc 1 category cụ thể trong appointment này. */
    public List<AppointmentServiceItem> getServicesByCategory(String categoryName) {
        List<AppointmentServiceItem> result = new ArrayList<>();
        if (services != null) {
            for (AppointmentServiceItem s : services) {
                if (categoryName.equals(s.getCategoryName())) result.add(s);
            }
        }
        return result;
    }

    /** True nếu appointment có ít nhất 1 dịch vụ thuộc category chỉ định. */
    public boolean hasCategory(String categoryName) {
        if (services == null) return false;
        for (AppointmentServiceItem s : services) {
            if (categoryName.equals(s.getCategoryName())) return true;
        }
        return false;
    }
}