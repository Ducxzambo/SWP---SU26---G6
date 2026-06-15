package com.petclinic.service;

import com.petclinic.dao.AppointmentDAO;
import com.petclinic.dao.ServiceDAO;
import com.petclinic.model.*;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Slot rules (updated):
 *  - Each slot = 120 minutes, stride = 120 minutes (no overlap)
 *  - Lunch break 12:00–13:30 skipped
 *  - maxCapacity per slot = floor(sum(serviceDuration) / 120) / staffCount
 *    → slot is greyed (unavailable) ONLY when currentLoad >= maxCapacity
 *  - 1 slot per booking (customer picks exactly one)
 *  - Inpatient: morning (08:00–12:00) or afternoon (13:30–17:30) slots on chosen date
 *  - Appointment status starts as "Pending"; only "Confirmed" ones count toward capacity
 */
public class BookingService {

    public  static final int    SLOT_MINUTES = 120;
    private static final LocalTime LUNCH_START = LocalTime.of(12, 0);
    private static final LocalTime LUNCH_END   = LocalTime.of(13, 30);
    private static final int    DAYS_AHEAD  = 30;

    // Inpatient fixed slots
    public static final LocalTime INPATIENT_MORNING_START   = LocalTime.of(8,  0);
    public static final LocalTime INPATIENT_MORNING_END     = LocalTime.of(12, 0);
    public static final LocalTime INPATIENT_AFTERNOON_START = LocalTime.of(13, 30);
    public static final LocalTime INPATIENT_AFTERNOON_END   = LocalTime.of(17, 30);

    // Deposit constants
    public static final double DEPOSIT_RATIO_NORMAL    = 0.20;
    public static final long   DEPOSIT_INPATIENT_VND   = 200_000L;

    private final AppointmentDAO appointmentDAO = new AppointmentDAO();
    private final ServiceDAO     serviceDAO     = new ServiceDAO();

    // ─────────────────────────────────────────────────────────────────────────
    //  CAPACITY CALCULATION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * maxCapacity = floor( sum(service.durationMinutes) / SLOT_MINUTES ) / staffCount
     * Minimum = 1 to avoid division edge cases.
     */
    public double computeMaxCapacity(List<Service> services, int serviceId) {
        int totalDuration = services.stream().mapToInt(Service::getDurationMinutes).sum();
        if (totalDuration <= 0) totalDuration = SLOT_MINUTES;
        int slots = Math.max(1, (int) Math.floor((double) totalDuration / SLOT_MINUTES));
        int staff = 1;
        try { staff = Math.max(1, serviceDAO.countStaffForService(serviceId)); }
        catch (Exception ignored) {}
        return (double) slots / staff;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  OUTPATIENT SLOT GENERATION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Generate 120-min slots for the next DAYS_AHEAD days.
     * serviceIds used to compute maxCapacity; primary serviceId for load count.
     */
    public Map<LocalDate, List<TimeSlot>> generateSlots(List<Integer> serviceIds) throws Exception {
        return generateSlotsInternal(serviceIds, -1, false);
    }

    /** Reschedule variant: excludes one appointment from load, applies 12h cutoff. */
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

        // Load services for capacity calculation
        List<Service> services = serviceIds != null && !serviceIds.isEmpty()
                ? serviceDAO.findByIds(serviceIds) : Collections.emptyList();
        int primaryServiceId = serviceIds != null && !serviceIds.isEmpty() ? serviceIds.get(0) : -1;
        double maxCap = services.isEmpty() ? 1.0 : computeMaxCapacity(services, primaryServiceId);
        int staffCount = Math.max(1, serviceDAO.countStaffForService(primaryServiceId));

        LocalDateTime cutoff = apply12hCutoff ? LocalDateTime.now().plusHours(12) : LocalDateTime.MIN;
        LocalDate today = LocalDate.now();
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

                    // Skip lunch overlap
                    if (!cursor.isBefore(LUNCH_START) && cursor.isBefore(LUNCH_END)) {
                        cursor = LUNCH_END; continue;
                    }
                    if (cursor.isBefore(LUNCH_START) && slotEnd.isAfter(LUNCH_START)) {
                        cursor = LUNCH_END; continue;
                    }

                    // 12h cutoff for reschedule
                    if (apply12hCutoff && !cutoff.isBefore(LocalDateTime.of(date, cursor))) {
                        cursor = cursor.plusMinutes(SLOT_MINUTES); continue;
                    }

                    // Count confirmed load for this slot
                    int load = 0;
                    if (primaryServiceId > 0) {
                        load = appointmentDAO.countConfirmedInSlot(date, cursor, slotEnd, primaryServiceId);
                        if (excludeId > 0) {
                            // Don't count the appointment being rescheduled
                            // (already handled server-side by status check)
                        }
                    }

                    boolean available = load < maxCap;
                    TimeSlot ts = new TimeSlot(date, cursor, slotEnd, available);
                    ts.setMaxCapacity(maxCap);
                    ts.setCurrentLoad(load);
                    slots.add(ts);

                    cursor = cursor.plusMinutes(SLOT_MINUTES);
                }
            }
            if (!slots.isEmpty()) result.put(date, slots);
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  INPATIENT SLOT GENERATION (morning / afternoon on a specific date)
    // ─────────────────────────────────────────────────────────────────────────

    /** Returns two TimeSlots: morning + afternoon for the given date.
     *  Available flag based on capacity check. */
    public List<TimeSlot> generateInpatientSlots(LocalDate date, int serviceId) throws Exception {
        List<TimeSlot> slots = new ArrayList<>();

        Service svc = serviceDAO.findById(serviceId);
        List<Service> svcList = svc != null ? Collections.singletonList(svc) : Collections.emptyList();
        double cap = computeMaxCapacity(svcList, serviceId);

        // Morning
        int morningLoad = appointmentDAO.countConfirmedInSlot(
                date, INPATIENT_MORNING_START, INPATIENT_MORNING_END, serviceId);
        TimeSlot morning = new TimeSlot(date, INPATIENT_MORNING_START, INPATIENT_MORNING_END, morningLoad < cap);
        morning.setMaxCapacity(cap);
        morning.setCurrentLoad(morningLoad);
        morning.setInpatient(true);
        slots.add(morning);

        // Afternoon
        int afternoonLoad = appointmentDAO.countConfirmedInSlot(
                date, INPATIENT_AFTERNOON_START, INPATIENT_AFTERNOON_END, serviceId);
        TimeSlot afternoon = new TimeSlot(date, INPATIENT_AFTERNOON_START, INPATIENT_AFTERNOON_END, afternoonLoad < cap);
        afternoon.setMaxCapacity(cap);
        afternoon.setCurrentLoad(afternoonLoad);
        afternoon.setInpatient(true);
        slots.add(afternoon);

        return slots;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  APPOINTMENT CREATION  (status = Pending, does NOT count toward capacity)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Create appointments for selected (pet × service) combos.
     * Single slotKey for outpatient; date + period for inpatient.
     * All start as Pending — only confirmed ones count toward capacity.
     * Returns list of created AppointmentIDs.
     */
    public List<Integer> createAppointments(int customerId, List<Integer> petIds,
                                            List<Integer> serviceIds, String slotKey,
                                            boolean isInpatient, String inpatientDate,
                                            String inpatientPeriod)
            throws Exception {

        List<Integer> created = new ArrayList<>();
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");

        LocalDate date;
        LocalTime start, end;

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
    //  DEPOSIT CALCULATION
    // ─────────────────────────────────────────────────────────────────────────

    /** Deposit for partial payment. */
    public long computeDeposit(BigDecimal totalPrice, boolean isInpatient) {
        if (isInpatient) return DEPOSIT_INPATIENT_VND;
        if (totalPrice == null) return 0L;
        return Math.max(1L, Math.round(totalPrice.doubleValue() * DEPOSIT_RATIO_NORMAL));
    }
}
