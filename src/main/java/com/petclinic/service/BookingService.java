package com.petclinic.service;

import com.petclinic.dao.AppointmentDAO;
import com.petclinic.dao.AppointmentServiceDAO;
import com.petclinic.dao.ServiceDAO;
import com.petclinic.dto.PetBookingRequest;
import com.petclinic.dto.TimeSlot;
import com.petclinic.model.*;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * FIXED-SLOT model (khong con dung bang StaffAvailability):
 *
 *  4 slot chinh, co dinh moi ngay, co the dat qua booking thuong:
 *      Slot 1: 08:00-10:00
 *      Slot 2: 10:00-12:00
 *      (nghi trua 12:00-13:30)
 *      Slot 3: 13:30-15:30
 *      Slot 4: 15:30-17:30
 *  1 slot phu (OT/qua dem), KHONG cho khach tu dat qua booking thuong -
 *  chi staff tao truc tiep (ngoai pham vi codebase nay):
 *      Slot 5: 18:30 -> 07:00 sang hom sau
 *
 *  Capacity: Fix cung:
 *      maxCapacity(slot, roleGroup) = (so nhan vien active cua role do) x 5
 *
 *  Role mapping (categoryId -> roleId), giu nhu cu:
 *      categoryId = 3 (Spa/Grooming) -> Groomer (roleId 4)
 *      moi categoryId khac (Kham, Phau thuat, Vaccine) -> Vet (roleId 3)
 *
 *  Chi Confirmed/InProgress/Done moi tinh vao load. Pending khong tinh.
 *
 * ─────────────────────────────────────────────────────────────────────────
 *  MODEL BOOKING (N-N qua AppointmentServices): 1 appointment = 1 pet +
 *  1 slot, nhieu dich vu.
 * ─────────────────────────────────────────────────────────────────────────
 */
public class BookingService {

    public  static final int    SLOT_MINUTES        = 120;
    private static final LocalTime LUNCH_START      = LocalTime.of(12, 0);
    private static final LocalTime LUNCH_END        = LocalTime.of(13, 30);
    private static final int    DAYS_AHEAD          = 30;

    /** 4 slot chinh, co dinh */
    public static final LocalTime[][] FIXED_SLOTS = {
            { LocalTime.of(8, 0),  LocalTime.of(10, 0) },
            { LocalTime.of(10, 0), LocalTime.of(12, 0) },
            { LocalTime.of(13, 30),LocalTime.of(15, 30) },
            { LocalTime.of(15, 30),LocalTime.of(17, 30) },
    };

    /** Slot phu (OT/qua dem) - khong cho dat qua booking thuong, chi dung de
     *  nhan dien appointment Done thuoc slot nay nham tinh phu thu 5%. */
    public static final LocalTime OVERTIME_SLOT_START = LocalTime.of(18, 30);
    public static final LocalTime OVERTIME_SLOT_END   = LocalTime.of(7, 0); // sang hom sau

    public static final LocalTime INPATIENT_MORNING_START   = LocalTime.of(8,  0);
    public static final LocalTime INPATIENT_MORNING_END     = LocalTime.of(12, 0);
    public static final LocalTime INPATIENT_AFTERNOON_START = LocalTime.of(13, 30);
    public static final LocalTime INPATIENT_AFTERNOON_END   = LocalTime.of(17, 30);

    /**
     * Tien coc CO DINH — CHI con ap dung cho Noi tru. Booking thuong KHONG
     * con coc, khach luon thanh toan 100% tong chi phi.
     */
    public static final long DEPOSIT_INPATIENT = 200_000L;


    public static final int GROOMING_CATEGORY_ID  = 3;
    public static final int VACCINE_CATEGORY_ID   = 4;
    public static final int INPATIENT_CATEGORY_ID = 7; // "Dich vu noi tru"
    private static final int VET_ROLE_ID     = 3;
    private static final int GROOMER_ROLE_ID = 4;

    /** True neu thoi diem bat dau trung dung slot phu (18:30) - dung de tinh phu thu OT. */
    public static boolean isOvertimeSlotStart(LocalTime start) {
        return start != null && start.equals(OVERTIME_SLOT_START);
    }

    /**
     * SlotShift: 1-4
     * FIXED_SLOTS+1, 5 = slot phu OT. Tra ve null neu khong khop slot nao
     */
    public static Integer slotShiftOf(LocalTime start) {
        if (start == null) return null;
        if (start.equals(OVERTIME_SLOT_START)) return 5;
        for (int i = 0; i < FIXED_SLOTS.length; i++) {
            if (start.equals(FIXED_SLOTS[i][0])) return i + 1;
        }
        return null;
    }

    private final AppointmentDAO        appointmentDAO        = new AppointmentDAO();
    private final ServiceDAO            serviceDAO            = new ServiceDAO();
    private final AppointmentServiceDAO appointmentServiceDAO = new AppointmentServiceDAO();

    // ─────────────────────────────────────────────────────────────────────────
    //  CAPACITY CALCULATION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Fix cung: 1 nhan vien (vet/groomer) duoc nhan toi da 5 pets / slot.
     */
    private int computeStaffCapacityForRole(int roleId) {
        int staff = 1;
        try { staff = Math.max(1, serviceDAO.countStaffByRoleId(roleId)); }
        catch (Exception ignored) {}
        return staff * 5;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  OUTPATIENT SLOT GENERATION
    // ─────────────────────────────────────────────────────────────────────────

    public Map<LocalDate, List<TimeSlot>> generateSlots(List<Integer> serviceIds) throws Exception {
        return generateSlotsInternal(serviceIds, -1, true);
    }

    public Map<LocalDate, List<TimeSlot>> generateSlotsForReschedule(
            List<Integer> serviceIds, int excludeAppointmentId) throws Exception {
        return generateSlotsInternal(serviceIds, excludeAppointmentId, true);
    }

    private Map<LocalDate, List<TimeSlot>> generateSlotsInternal(
            List<Integer> serviceIds, int excludeId, boolean apply12hCutoff) throws Exception {

        // Split selected services into Grooming vs Vet groups
        List<Service> allServices = (serviceIds != null && !serviceIds.isEmpty())
                ? serviceDAO.findByIds(serviceIds) : Collections.emptyList();

        List<Service> groomSvcs = new ArrayList<>();
        List<Service> vetSvcs   = new ArrayList<>();
        for (Service s : allServices) {
            if (s.getCategoryID() == GROOMING_CATEGORY_ID) groomSvcs.add(s);
            else                                            vetSvcs.add(s);
        }

        boolean vetNeeded   = !vetSvcs.isEmpty();
        boolean groomNeeded = !groomSvcs.isEmpty();

        int groomCap  = groomNeeded ? computeStaffCapacityForRole(GROOMER_ROLE_ID) : 0;
        int vetCap    = vetNeeded   ? computeStaffCapacityForRole(VET_ROLE_ID) : 0;

        // If no services selected yet, use a placeholder capacity for display
        boolean noSelectionYet = allServices.isEmpty();

        LocalDateTime cutoff  = apply12hCutoff ? LocalDateTime.now().plusHours(12) : LocalDateTime.MIN;
        LocalDate today       = LocalDate.now();
        Map<LocalDate, List<TimeSlot>> result = new LinkedHashMap<>();


        for (int d = 0; d < DAYS_AHEAD; d++) {
            LocalDate date  = today.plusDays(d);
            List<TimeSlot> slots = new ArrayList<>();

            for (LocalTime[] win : FIXED_SLOTS) {
                LocalTime cursor  = win[0];
                LocalTime slotEnd = win[1];

                // 12h cutoff (dung cho reschedule - khong cho chon slot qua gan)
                if (apply12hCutoff && !cutoff.isBefore(LocalDateTime.of(date, cursor))) {
                    continue;
                }

                int slotShift = slotShiftOf(cursor); // luon khop 1 trong FIXED_SLOTS
                int groomLoad = groomNeeded ? appointmentDAO.countConfirmedInSlotByRoleGroup(
                        date, slotShift, 4) : 0;
                int vetLoad   = vetNeeded   ? appointmentDAO.countConfirmedInSlotByRoleGroup(
                        date, slotShift, VET_ROLE_ID) : 0;

                boolean groomOk = !groomNeeded || groomLoad < groomCap;
                boolean vetOk   = !vetNeeded   || vetLoad   < vetCap;
                boolean available = noSelectionYet || (groomOk && vetOk);

                int totalLoad  = groomLoad + vetLoad;
                int totalCap   = Math.max(1, groomCap + vetCap);

                TimeSlot ts = new TimeSlot(date, cursor, slotEnd, available);
                ts.setCurrentLoad(totalLoad);
                ts.setMaxCapacity(noSelectionYet ? 100 : totalCap);
                ts.setPlaceholder(noSelectionYet);
                ts.setGroomLoad(groomLoad);  ts.setGroomCap(groomCap);
                ts.setVetLoad(vetLoad);       ts.setVetCap(vetCap);
                slots.add(ts);
            }
            if (!slots.isEmpty()) result.put(date, slots);
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  APPOINTMENT CREATION — NOI TRU
    // ─────────────────────────────────────────────────────────────────────────
    public List<Integer> createAppointments(int customerId, List<Integer> petIds,
                                            List<Integer> serviceIds, String slotKey,
                                            boolean isInpatient, String inpatientDate,
                                            String inpatientPeriod) throws Exception {
        if (petIds == null || petIds.size() != 1) {
            throw new IllegalArgumentException("Mỗi lượt đặt lịch chỉ áp dụng cho đúng 1 thú cưng.");
        }
        if (serviceIds == null || serviceIds.size() != 1) {
            throw new IllegalArgumentException("Mỗi lượt đặt lịch chỉ áp dụng cho đúng 1 dịch vụ.");
        }

        List<Integer> created = new ArrayList<>();
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");

        LocalDate date; LocalTime start, end;
        if (isInpatient) {
            date  = LocalDate.parse(inpatientDate, df);
            boolean isMorning = "morning".equalsIgnoreCase(inpatientPeriod);
            start = isMorning ? INPATIENT_MORNING_START   : INPATIENT_AFTERNOON_START;
            end   = isMorning ? INPATIENT_MORNING_END     : INPATIENT_AFTERNOON_END;
        } else {
            String[] parts = slotKey.split("\\|");
            date  = LocalDate.parse(parts[0], df);
            start = LocalTime.parse(parts[1], tf);
            end   = start.plusMinutes(SLOT_MINUTES);
        }

        Appointment a = new Appointment();
        a.setCustomerID(customerId);
        a.setPetID(petIds.get(0));
        a.setAppointmentDate(date);
        a.setStartTime(start);
        a.setEndTime(end);
        a.setStatus("Pending");
        // Inpatient khong khop 1 ca co dinh nao nen de SlotShift = null
        if (!isInpatient) a.setSlotShift(slotShiftOf(start));

        int serviceId = serviceIds.get(0);
        Service svc = serviceDAO.findById(serviceId);
        if (svc == null) {
            throw new IllegalStateException(
                    "Chưa cấu hình dịch vụ đại diện cho nhóm dịch vụ này (ServiceID=" + serviceId
                            + " không tồn tại hoặc đã ngừng hoạt động). Vui lòng liên hệ quản trị viên để thêm ít nhất 1 dịch vụ (IsActive=1) cho nhóm này.");
        }

        int apptId = appointmentDAO.insert(a);
        if (apptId > 0) {
            BigDecimal price = svc.getPrice() != null ? svc.getPrice() : BigDecimal.ZERO;
            appointmentServiceDAO.insert(apptId, serviceId, price);
            created.add(apptId);
        }
        return created;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  APPOINTMENT CREATION — 1 appointment = 1 pet + NHIEU dich vu + 1 slot
    // ─────────────────────────────────────────────────────────────────────────
    public List<Integer> createAppointmentsForPets(int customerId, List<PetBookingRequest> booking,
                                                   String slotKey) throws Exception {
        if (booking == null || booking.size() != 1) {
            throw new IllegalArgumentException("Mỗi lượt đặt lịch chỉ áp dụng cho đúng 1 thú cưng.");
        }
        PetBookingRequest r = booking.get(0);
        if (r.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất 1 dịch vụ hoặc vaccine.");
        }

        List<Integer> created = new ArrayList<>();
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");

        String[] parts = slotKey.split("\\|");
        LocalDate date  = LocalDate.parse(parts[0], df);
        LocalTime start = LocalTime.parse(parts[1], tf);
        LocalTime end   = start.plusMinutes(SLOT_MINUTES);
        Integer   shift = slotShiftOf(start);

        Appointment a = new Appointment();
        a.setCustomerID(customerId);
        a.setPetID(r.getPetId());
        a.setAppointmentDate(date);
        a.setStartTime(start);
        a.setEndTime(end);
        a.setStatus("Pending");
        a.setSlotShift(shift);

        int apptId = appointmentDAO.insert(a);
        if (apptId <= 0) return created;

        // Moi dich vu duoc snapshot gia hien tai (Service.Price) tai thoi
        // diem dat lich — khong doi ke ca khi Service.Price thay doi sau nay.
        if (!r.getServiceIds().isEmpty()) {
            List<Service> svcs = serviceDAO.findByIds(r.getServiceIds());
            for (Service svc : svcs) {
                BigDecimal price = svc.getPrice() != null ? svc.getPrice() : BigDecimal.ZERO;
                appointmentServiceDAO.insert(apptId, svc.getServiceID(), price);
            }
        }
        if (!r.getVaccineIds().isEmpty()) {
            Service vaccinePlaceholder = serviceDAO.findFirstActiveByCategory(VACCINE_CATEGORY_ID);
            if (vaccinePlaceholder != null) {
                BigDecimal price = vaccinePlaceholder.getPrice() != null
                        ? vaccinePlaceholder.getPrice() : BigDecimal.ZERO;
                appointmentServiceDAO.insert(apptId, vaccinePlaceholder.getServiceID(), price);
            }
        }

        created.add(apptId);
        return created;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DEPOSIT
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Tien coc:
     *   - Noi tru:     DEPOSIT_INPATIENT (200.000d) — phi tam ung nhap vien.
     *   - Binh thuong: 0 (khach thanh toan 100% tong chi phi ngay khi dat lich).
     */
    public long computeDeposit(boolean isInpatient) {
        return isInpatient ? DEPOSIT_INPATIENT : 0L;
    }
}
