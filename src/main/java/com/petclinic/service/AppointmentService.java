package com.petclinic.service;

import com.petclinic.dao.*;
import com.petclinic.model.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AppointmentService {

    private static final int    SLOT_WORK_MIN = 25;
    private static final int    SLOT_STRIDE   = 30;
    private static final LocalTime LUNCH_START = LocalTime.of(12, 0);
    private static final LocalTime LUNCH_END   = LocalTime.of(13, 30);
    private static final int    HOURS_LOCK    = 12; // cannot modify within 12h of appointment
    private static final int    DAYS_AHEAD    = 30;

    private final AppointmentDAO  appointmentDAO  = new AppointmentDAO();
    private final MedicalRecordDAO medicalDAO     = new MedicalRecordDAO();
    private final InvoiceDAO       invoiceDAO     = new InvoiceDAO();
    private final ServiceDAO       serviceDAO     = new ServiceDAO();

    // ── List ─────────────────────────────────────────────────────────────────

    public List<Appointment> getForCustomer(int customerId) throws Exception {
        return appointmentDAO.findByCustomer(customerId);
    }

    // ── Detail ────────────────────────────────────────────────────────────────

    public Appointment getDetail(int appointmentId, int customerId) throws Exception {
        Appointment a = appointmentDAO.findById(appointmentId);
        if (a == null || a.getCustomerID() != customerId) return null;
        return a;
    }

    public MedicalRecord getMedicalRecord(int appointmentId) throws Exception {
        return medicalDAO.findByAppointment(appointmentId);
    }

    public Invoice getInvoice(int appointmentId) throws Exception {
        return invoiceDAO.findByAppointment(appointmentId);
    }

    // ── Cancel ────────────────────────────────────────────────────────────────

    public enum CancelResult { SUCCESS, NOT_FOUND, FORBIDDEN, TOO_LATE }

    public CancelResult cancel(int appointmentId, int customerId, String reason) throws Exception {
        Appointment a = appointmentDAO.findById(appointmentId);
        if (a == null || a.getCustomerID() != customerId) return CancelResult.NOT_FOUND;
        if (!"Pending".equals(a.getStatus()) && !"Confirmed".equals(a.getStatus()))
            return CancelResult.FORBIDDEN;
        if (!a.canModify()) return CancelResult.TOO_LATE;
        appointmentDAO.cancel(appointmentId, reason != null ? reason : "");
        return CancelResult.SUCCESS;
    }

    // ── Reschedule: slot generation (excludes current appointment from conflicts)

    public Map<LocalDate, List<TimeSlot>> generateRescheduleSlots(int excludeAppointmentId)
            throws Exception {

        List<StaffAvailability> vetAvail = appointmentDAO.findVetAvailability();

        Map<Integer, List<LocalTime[]>> windowsByDay = new TreeMap<>();
        for (StaffAvailability sa : vetAvail) {
            windowsByDay.computeIfAbsent(sa.getDayOfWeek(), k -> new ArrayList<>())
                    .add(new LocalTime[]{sa.getStartTime(), sa.getEndTime()});
        }

        // Cutoff: must be at least 12h from now
        LocalDateTime cutoff = LocalDateTime.now().plusHours(HOURS_LOCK);
        LocalDate     today  = LocalDate.now();

        Map<LocalDate, List<TimeSlot>> result = new LinkedHashMap<>();
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");

        for (int d = 0; d < DAYS_AHEAD; d++) {
            LocalDate date  = today.plusDays(d);
            int       dbDow = date.getDayOfWeek() == DayOfWeek.SUNDAY ? 0
                    : date.getDayOfWeek().getValue();

            List<LocalTime[]> windows = windowsByDay.get(dbDow);
            if (windows == null || windows.isEmpty()) continue;

            // Booked times excluding the appointment being rescheduled
            Set<String> booked = new HashSet<>();
            for (Appointment a : appointmentDAO.findByDateExcluding(date, excludeAppointmentId)) {
                LocalTime t = a.getStartTime();
                while (t.isBefore(a.getEndTime())) {
                    booked.add(date + "|" + t.format(tf));
                    t = t.plusMinutes(SLOT_STRIDE);
                }
            }

            List<TimeSlot> slots = new ArrayList<>();
            for (LocalTime[] win : windows) {
                LocalTime cursor = win[0];
                while (true) {
                    LocalTime slotEnd = cursor.plusMinutes(SLOT_WORK_MIN);
                    if (slotEnd.isAfter(win[1])) break;

                    // Lunch skip
                    if (!cursor.isBefore(LUNCH_START) && cursor.isBefore(LUNCH_END)) {
                        cursor = LUNCH_END; continue;
                    }
                    if (cursor.isBefore(LUNCH_START) && slotEnd.isAfter(LUNCH_START)) {
                        cursor = LUNCH_END; continue;
                    }

                    // 12h cutoff: slot must start after cutoff
                    LocalDateTime slotDt = LocalDateTime.of(date, cursor);
                    boolean available = !booked.contains(date + "|" + cursor.format(tf))
                            && slotDt.isAfter(cutoff);

                    slots.add(new TimeSlot(date, cursor, slotEnd, available));
                    cursor = cursor.plusMinutes(SLOT_STRIDE);
                }
            }
            if (!slots.isEmpty()) result.put(date, slots);
        }
        return result;
    }

    // ── Reschedule: apply ─────────────────────────────────────────────────────

    public enum RescheduleResult { SUCCESS, NOT_FOUND, FORBIDDEN, TOO_LATE, SLOT_TAKEN, INVALID_SLOT }

    public RescheduleResult reschedule(int appointmentId, int customerId, String slotKey)
            throws Exception {

        Appointment a = appointmentDAO.findById(appointmentId);
        if (a == null || a.getCustomerID() != customerId) return RescheduleResult.NOT_FOUND;
        if (!"Pending".equals(a.getStatus()) && !"Confirmed".equals(a.getStatus()))
            return RescheduleResult.FORBIDDEN;
        if (!a.canModify()) return RescheduleResult.TOO_LATE;

        // Parse slotKey "yyyy-MM-dd|HH:mm"
        String[] parts;
        LocalDate newDate;
        LocalTime newStart, newEnd;
        try {
            parts    = slotKey.split("\\|");
            newDate  = LocalDate.parse(parts[0], DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            newStart = LocalTime.parse(parts[1], DateTimeFormatter.ofPattern("HH:mm"));
            newEnd   = newStart.plusMinutes(SLOT_WORK_MIN);
        } catch (Exception e) {
            return RescheduleResult.INVALID_SLOT;
        }

        // 12h cutoff from now
        if (!LocalDateTime.of(newDate, newStart).isAfter(LocalDateTime.now().plusHours(HOURS_LOCK)))
            return RescheduleResult.TOO_LATE;

        // Conflict check (excluding this appointment)
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");
        Set<String> booked = new HashSet<>();
        for (Appointment other : appointmentDAO.findByDateExcluding(newDate, appointmentId)) {
            LocalTime t = other.getStartTime();
            while (t.isBefore(other.getEndTime())) {
                booked.add(t.format(tf));
                t = t.plusMinutes(SLOT_STRIDE);
            }
        }
        if (booked.contains(newStart.format(tf))) return RescheduleResult.SLOT_TAKEN;

        appointmentDAO.updateSlot(appointmentId, newDate, newStart, newEnd);
        return RescheduleResult.SUCCESS;
    }
}