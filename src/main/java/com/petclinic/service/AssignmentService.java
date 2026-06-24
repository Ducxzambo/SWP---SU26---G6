package com.petclinic.service;

import com.petclinic.dao.AppointmentDAO;
import com.petclinic.dao.ServiceDAO;
import com.petclinic.dao.StaffDAO;
import com.petclinic.model.Appointment;
import com.petclinic.model.Service;
import com.petclinic.model.Staff;

import java.util.List;
import java.util.logging.Logger;

/**
 * Tự động gán Vet/Groomer cho appointment khi chuyển sang Confirmed.
 *
 * Quy tắc chọn:
 *  1. Ưu tiên nhân viên (cùng role với category của dịch vụ) đang được
 *     gán ÍT HƠN trong CHÍNH slot giờ đó (cùng ngày, cùng giờ bắt đầu/kết thúc).
 *  2. Nếu bằng nhau, chọn người được gán ÍT HƠN trong CẢ NGÀY đó.
 *
 * Role mapping giữ theo ServiceDAO.roleIdForCategory(categoryId):
 *  categoryId = 3 (Grooming) → roleId 4 (Groomer)
 *  categoryId khác           → roleId 3 (Vet) — bao gồm cả Vaccine (4)
 */
public class AssignmentService {

    private static final Logger LOG = Logger.getLogger(AssignmentService.class.getName());

    private final AppointmentDAO appointmentDAO = new AppointmentDAO();
    private final ServiceDAO     serviceDAO     = new ServiceDAO();
    private final StaffDAO       staffDAO       = new StaffDAO();

    /** Gọi sau khi appointment chuyển Status='Confirmed'. Không throw — lỗi chỉ log. */
    public void autoAssign(int appointmentId) {
        try {
            Appointment appt = appointmentDAO.findById(appointmentId);
            if (appt == null) return;
            if (appt.getAssignedStaffID() != null) return; // đã có người gán rồi thì bỏ qua

            Service svc = serviceDAO.findById(appt.getServiceID());
            if (svc == null) return;

            // Inpatient (category 5) GIỮ LOGIC CŨ — không tự gán ở bước Confirmed,
            // staff gán tay sau (qua luồng nhập viện/DailyAssessment, ngoài codebase này).
            if (svc.getCategoryID() == BookingService.INPATIENT_CATEGORY_ID) return;

            int roleId = ServiceDAO.roleIdForCategory(svc.getCategoryID());
            List<Staff> candidates = staffDAO.findActiveByRole(roleId);
            if (candidates.isEmpty()) {
                LOG.warning("[AssignmentService] Không có staff active cho roleId=" + roleId
                        + " (appt #" + appointmentId + ")");
                return;
            }

            // SlotShift có thể null với dữ liệu cũ/insert tay thiếu cột — suy ra lại
            // từ StartTime làm fallback để không vô tình loại các appointment đó
            // khỏi việc đếm tải khi tìm staff ít việc nhất.
            Integer shift = appt.getSlotShift();
            if (shift == null) shift = BookingService.slotShiftOf(appt.getStartTime());

            Staff best = null;
            int bestSlotCount = Integer.MAX_VALUE;
            int bestDayCount  = Integer.MAX_VALUE;

            for (Staff s : candidates) {
                int slotCount = (shift != null)
                        ? staffDAO.countAssignedInSlot(s.getStaffID(), appt.getAppointmentDate(), shift)
                        : 0; // không xác định được ca -> không phân biệt được, coi như 0
                int dayCount = staffDAO.countAssignedOnDate(s.getStaffID(), appt.getAppointmentDate());

                boolean better = best == null
                        || slotCount < bestSlotCount
                        || (slotCount == bestSlotCount && dayCount < bestDayCount);

                if (better) {
                    best = s; bestSlotCount = slotCount; bestDayCount = dayCount;
                }
            }

            if (best != null) {
                appointmentDAO.assignVet(appointmentId, best.getStaffID());
                LOG.info("[AssignmentService] Assigned staff #" + best.getStaffID()
                        + " (" + best.getFullName() + ") to appt #" + appointmentId);
            }
        } catch (Exception e) {
            LOG.warning("[AssignmentService] autoAssign failed for appt #" + appointmentId
                    + ": " + e.getMessage());
        }
    }
}