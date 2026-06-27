package com.petclinic.service;

import com.petclinic.dao.AppointmentDAO;
import com.petclinic.dao.ServiceDAO;
import com.petclinic.model.*;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * FIXED-SLOT model (không còn dùng bảng StaffAvailability):
 *
 *  4 slot chính, cố định mỗi ngày, có thể đặt qua booking thường:
 *      Slot 1: 08:00–10:00
 *      Slot 2: 10:00–12:00
 *      (nghỉ trưa 12:00–13:30)
 *      Slot 3: 13:30–15:30
 *      Slot 4: 15:30–17:30
 *  1 slot phụ (OT/qua đêm), KHÔNG cho khách tự đặt qua booking thường —
 *  chỉ staff tạo trực tiếp (ngoài phạm vi codebase này):
 *      Slot 5: 18:30 → 07:00 sáng hôm sau
 *
 *  Capacity: KHÔNG còn tính theo sub-slot/duration. Fix cứng:
 *      maxCapacity(slot, roleGroup) = (số nhân viên active của role đó) × 5
 *
 *  Role mapping (categoryId → roleId), giữ như cũ:
 *      categoryId = 3 (Spa/Grooming) → Groomer (roleId 4)
 *      mọi categoryId khác (Khám, Phẫu thuật, Vaccine) → Vet (roleId 3)
 *
 *  Nếu khách chọn dịch vụ từ CẢ 2 nhóm, capacity tính riêng từng nhóm;
 *  slot hết chỗ nếu MỘT trong 2 nhóm đầy.
 *
 *  Chỉ Confirmed/InProgress/Done mới tính vào load. Pending không tính.
 */
public class BookingService {

    public  static final int    SLOT_MINUTES        = 120;
    private static final LocalTime LUNCH_START      = LocalTime.of(12, 0);
    private static final LocalTime LUNCH_END        = LocalTime.of(13, 30);
    private static final int    DAYS_AHEAD          = 30;

    /** 4 slot chính, cố định — KHÔNG còn phụ thuộc StaffAvailability/DayOfWeek. */
    public static final LocalTime[][] FIXED_SLOTS = {
            { LocalTime.of(8, 0),  LocalTime.of(10, 0) },
            { LocalTime.of(10, 0), LocalTime.of(12, 0) },
            { LocalTime.of(13, 30),LocalTime.of(15, 30) },
            { LocalTime.of(15, 30),LocalTime.of(17, 30) },
    };

    /** Slot phụ (OT/qua đêm) — không cho đặt qua booking thường, chỉ dùng để
     *  nhận diện appointment Done thuộc slot này nhằm tính phụ thu 5%. */
    public static final LocalTime OVERTIME_SLOT_START = LocalTime.of(18, 30);
    public static final LocalTime OVERTIME_SLOT_END   = LocalTime.of(7, 0); // sáng hôm sau

    public static final LocalTime INPATIENT_MORNING_START   = LocalTime.of(8,  0);
    public static final LocalTime INPATIENT_MORNING_END     = LocalTime.of(12, 0);
    public static final LocalTime INPATIENT_AFTERNOON_START = LocalTime.of(13, 30);
    public static final LocalTime INPATIENT_AFTERNOON_END   = LocalTime.of(17, 30);

    public static final double DEPOSIT_RATIO_NORMAL  = 0.20;
    public static final long   DEPOSIT_INPATIENT_VND = 200_000L;


    public static final int GROOMING_CATEGORY_ID  = 3;
    public static final int VACCINE_CATEGORY_ID   = 4;
    public static final int INPATIENT_CATEGORY_ID = 7; // "Dịch vụ nội trú"

    /** True nếu thời điểm bắt đầu trùng đúng slot phụ (18:30) — dùng để tính phụ thu OT. */
    public static boolean isOvertimeSlotStart(LocalTime start) {
        return start != null && start.equals(OVERTIME_SLOT_START);
    }

    /**
     * Suy ra SlotShift (cột mới trong DB) từ giờ bắt đầu — 1-4 khớp index
     * FIXED_SLOTS+1, 5 = slot phụ OT. Trả về null nếu không khớp slot nào
     * (ví dụ: Inpatient — không gán SlotShift vì không tương ứng 1 ca cố định).
     */
    public static Integer slotShiftOf(LocalTime start) {
        if (start == null) return null;
        if (start.equals(OVERTIME_SLOT_START)) return 5;
        for (int i = 0; i < FIXED_SLOTS.length; i++) {
            if (start.equals(FIXED_SLOTS[i][0])) return i + 1;
        }
        return null;
    }

    private final AppointmentDAO appointmentDAO = new AppointmentDAO();
    private final ServiceDAO     serviceDAO     = new ServiceDAO();

    // ─────────────────────────────────────────────────────────────────────────
    //  CAPACITY CALCULATION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Fix cứng: 1 nhân viên (vet/groomer) được nhận tối đa 5 pets / slot.
     * maxCapacity = (số nhân viên active của role tương ứng) × 5.
     * KHÔNG còn tính theo duration dịch vụ.
     */
    public int computeMaxCapacity(List<Service> services, int categoryId) {
        if (services == null || services.isEmpty()) return 5;
        int staff = 1;
        try { staff = Math.max(1, serviceDAO.countStaffByCategoryId(categoryId)); }
        catch (Exception ignored) {}
        return staff * 5;
    }


    /**
     * Capacity info for a slot, split by role group.
     * Returns a SlotCapacity with grooming and vet parts separate.
     */
    public SlotCapacity computeSlotCapacity(
            List<Service> groomingServices,
            List<Service> vetServices) throws Exception {

        int groomCap = groomingServices.isEmpty() ? 0
                : computeMaxCapacity(groomingServices, GROOMING_CATEGORY_ID);
        int vetCap   = vetServices.isEmpty() ? 0
                : computeMaxCapacity(vetServices, 2 /* any non-1 categoryId → Vet */);

        return new SlotCapacity(groomCap, vetCap,
                groomingServices.isEmpty() ? 0 : serviceDAO.countStaffByCategoryId(GROOMING_CATEGORY_ID),
                vetServices.isEmpty()      ? 0 : serviceDAO.countStaffByCategoryId(2));
    }

    /** Simple holder for per-role-group capacity info. */
    public static class SlotCapacity {
        public final int groomCap, vetCap, groomStaff, vetStaff;
        public SlotCapacity(int gc, int vc, int gs, int vs) {
            groomCap = gc; vetCap = vc; groomStaff = gs; vetStaff = vs;
        }
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

        int groomCap  = groomSvcs.isEmpty() ? 0 : computeMaxCapacity(groomSvcs, GROOMING_CATEGORY_ID);
        int vetCap    = vetSvcs.isEmpty()   ? 0 : computeMaxCapacity(vetSvcs,   2);

        // If no services selected yet, use a placeholder capacity for display
        boolean noSvcSelected = allServices.isEmpty();

        LocalDateTime cutoff  = apply12hCutoff ? LocalDateTime.now().plusHours(12) : LocalDateTime.MIN;
        LocalDate today       = LocalDate.now();
        Map<LocalDate, List<TimeSlot>> result = new LinkedHashMap<>();

        // Lịch CỐ ĐỊNH, giống nhau mọi ngày (không còn phân biệt DayOfWeek /
        // StaffAvailability) — chỉ 4 slot chính, KHÔNG bao gồm slot phụ OT.
        for (int d = 0; d < DAYS_AHEAD; d++) {
            LocalDate date  = today.plusDays(d);
            List<TimeSlot> slots = new ArrayList<>();

            for (LocalTime[] win : FIXED_SLOTS) {
                LocalTime cursor  = win[0];
                LocalTime slotEnd = win[1];

                // 12h cutoff (dùng cho reschedule — không cho chọn slot quá gần)
                if (apply12hCutoff && !cutoff.isBefore(LocalDateTime.of(date, cursor))) {
                    continue;
                }

                int slotShift = slotShiftOf(cursor); // luôn khớp 1 trong FIXED_SLOTS ở đây
                int groomLoad = groomSvcs.isEmpty() ? 0
                        : appointmentDAO.countConfirmedInSlotByRoleGroup(
                        date, slotShift, GROOMING_CATEGORY_ID);
                int vetLoad   = vetSvcs.isEmpty()   ? 0
                        : appointmentDAO.countConfirmedInSlotByRoleGroup(
                        date, slotShift, 2);

                boolean groomOk = groomSvcs.isEmpty() || groomLoad < groomCap;
                boolean vetOk   = vetSvcs.isEmpty()   || vetLoad   < vetCap;
                boolean available = noSvcSelected || (groomOk && vetOk);

                int totalLoad  = groomLoad + vetLoad;
                int totalCap   = Math.max(1, groomCap + vetCap);

                TimeSlot ts = new TimeSlot(date, cursor, slotEnd, available);
                ts.setCurrentLoad(totalLoad);
                ts.setMaxCapacity(noSvcSelected ? 100 : totalCap);
                ts.setGroomLoad(groomLoad);  ts.setGroomCap(groomCap);
                ts.setVetLoad(vetLoad);       ts.setVetCap(vetCap);
                slots.add(ts);

                System.out.print("Cap: ");
                System.out.println(groomCap + "   " + vetCap + "    " + totalCap);
                System.out.print("Load: ");
                System.out.println(groomLoad + "   " + vetLoad + "   " + totalLoad);
                System.out.println(groomSvcs.size() + "   " + vetSvcs.size());
                System.out.println(allServices.size() + "   " + slots.size());
            }
            if (!slots.isEmpty()) result.put(date, slots);


        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  APPOINTMENT CREATION
    // ─────────────────────────────────────────────────────────────────────────

    public List<Integer> createAppointments(int customerId, List<Integer> petIds,
                                            List<Integer> serviceIds, String slotKey,
                                            boolean isInpatient, String inpatientDate,
                                            String inpatientPeriod) throws Exception {
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

        for (int petId : petIds) {
            for (int serviceId : serviceIds) {
                Appointment a = new Appointment();
                a.setCustomerID(customerId);
                a.setPetID(petId);
                a.setServiceID(serviceId);
                a.setAppointmentDate(date);
                a.setStartTime(start);
                a.setEndTime(end);
                a.setStatus("Pending");
                // Inpatient không khớp 1 ca cố định nào (gộp 2 ca buổi sáng/chiều)
                // nên để SlotShift = null, chỉ set cho booking thường.
                if (!isInpatient) a.setSlotShift(slotShiftOf(start));
                created.add(appointmentDAO.insert(a));
            }
        }
        return created;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DEPOSIT
    // ─────────────────────────────────────────────────────────────────────────

    public long computeDeposit(BigDecimal totalPrice, boolean isInpatient) {
        if (isInpatient) return DEPOSIT_INPATIENT_VND;
        if (totalPrice == null) return 0L;
        return Math.max(1L, Math.round(totalPrice.doubleValue() * DEPOSIT_RATIO_NORMAL));
    }
}