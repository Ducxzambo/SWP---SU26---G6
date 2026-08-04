package com.petclinic.backend.service;

import com.petclinic.backend.dao.*;
import com.petclinic.backend.model.*;
import com.petclinic.backend.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * BP-04/05 — Đồng bộ hóa đơn khi vet/groomer hoàn thành 1 phần việc của 1
 * appointment (thay cho stub "triggerInvoice" cũ ở ExaminationServlet/
 * GroomingServlet):
 *
 *  1. Thêm InvoiceItems cho các dòng phát sinh lúc khám mà CHƯA có trên hóa
 *     đơn — dịch vụ Chẩn đoán/Điều trị được vet CHỌN THÊM ngoài các dòng đã
 *     chọn lúc check-in (labTestID[]/treatmentID[] trên form khám), và thuốc
 *     kê đơn (PrescriptionItems — luôn là dòng mới, vì mỗi appointment chỉ
 *     lưu 1 bệnh án duy nhất).
 *  2. Trừ kho vaccine (-1) nếu appointment có dịch vụ thuộc nhóm Vaccine —
 *     áp dụng cho CẢ vet lẫn groomer hoàn thành (dùng chung
 *     deductVaccineStockIfNeeded), khớp với dòng InvoiceItems loại 'Vaccine'
 *     đã được ghi khi tạo hóa đơn (đối chiếu qua tên vaccine — xem
 *     VaccineDAO.findByName). Trừ kho THUỐC theo đơn đã được xử lý sẵn ngay
 *     trong MedicalRecordDAO.save() (transaction cùng lúc lưu bệnh án), nên
 *     KHÔNG lặp lại ở đây.
 *
 * Mọi lỗi trong quá trình đồng bộ đều được nuốt (log lại) — bệnh án/kết quả
 * grooming ĐÃ lưu thành công trước đó rồi, không để lỗi ở bước hóa đơn (vốn
 * có thể chỉnh tay) làm hỏng luồng chính của vet/groomer.
 */
public class InvoiceSyncService {

    private static final Logger LOG = Logger.getLogger(InvoiceSyncService.class.getName());

    private final AppointmentDAO appointmentDAO = new AppointmentDAO();
    private final ServiceDAO     serviceDAO     = new ServiceDAO();
    private final MedicineDAO    medicineDAO    = new MedicineDAO();
    private final VaccineDAO     vaccineDAO     = new VaccineDAO();
    private final InvoiceDAO     invoiceDAO     = new InvoiceDAO();
    private final PaymentService paymentService = new PaymentService();

    /**
     * Gọi SAU KHI vet lưu thành công bệnh án (ExaminationService.saveMedicalRecord
     * trả về SUCCESS).
     *
     * @param appointmentId      appointment vừa khám xong
     * @param staffId            vet đang thực hiện (dùng làm AssignedStaffID cho
     *                           dịch vụ mới + PerformedByID cho StockTransactions)
     * @param selectedServiceIds TOÀN BỘ ServiceID Chẩn đoán/Điều trị được chọn
     *                           trên form khám (labTestID[] + treatmentID[]) —
     *                           có thể gồm cả những dòng ĐÃ có từ lúc check-in
     *                           lẫn dòng MỚI chọn thêm; hàm tự lọc ra dòng mới.
     * @param prescriptionItems  danh sách thuốc kê đơn vừa lưu (đã có unitPrice
     *                           được resolve ở ExaminationService.saveMedicalRecord)
     */
    public void syncAfterExamination(int appointmentId, int staffId,
                                     List<Integer> selectedServiceIds,
                                     List<PrescriptionItem> prescriptionItems) {
        try {
            Appointment appt = appointmentDAO.findById(appointmentId);
            if (appt == null) return;

            Invoice invoice = getOrCreateInvoice(appt);
            if (invoice == null) return;

            addNewlySelectedServices(appointmentId, staffId, appt, invoice, selectedServiceIds);
            addPrescriptionItems(invoice, prescriptionItems);
            deductVaccineStockIfNeeded(appointmentId, staffId, invoice);

        } catch (Exception e) {
            LOG.warning("syncAfterExamination thất bại cho appointment " + appointmentId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gọi SAU KHI groomer lưu thành công kết quả grooming
     * (GroomingService.saveGroomingRecord trả về SUCCESS). Grooming không phát
     * sinh InvoiceItems mới (không có "đơn thuốc"/dịch vụ chọn thêm như vet) —
     * chỉ cần đồng bộ phần trừ kho vaccine cho appointment trộn Grooming + Vaccine.
     */
    public void syncAfterGrooming(int appointmentId, int staffId) {
        try {
            Appointment appt = appointmentDAO.findById(appointmentId);
            if (appt == null) return;

            Invoice invoice = getOrCreateInvoice(appt);
            if (invoice == null) return;

            deductVaccineStockIfNeeded(appointmentId, staffId, invoice);

        } catch (Exception e) {
            LOG.warning("syncAfterGrooming thất bại cho appointment " + appointmentId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── 1. Dịch vụ Chẩn đoán/Điều trị chọn thêm lúc khám ────────────────────

    private void addNewlySelectedServices(int appointmentId, int staffId, Appointment appt,
                                          Invoice invoice, List<Integer> selectedServiceIds) throws Exception {
        if (selectedServiceIds == null || selectedServiceIds.isEmpty()) return;

        Set<Integer> existingServiceIds = new LinkedHashSet<>();
        for (AppointmentService s : appt.getServices()) existingServiceIds.add(s.getServiceID());

        for (Integer sid : selectedServiceIds) {
            if (sid == null || sid <= 0 || existingServiceIds.contains(sid)) continue;

            Service svc = serviceDAO.findById(sid);
            if (svc == null) continue;

            BigDecimal price = svc.getPrice() != null ? svc.getPrice() : BigDecimal.ZERO;
            appointmentDAO.addServiceToAppointment(appointmentId, sid, price, staffId);
            invoiceDAO.addInvoiceItemAndGrowTotal(invoice.getInvoiceID(), "Service", svc.getName(),
                    BigDecimal.ONE, price);

            existingServiceIds.add(sid); // tránh thêm trùng nếu labTestID[]/treatmentID[] có ID lặp
        }
    }

    // ── 2. Thuốc kê đơn ──────────────────────────────────────────────────────

    private void addPrescriptionItems(Invoice invoice, List<PrescriptionItem> items) throws SQLException {
        if (items == null || items.isEmpty()) return;

        for (PrescriptionItem item : items) {
            if (item.getMedicineID() <= 0) continue;

            BigDecimal qty   = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ONE;
            BigDecimal price = item.getUnitPrice();
            String     name  = item.getMedicineName();

            if (price == null || name == null) {
                Medicine med = medicineDAO.findById(item.getMedicineID());
                if (med != null) {
                    if (price == null) price = med.getUnitPrice();
                    if (name == null)  name  = med.getName();
                }
            }
            if (price == null) price = BigDecimal.ZERO;
            if (name == null)  name  = "Thuốc #" + item.getMedicineID();

            invoiceDAO.addInvoiceItemAndGrowTotal(invoice.getInvoiceID(), "Medicine", name, qty, price);
        }
    }

    // ── 3. Trừ kho vaccine ───────────────────────────────────────────────────

    /**
     * Trừ 1 đơn vị vaccine cho MỖI dòng InvoiceItems loại 'Vaccine' trên hóa
     * đơn (đối chiếu tên vaccine với bảng Vaccines — xem VaccineDAO.findByName),
     * được ghi từ lúc tạo hóa đơn (booking online của khách hàng — xem
     * ConfirmServlet). Có guard chống trừ trùng (StockTransactions.Reason
     * mã hóa AppointmentID + VaccineID) để an toàn khi vet VÀ groomer cùng
     * hoàn thành trên 1 appointment trộn nhiều category dịch vụ.
     *
     * Ghi chú phạm vi: lịch hẹn OFFLINE do lễ tân tạo (walk-in — BP-04) chỉ
     * chọn được category "Vaccine" chung chung (không chọn đúng loại vaccine
     * cụ thể như lúc đặt lịch online), nên KHÔNG có dòng InvoiceItems loại
     * 'Vaccine' để đối chiếu — trường hợp này chưa trừ được kho vaccine cụ
     * thể nào (không có dữ liệu để xác định đúng loại).
     */
    private void deductVaccineStockIfNeeded(int appointmentId, int staffId, Invoice invoice) throws SQLException {
        if (invoice.getItems() == null) return;

        Set<String> processedVaccineNames = new LinkedHashSet<>();
        for (InvoiceItem item : invoice.getItems()) {
            if (!"Vaccine".equals(item.getItemType())) continue;
            String vaccineName = item.getDescription();
            if (vaccineName == null || !processedVaccineNames.add(vaccineName)) continue;

            try (Connection c = DBConnection.getConnection()) {
                Vaccine v = vaccineDAO.findByName(c, vaccineName);
                if (v == null) continue;

                String reason = "Used - AppointmentID=" + appointmentId + " - VaccineID=" + v.getVaccineID();
                if (stockTransactionExists(c, reason)) continue;

                vaccineDAO.deductStock(c, v.getVaccineID(), 1);
                insertVaccineStockTransaction(c, v.getVaccineID(), staffId, reason);
            }
        }
    }

    private boolean stockTransactionExists(Connection c, String reason) throws SQLException {
        String sql = "SELECT COUNT(*) FROM StockTransactions WHERE ItemType = 'Vaccine' AND Reason = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, reason);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private void insertVaccineStockTransaction(Connection c, int vaccineId, int staffId, String reason)
            throws SQLException {
        String sql = "INSERT INTO StockTransactions " +
                "(ItemType, ItemID, QuantityChange, Reason, PerformedByID, TransactionType) " +
                "VALUES ('Vaccine', ?, -1, ?, ?, 'Export')";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, vaccineId);
            ps.setString(2, reason);
            ps.setInt(3, staffId);
            ps.executeUpdate();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Lấy Invoice của appointment; trường hợp hiếm chưa có (dữ liệu cũ trước
     * khi có BP-04, hoặc appointment tạo theo luồng khác chưa qua bước tạo
     * hóa đơn) thì tạo 1 invoice rỗng (TotalAmount=0) để có chỗ gắn các dòng
     * phát sinh — invoice này sẽ tăng dần qua addInvoiceItemAndGrowTotal.
     */
    private Invoice getOrCreateInvoice(Appointment appt) throws Exception {
        Invoice invoice = invoiceDAO.findByAppointment(appt.getAppointmentID());
        if (invoice != null) return invoice;

        int invoiceId = paymentService.createInvoice(appt.getCustomerID(), appt.getAppointmentID(), BigDecimal.ZERO);
        if (invoiceId <= 0) return null;
        return invoiceDAO.findById(invoiceId);
    }
}
