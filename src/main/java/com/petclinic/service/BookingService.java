package com.petclinic.service;

import com.petclinic.dao.AppointmentDAO;
import com.petclinic.dao.ServiceDAO;
import com.petclinic.model.*;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Slot-based Allocation rules:
 *
 *  Sub-slot unit = 30 minutes.
 *  One appointment slot = 120 minutes = 4 sub-slots.
 *  For each service:
 *      subSlots(service) = ceil(service.durationMinutes / 30)
 *  maxCapacity(slot, services) = Σ subSlots / staffCount(roleGroup)
 *
 *  Role mapping (categoryId → roleId):
 *      categoryId = 3  → Groomer (roleId 4))
 *      everything else → Vet     (roleId 3)
 *
 *  If customer selects services from BOTH groups, capacity is computed
 *  separately per group; the slot is unavailable if EITHER group is full.
 *
 *  Only Confirmed/InProgress/Done appointments count toward load.
 *  Pending appointments do NOT consume capacity.
 */
public class BookingService {

    public  static final int    SLOT_MINUTES    = 120;
    public  static final int    SUB_SLOT_MINUTES= 30;
    private static final LocalTime LUNCH_START  = LocalTime.of(12, 0);
    private static final LocalTime LUNCH_END    = LocalTime.of(13, 30);
    private static final int    DAYS_AHEAD      = 30;

    public static final LocalTime INPATIENT_MORNING_START   = LocalTime.of(8,  0);
    public static final LocalTime INPATIENT_MORNING_END     = LocalTime.of(12, 0);
    public static final LocalTime INPATIENT_AFTERNOON_START = LocalTime.of(13, 30);
    public static final LocalTime INPATIENT_AFTERNOON_END   = LocalTime.of(17, 30);

    public static final double DEPOSIT_RATIO_NORMAL  = 0.20;
    public static final long   DEPOSIT_INPATIENT_VND = 200_000L;

    // Grooming category ID constant
    public static final int GROOMING_CATEGORY_ID = 3;

    private final AppointmentDAO appointmentDAO = new AppointmentDAO();
    private final ServiceDAO     serviceDAO     = new ServiceDAO();

    // ─────────────────────────────────────────────────────────────────────────
    //  CAPACITY CALCULATION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Sub-slot capacity for a list of services belonging to the SAME role group.
     *
     * maxCapacity = Σ ceil(svc.duration / SUB_SLOT_MINUTES)  ×  staffCount
     *
     * Rationale: each staff member can handle as many sub-slots as the
     * slot contains.  Total capacity = how many customers fit across all staff.
     */
    public int computeMaxCapacity(List<Service> services, int categoryId) {
        if (services == null || services.isEmpty()) return 1;
        int totalSubSlots = services.stream()
                .mapToInt(s -> (int) Math.ceil((double) s.getDurationMinutes() / SUB_SLOT_MINUTES))
                .sum();
        if (totalSubSlots <= 0) totalSubSlots = SLOT_MINUTES / SUB_SLOT_MINUTES; // default 4
        int staff = 1;
        try { staff = Math.max(1, serviceDAO.countStaffByCategoryId(categoryId)); }
        catch (Exception ignored) {}
        return totalSubSlots * staff;
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
        return generateSlotsInternal(serviceIds, -1, false);
    }

    public Map<LocalDate, List<TimeSlot>> generateSlotsForReschedule(
            List<Integer> serviceIds, int excludeAppointmentId) throws Exception {
        return generateSlotsInternal(serviceIds, excludeAppointmentId, true);
    }

    private Map<LocalDate, List<TimeSlot>> generateSlotsInternal(
            List<Integer> serviceIds, int excludeId, boolean apply12hCutoff) throws Exception {

        List<StaffAvailability> vetAvail = appointmentDAO.findVetAvailability();
        Map<Integer, List<LocalTime[]>> windowsByDay = new TreeMap<>();
        for (StaffAvailability sa : vetAvail) {
            windowsByDay.computeIfAbsent(sa.getDayOfWeek(), k -> new ArrayList<>())
                        .add(new LocalTime[]{sa.getStartTime(), sa.getEndTime()});
        }

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

        for (int d = 0; d < DAYS_AHEAD; d++) {
            LocalDate date  = today.plusDays(d);
            int dbDow = date.getDayOfWeek() == DayOfWeek.SUNDAY ? 0 : date.getDayOfWeek().getValue();
            List<LocalTime[]> windows = windowsByDay.get(dbDow);
            if (windows == null || windows.isEmpty()) continue;

            List<TimeSlot> slots = new ArrayList<>();
            for (LocalTime[] win : windows) {
                LocalTime cursor = win[0];
                while (true) {
                    LocalTime slotEnd = cursor.plusMinutes(SLOT_MINUTES);
                    if (slotEnd.isAfter(win[1])) break;

                    // Skip lunch
                    if (!cursor.isBefore(LUNCH_START) && cursor.isBefore(LUNCH_END)) {
                        cursor = LUNCH_END; continue;
                    }
                    if (cursor.isBefore(LUNCH_START) && slotEnd.isAfter(LUNCH_START)) {
                        cursor = LUNCH_END; continue;
                    }

                    // 12h cutoff
                    if (apply12hCutoff && !cutoff.isBefore(LocalDateTime.of(date, cursor))) {
                        cursor = cursor.plusMinutes(SLOT_MINUTES); continue;
                    }

                    // Load per group
                    int groomLoad = groomSvcs.isEmpty() ? 0
                            : appointmentDAO.countConfirmedInSlotByRoleGroup(
                                    date, cursor, slotEnd, GROOMING_CATEGORY_ID);
                    int vetLoad   = vetSvcs.isEmpty()   ? 0
                            : appointmentDAO.countConfirmedInSlotByRoleGroup(
                                    date, cursor, slotEnd, 2);

                    // Slot is available if BOTH groups still have capacity
                    // (or if that group is not requested)
                    boolean groomOk = groomSvcs.isEmpty() || groomLoad < groomCap;
                    boolean vetOk   = vetSvcs.isEmpty()   || vetLoad   < vetCap;
                    boolean available = noSvcSelected || (groomOk && vetOk);

                    // Represent fill as combined load / combined capacity
                    int totalLoad  = groomLoad + vetLoad;
                    int totalCap   = Math.max(1, groomCap + vetCap);

                    TimeSlot ts = new TimeSlot(date, cursor, slotEnd, available);
                    ts.setCurrentLoad(totalLoad);
                    ts.setMaxCapacity(noSvcSelected ? 999 : totalCap);
                    // Store per-group info in extended fields
                    ts.setGroomLoad(groomLoad);  ts.setGroomCap(groomCap);
                    ts.setVetLoad(vetLoad);       ts.setVetCap(vetCap);
                    slots.add(ts);

                    cursor = cursor.plusMinutes(SLOT_MINUTES);
                }
            }
            if (!slots.isEmpty()) result.put(date, slots);
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  INPATIENT SLOTS
    // ─────────────────────────────────────────────────────────────────────────

    public List<TimeSlot> generateInpatientSlots(LocalDate date, int serviceId) throws Exception {
        List<TimeSlot> slots = new ArrayList<>();
        Service svc = serviceDAO.findById(serviceId);
        List<Service> svcList = svc != null ? Collections.singletonList(svc) : Collections.emptyList();
        int catId = svc != null ? svc.getCategoryID() : 2;
        int cap   = svcList.isEmpty() ? 1 : computeMaxCapacity(svcList, catId);

        LocalTime mStart = INPATIENT_MORNING_START,   mEnd = INPATIENT_MORNING_END;
        LocalTime aStart = INPATIENT_AFTERNOON_START, aEnd = INPATIENT_AFTERNOON_END;

        int mLoad = appointmentDAO.countConfirmedInSlotByRoleGroup(date, mStart, mEnd, catId);
        TimeSlot morning = new TimeSlot(date, mStart, mEnd, mLoad < cap);
        morning.setMaxCapacity(cap); morning.setCurrentLoad(mLoad); morning.setInpatient(true);
        slots.add(morning);

        int aLoad = appointmentDAO.countConfirmedInSlotByRoleGroup(date, aStart, aEnd, catId);
        TimeSlot afternoon = new TimeSlot(date, aStart, aEnd, aLoad < cap);
        afternoon.setMaxCapacity(cap); afternoon.setCurrentLoad(aLoad); afternoon.setInpatient(true);
        slots.add(afternoon);

        return slots;
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
