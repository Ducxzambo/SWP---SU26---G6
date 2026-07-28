package com.petclinic.backend.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import com.petclinic.backend.model.AppointmentServiceItem;
import com.petclinic.backend.model.AppointmentService;

/**
 * 1 appointment = 1 customer + 1 slot, co the gan NHIEU dich vu
 * qua bang join AppointmentServices (N-N).
 *
 * petID CO THE NULL
 *
 * serviceName / categoryName / staffName ben duoi la cac truong HIEN THI
 * tong hop (aggregate), duoc AppointmentDAO tinh tu danh sach services.
 */
public class Appointment {
    private int       appointmentID;
    private int       customerID;
    private Integer   petID;
    private LocalDate appointmentDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String    status;
    private String    notes;
    private String    cancelReason;
    private Integer   slotShift; // 1-4 = 4 ca chinh co dinh (FIXED_SLOTS), 5 = ca phu OT (18:30-07:00, chi staff tao)

    // Chi tiet dich vu da chon (N-N)
    private List<AppointmentService> services = new ArrayList<>();

    // join fields
    private String customerName;
    private String staffName;
    private String petName;
    private String serviceName;
    private String categoryName;
    private Integer recordID; // RecordID của MedicalRecord/GroomingRecord nếu status = Done (nullable)

    private String           petSpeciesName;
    private String           petBreedName;
    private String           petGender;
    private java.math.BigDecimal petWeight;

    public Appointment() {}
    // format date to VN
    public String getMonthDisplayVi() {
        if (this.appointmentDate == null) return "";
        return this.appointmentDate.getMonth().getDisplayName(TextStyle.SHORT, new Locale("vi"));
    }

    public String getFormattedAppointmentDate() {
        if (this.appointmentDate == null) return "";
        return this.appointmentDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    // Them ham getter nay cho startTime
    public String getFormattedStartTime() {
        if (this.startTime == null) return "";
        return this.startTime.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    // Them ham getter nay cho endTime
    public String getFormattedEndTime() {
        if (this.endTime == null) return "";
        return this.endTime.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    public int       getAppointmentID()         { return appointmentID; }
    public void      setAppointmentID(int v)    { appointmentID = v; }
    public int       getCustomerID()            { return customerID; }
    public void      setCustomerID(int v)       { customerID = v; }
    public Integer   getPetID()                 { return petID; }
    public void      setPetID(Integer v)        { petID = v; }
    public LocalDate getAppointmentDate()       { return appointmentDate; }
    public void      setAppointmentDate(LocalDate v){ appointmentDate = v; }
    public LocalTime getStartTime()             { return startTime; }
    public void      setStartTime(LocalTime v)  { startTime = v; }
    public LocalTime getEndTime()               { return endTime; }
    public void      setEndTime(LocalTime v)    { endTime = v; }
    public String    getStatus()                { return status; }
    public void      setStatus(String v)        { status = v; }
    public String    getNotes()                 { return notes; }
    public void      setNotes(String v)         { notes = v; }
    public String    getCancelReason()          { return cancelReason; }
    public void      setCancelReason(String v)  { cancelReason = v; }
    public Integer   getSlotShift()             { return slotShift; }
    public void      setSlotShift(Integer v)    { slotShift = v; }
    public String    getPetName()               { return petName; }
    public void      setPetName(String v)       { petName = v; }

    // join fields
    public String getCustomerName()              { return customerName; }
    public void   setCustomerName(String v)      { customerName = v; }
    public Integer getRecordID()                 { return recordID; }
    public void    setRecordID(Integer v)        { recordID = v; }

    public String           getPetSpeciesName() { return petSpeciesName; }
    public void             setPetSpeciesName(String v) { petSpeciesName = v; }
    public String           getPetBreedName()  { return petBreedName; }
    public void             setPetBreedName(String v)  { petBreedName = v; }
    public String           getPetGender()    { return petGender; }
    public void             setPetGender(String v)    { petGender = v; }
    public java.math.BigDecimal getPetWeight() { return petWeight; }
    public void             setPetWeight(java.math.BigDecimal v) { petWeight = v; }

    public List<AppointmentService> getServices()                     { return services; }
    public void                     setServices(List<AppointmentService> v) {
        this.services = v != null ? v : new ArrayList<>();
    }

    /**
     * Chuoi hien thi tong hop ten dich vu (vd "Kham tong quat, Tam spa,
     * Vaccine").
     */
    public String getServiceName() {
        if (serviceName != null) return serviceName;
        List<String> names = new ArrayList<>();
        for (AppointmentService s : services) if (s.getServiceName() != null) names.add(s.getServiceName());
        return String.join(", ", names);
    }
    public void setServiceName(String v) { serviceName = v; }

    /** Chuoi hien thi tong hop ten nhom dich vu (distinct). */
    public String getCategoryName() {
        if (categoryName != null) return categoryName;
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (AppointmentService s : services) if (s.getCategoryName() != null) names.add(s.getCategoryName());
        return String.join(", ", names);
    }
    public void setCategoryName(String v) { categoryName = v; }

    /** Chuoi hien thi tong hop ten nhan vien da duoc gan (distinct), co the nhieu nguoi phu trach cac dich vu khac nhau. */
    public String getStaffName() {
        if (staffName != null) return staffName;
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (AppointmentService s : services) if (s.getStaffName() != null) names.add(s.getStaffName());
        return String.join(", ", names);
    }
    public void setStaffName(String v) { staffName = v; }

    /** Danh sach ServiceID da chon - dung cho tinh slot/capacity khi reschedule. */
    public List<Integer> getServiceIds() {
        return services.stream().map(AppointmentService::getServiceID).collect(Collectors.toList());
    }

    // -- Business logic ----------------------------------------------------

    /**
     * Gio cutoff co dinh dung de tinh deadline chinh sua (huy/doi lich):
     * 17:30 CUA NGAY HOM TRUOC ngay hen
     * . Vi du: lich hen ngay 20/07 deu phai duoc huy/doi truoc 17:30 ngay 19/07.
     */
    public static final LocalTime MODIFY_DEADLINE_TIME = LocalTime.of(17, 30);

    /**
     * Tinh deadline 17:30 ngay hom truoc cho 1 ngay hen bat ky.
     */
    public static LocalDateTime deadlineFor(LocalDate date) {
        return LocalDateTime.of(date.minusDays(1), MODIFY_DEADLINE_TIME);
    }

    /**
     * Deadline chung cho ca doi lich (reschedule) va huy lich (cancel):
     */
    public LocalDateTime getModifyDeadline() {
        if (appointmentDate == null || startTime == null) return null;
        if (!"Pending".equals(status) && !"Confirmed".equals(status)) return null;
        return deadlineFor(appointmentDate);
    }

    /**
     * Co the doi lich khong.
     */
    public boolean canReschedule() {
        LocalDateTime deadline = getModifyDeadline();
        return deadline != null && LocalDateTime.now().isBefore(deadline);
    }

    /**
     * Co the huy lich khong.
     */
    public boolean canCancel() {
        return canReschedule();
    }

    /**
     * Con dung cho UI tong quat (badge "Co the chinh sua"): reschedule va
     * cancel nay dung chung dieu kien nen canModify() == canReschedule().
     */
    public boolean canModify() {
        return canReschedule();
    }

    /**
     * Appointment da ket thuc.
     */
    public boolean isCompleted() {
        return "Done".equals(status) || "NoShow".equals(status)
                || "Cancelled".equals(status);
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
            for (AppointmentService s : services) {
                if (s.getUnitPrice() != null) total = total.add(s.getUnitPrice());
            }
        }
        return total;
    }

    /** Tên nhân viên phụ trách các dịch vụ thuộc 1 category, nối dấu phẩy nếu nhiều người khác nhau. */
    public String getStaffNamesByCategory(String categoryName) {
        if (services == null) return "";
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (AppointmentService s : services) {
            if (categoryName.equals(s.getCategoryName()) && s.getStaffName() != null) {
                names.add(s.getStaffName());
            }
        }
        return String.join(", ", names);
    }

    /** True nếu ÍT NHẤT 1 dịch vụ thuộc category chỉ định CHƯA có staff phụ trách. */
    public boolean hasUnassignedServiceInCategory(String categoryName) {
        if (services == null) return false;
        for (AppointmentService s : services) {
            if (categoryName.equals(s.getCategoryName()) && s.getAssignedStaffID() == null) return true;
        }
        return false;
    }

    /** Danh sách dịch vụ thuộc 1 category cụ thể trong appointment này. */
    public List<AppointmentService> getServicesByCategory(String categoryName) {
        List<AppointmentService> result = new ArrayList<>();
        if (services != null) {
            for (AppointmentService s : services) {
                if (categoryName.equals(s.getCategoryName())) result.add(s);
            }
        }
        return result;
    }

    /** True nếu appointment có ít nhất 1 dịch vụ thuộc category chỉ định. */
    public boolean hasCategory(String categoryName) {
        if (services == null) return false;
        for (AppointmentService s : services) {
            if (categoryName.equals(s.getCategoryName())) return true;
        }
        return false;
    }
    /**
     * Appointment dang active (Pending/Confirmed/InProgress).
     */
    public boolean isActive() {
        return "Pending".equals(status) || "Confirmed".equals(status)
                || "InProgress".equals(status);
    }
    public boolean isPetUnassigned() {
        return petID == null;
    }
}