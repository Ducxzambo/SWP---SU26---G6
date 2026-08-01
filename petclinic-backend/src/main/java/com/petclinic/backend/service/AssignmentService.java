package com.petclinic.backend.service;

import com.petclinic.backend.dao.AppointmentDAO;
import com.petclinic.backend.dao.AppointmentServiceDAO;
import com.petclinic.backend.dao.ServiceDAO;
import com.petclinic.backend.dao.StaffDAO;
import com.petclinic.backend.model.Appointment;
import com.petclinic.backend.model.AppointmentService;
import com.petclinic.backend.model.Staff;

import java.time.LocalDate;
import java.util.*;
import java.util.logging.Logger;

public class AssignmentService {

    private static final Logger LOG = Logger.getLogger(AssignmentService.class.getName());

    private final AppointmentDAO        appointmentDAO        = new AppointmentDAO();
    private final AppointmentServiceDAO appointmentServiceDAO = new AppointmentServiceDAO();
    private final StaffDAO              staffDAO              = new StaffDAO();

    public void autoAssign(int appointmentId) {
        try {
            Appointment appt = appointmentDAO.findById(appointmentId);
            if (appt == null) return;

            // Gom cac dich vu CHUA duoc gan (bo qua Noi tru) theo nhom vai tro.
            Map<Integer, List<AppointmentService>> byRole = new LinkedHashMap<>();
            for (AppointmentService item : appt.getServices()) {
                if (item.getCategoryID() == BookingService.INPATIENT_CATEGORY_ID) continue;
                if (item.getAssignedStaffID() != null) continue;
                int roleId = ServiceDAO.roleIdForCategory(item.getCategoryID());
                byRole.computeIfAbsent(roleId, k -> new ArrayList<>()).add(item);
            }
            if (byRole.isEmpty()) return;

            Integer shift = appt.getSlotShift();
            if (shift == null) shift = BookingService.slotShiftOf(appt.getStartTime());

            for (Map.Entry<Integer, List<AppointmentService>> e : byRole.entrySet()) {
                int roleId = e.getKey();
                List<AppointmentService> items = e.getValue();

                List<Staff> candidates = staffDAO.findActiveByRole(roleId);
                if (candidates.isEmpty()) {
                    LOG.warning("Khong co nhan vien active cho roleId=" + roleId
                            + " (appointment " + appointmentId + ") — bo qua auto-assign nhom nay.");
                    continue;
                }

                Staff best = pickLeastLoaded(candidates, appt.getAppointmentDate(), shift);
                if (best == null) continue;

                for (AppointmentService item : items) {
                    appointmentServiceDAO.assignStaff(item.getAppointmentServiceID(), best.getStaffID());
                }
                LOG.info("Auto-assigned staff #" + best.getStaffID() + " (" + best.getFullName()
                        + ") cho " + items.size() + " dich vu (roleId=" + roleId
                        + ") cua appointment " + appointmentId);
            }
        } catch (Exception ex) {
            LOG.warning("autoAssign that bai cho appointment " + appointmentId + ": " + ex.getMessage());
        }
    }

    /** Chon nhan vien co tai (slot + ca ngay) it nhat trong danh sach candidates. */
    private Staff pickLeastLoaded(List<Staff> candidates, LocalDate date, Integer shift) throws Exception {
        Staff best = null;
        int bestSlotCount = Integer.MAX_VALUE;
        int bestDayCount  = Integer.MAX_VALUE;
        for (Staff s : candidates) {
            int slotCount = (shift != null) ? staffDAO.countAssignedInSlot(s.getStaffID(), date, shift) : 0;
            int dayCount  = staffDAO.countAssignedOnDate(s.getStaffID(), date);
            boolean better = best == null
                    || slotCount < bestSlotCount
                    || (slotCount == bestSlotCount && dayCount < bestDayCount);
            if (better) {
                best = s;
                bestSlotCount = slotCount;
                bestDayCount  = dayCount;
            }
        }
        return best;
    }
}
