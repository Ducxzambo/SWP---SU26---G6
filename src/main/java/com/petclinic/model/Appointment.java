package com.petclinic.model;

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

/**
 * 1 appointment = 1 customer + 1 pet + 1 slot, nhung nay co the gan NHIEU
 * dich vu qua bang join AppointmentServices (N-N) - khop voi schema moi.
 * Appointments KHONG con cot ServiceID hay AssignedStaffID truc tiep; cac
 * thong tin nay nay nam o tung dong AppointmentService (moi dich vu co the
 * duoc gan cho 1 nhan vien khac nhau).
 *
 * Luu y ve Vaccine: khong co bang join rieng cho vaccine. Khi khach chon
 * category "Vaccine" (co the chon nhieu vaccine cu the), he thong CHI insert
 * DUY NHAT 1 dong AppointmentServices dai dien cho ca category Vaccine (dung
 * 1 Service "dai dien" co CategoryID = Vaccine) — con danh sach CU THE cac
 * vaccine da chon (co the nhieu) duoc luu chi tiet ben InvoiceItems
 * (ItemType='Vaccine'), khong nam trong AppointmentServices/Appointment nay.
 * Tuong tu voi category "Dich vu noi tru" (chi 1 dong dai dien).
 *
 * serviceName / categoryName / staffName ben duoi la cac truong HIEN THI
 * tong hop (aggregate), duoc AppointmentDAO tinh tu danh sach services -
 * giu de tuong thich nguoc voi JSP/JS hien co von hien thi 1 chuoi duy nhat.
 */
public class Appointment {
    private int       appointmentID;
    private int       customerID;
    private int       petID;
    private LocalDate appointmentDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String    status;
    private String    notes;
    private String    cancelReason;
    private Integer   slotShift; // 1-4 = 4 ca chinh co dinh (FIXED_SLOTS), 5 = ca phu OT (18:30-07:00, chi staff tao)

    // Chi tiet dich vu da chon (N-N)
    private List<AppointmentService> services = new ArrayList<>();

    // Joined display fields (tong hop tu services)
    private String    petName;
    private String    serviceName;
    private String    categoryName;
    private String    staffName;

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
    public int       getPetID()                 { return petID; }
    public void      setPetID(int v)            { petID = v; }
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
     * Deadline chung cho ca doi lich (reschedule) va huy lich (cancel):
     * dung 12 gio truoc StartTime cua chinh lich hen nay. Ap dung nhu nhau cho ca
     * Pending va Confirmed. Tra ve null neu status khac (khong cho chinh
     * sua) hoac thieu AppointmentDate/StartTime.
     */
    public LocalDateTime getModifyDeadline() {
        if (appointmentDate == null || startTime == null) return null;
        if (!"Pending".equals(status) && !"Confirmed".equals(status)) return null;
        return LocalDateTime.of(appointmentDate, startTime).minusHours(12);
    }

    /**
     * Co the doi lich khong. Luu y: sau khi doi lich, Status duoc GIU
     * NGUYEN (khong reset ve Pending) - xem AppointmentDAO.updateSlot().
     */
    public boolean canReschedule() {
        LocalDateTime deadline = getModifyDeadline();
        return deadline != null && LocalDateTime.now().isBefore(deadline);
    }

    /**
     * Co the huy lich khong - dung chung deadline 12 gio truoc StartTime
     * voi reschedule.
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

    /**
     * Appointment dang active (Pending/Confirmed/InProgress).
     */
    public boolean isActive() {
        return "Pending".equals(status) || "Confirmed".equals(status)
                || "InProgress".equals(status);
    }
}