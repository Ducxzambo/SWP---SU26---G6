package com.petclinic.backend.service;

import com.petclinic.backend.dao.AppointmentDAO;
import com.petclinic.backend.dao.AppointmentServiceDAO;
import com.petclinic.backend.dao.ServiceDAO;
import com.petclinic.backend.dao.ShiftCapacityDAO;
import com.petclinic.backend.dto.BookingSelection;
import com.petclinic.backend.dto.TimeSlot;
import com.petclinic.backend.model.*;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class BookingService {

    public  static final int    SLOT_MINUTES        = 120;
    public static final int    DAYS_AHEAD          = 7;

    public static final LocalTime[][] FIXED_SLOTS = {
            { LocalTime.of(8, 0),  LocalTime.of(10, 0) },
            { LocalTime.of(10, 0), LocalTime.of(12, 0) },
            { LocalTime.of(13, 30),LocalTime.of(15, 30) },
            { LocalTime.of(15, 30),LocalTime.of(17, 30) },
    };

    public static final LocalTime OVERTIME_SLOT_START = LocalTime.of(18, 30);
    public static final LocalTime OVERTIME_SLOT_END   = LocalTime.of(7, 0); // sang hom sau

    public static final LocalTime INPATIENT_MORNING_START   = LocalTime.of(8,  0);
    public static final LocalTime INPATIENT_MORNING_END     = LocalTime.of(12, 0);
    public static final LocalTime INPATIENT_AFTERNOON_START = LocalTime.of(13, 30);
    public static final LocalTime INPATIENT_AFTERNOON_END   = LocalTime.of(17, 30);

    public static final long DEPOSIT_INPATIENT = 200000;
    public static final double DEPOSIT_RATIO_NORMAL = 0.5;

    public static final int GROOMING_CATEGORY_ID  = 3;
    public static final int VACCINE_CATEGORY_ID   = 4;
    public static final int INPATIENT_CATEGORY_ID = 7; // "Dich vu noi tru"
    private static final int VET_ROLE_ID     = 3;
    private static final int GROOMER_ROLE_ID = 4;

    public static boolean isOvertimeSlotStart(LocalTime start) {
        return start != null && start.equals(OVERTIME_SLOT_START);
    }

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
    private final ShiftCapacityDAO shiftCapacityDAO      = new ShiftCapacityDAO();

    public Map<LocalDate, List<TimeSlot>> generateSlots(List<Integer> serviceIds) throws Exception {
        return generateSlotsInternal(serviceIds, -1, true);
    }

    public Map<LocalDate, List<TimeSlot>> generateSlotsForReschedule(
            List<Integer> serviceIds, int excludeAppointmentId) throws Exception {
        return generateSlotsInternal(serviceIds, excludeAppointmentId, true);
    }

    private Map<LocalDate, List<TimeSlot>> generateSlotsInternal(
            List<Integer> serviceIds, int excludeId, boolean applyDeadlineCutoff) throws Exception {


        List<Service> allServices = (serviceIds != null && !serviceIds.isEmpty())
                ? serviceDAO.findByIds(serviceIds) : Collections.emptyList();

        // service = grooming + vet
        List<Service> groomSvcs = new ArrayList<>();
        List<Service> vetSvcs   = new ArrayList<>();
        for (Service s : allServices) {
            if (s.getCategoryID() == GROOMING_CATEGORY_ID) groomSvcs.add(s);
            else                                            vetSvcs.add(s);
        }

        boolean vetNeeded   = !vetSvcs.isEmpty();
        boolean groomNeeded = !groomSvcs.isEmpty();

        boolean noSelectionYet = allServices.isEmpty();

        LocalDate today = LocalDate.now();
        LocalDate rangeEnd = today.plusDays(DAYS_AHEAD - 1);

        Map<LocalDate, Map<Integer, ShiftCapacity>> capacityMap;
        try {
            capacityMap = shiftCapacityDAO.findByDateRange(today, rangeEnd);
        } catch (Exception e) {
            capacityMap = new LinkedHashMap<>();
        }
        Map<LocalDate, List<TimeSlot>> result = new LinkedHashMap<>();

        for (int d = 0; d < DAYS_AHEAD; d++) {
            LocalDate date  = today.plusDays(d);
            if (applyDeadlineCutoff && !LocalDateTime.now().isBefore(Appointment.deadlineFor(date))) {
                continue;
            }
            Map<Integer, ShiftCapacity> dayCapacity = capacityMap.getOrDefault(date, Collections.emptyMap());
            List<TimeSlot> slots = new ArrayList<>();

            for (LocalTime[] win : FIXED_SLOTS) {
                LocalTime cursor  = win[0];
                LocalTime slotEnd = win[1];

                int slotShift = slotShiftOf(cursor);

                // set groom and vet capacity and load
                ShiftCapacity cap = dayCapacity.get(slotShift);
                int groomCap = (cap != null && groomNeeded) ? cap.getGroomCap() : 0;
                int vetCap   = (cap != null && vetNeeded)   ? cap.getVetCap()   : 0;

                int groomLoad = groomNeeded ? appointmentDAO.countConfirmedInSlotByRoleGroup(
                        date, slotShift, GROOMER_ROLE_ID) : 0;
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

    public int createInpatientAppointment(int customerId,
                                          int serviceId,
                                          String inpatientDate,
                                          String inpatientPeriod,
                                          Integer petId) throws Exception {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        LocalDate date = LocalDate.parse(inpatientDate, df);
        boolean isMorning = "morning".equalsIgnoreCase(inpatientPeriod);
        LocalTime start = isMorning ? INPATIENT_MORNING_START : INPATIENT_AFTERNOON_START;
        LocalTime end   = isMorning ? INPATIENT_MORNING_END   : INPATIENT_AFTERNOON_END;

        Service svc = serviceDAO.findById(serviceId);
        if (svc == null) {
            throw new IllegalStateException(
                    "Chưa cấu hình dịch vụ đại diện cho nhóm dịch vụ này (ServiceID=" + serviceId
                            + " không tồn tại hoặc đã ngừng hoạt động). Vui lòng liên hệ quản trị viên để thêm ít nhất 1 dịch vụ (IsActive=1) cho nhóm này.");
        }

        int apptId = insertAppointmentRow(customerId, date, start, end, null, petId);
        if (apptId <= 0) return -1;

        BigDecimal price = svc.getPrice() != null ? svc.getPrice() : BigDecimal.ZERO;
        appointmentServiceDAO.insert(apptId, serviceId, price);
        return apptId;
    }
    public int createNormalAppointment(int customerId, BookingSelection booking,
                                       String slotKey, Integer petId) throws Exception {
        if (booking == null || booking.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất 1 dịch vụ hoặc vaccine.");
        }

        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");

        String[] parts = slotKey.split("\\|");
        LocalDate date  = LocalDate.parse(parts[0], df);
        LocalTime start = LocalTime.parse(parts[1], tf);
        LocalTime end   = start.plusMinutes(SLOT_MINUTES);
        Integer   shift = slotShiftOf(start);

        int apptId = insertAppointmentRow(customerId, date, start, end, shift, petId);
        if (apptId <= 0) return -1;

        if (!booking.getServiceIds().isEmpty()) {
            List<Service> svcs = serviceDAO.findByIds(booking.getServiceIds());
            for (Service svc : svcs) {
                BigDecimal price = svc.getPrice() != null ? svc.getPrice() : BigDecimal.ZERO;
                appointmentServiceDAO.insert(apptId, svc.getServiceID(), price);
            }
        }
        if (!booking.getVaccineIds().isEmpty()) {
            Service vaccinePlaceholder = serviceDAO.findFirstActiveByCategory(VACCINE_CATEGORY_ID);
            if (vaccinePlaceholder != null) {
                BigDecimal price = vaccinePlaceholder.getPrice() != null
                        ? vaccinePlaceholder.getPrice() : BigDecimal.ZERO;
                appointmentServiceDAO.insert(apptId, vaccinePlaceholder.getServiceID(), price);
            }
        }

        return apptId;
    }

    private int insertAppointmentRow(int customerId, LocalDate date,
                                     LocalTime start, LocalTime end,
                                     Integer slotShift, Integer petId) throws Exception {
        Appointment a = new Appointment();
        a.setCustomerID(customerId);
        a.setPetID(petId);
        a.setAppointmentDate(date);
        a.setStartTime(start);
        a.setEndTime(end);
        a.setStatus("Pending");
        a.setSlotShift(slotShift);
        return appointmentDAO.insert(a);
    }

    // deposit
    public long computeDeposit(BigDecimal totalPrice, boolean isInpatient) {
        if (isInpatient) return DEPOSIT_INPATIENT;
        if (totalPrice == null) return 0;
        return Math.max(1, Math.round(totalPrice.doubleValue() * DEPOSIT_RATIO_NORMAL));
    }
}
