package com.petclinic.backend.service;

import com.petclinic.backend.dao.*;
import com.petclinic.backend.model.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ExaminationService {

    public static final String CAT_LAB_TEST  = "Chẩn đoán";
    public static final String CAT_TREATMENT = "Điều trị";
    public static final String CAT_GROOMING  = "Grooming";

    private final AppointmentDAO appointmentDAO   = new AppointmentDAO();
    private final MedicalRecordDAO medicalRecordDAO = new MedicalRecordDAO();
    private final MedicineDAO      medicineDAO      = new MedicineDAO();
    private final ServiceDAO       serviceDAO       = new ServiceDAO();
    private final CustomerDAO      customerDAO      = new CustomerDAO();
    private final PetDAO           petDAO           = new PetDAO();
    private final StaffDAO         staffDAO         = new StaffDAO();
    private final PaymentService   paymentService   = new PaymentService();
    private final AssignmentService assignmentSvc = new AssignmentService();
    private final SupplyDAO    supplyDAO    = new SupplyDAO();

    //CHECK-IN
    public enum CheckInResult { SUCCESS, NOT_FOUND, WRONG_STATUS, ALREADY_CHECKED_IN }

    /**
     * Check-in đơn thuần: đổi Confirmed → Arrived. KHÔNG còn gán staff kèm theo
     * (vì staff giờ gán riêng theo từng dòng dịch vụ — dùng assignStaffToServiceLine
     * hoặc assignStaffToCategory sau khi check-in).
     */
    public CheckInResult checkIn(int appointmentID) throws SQLException {
        Appointment appt = appointmentDAO.findById(appointmentID);
        if (appt == null)                          return CheckInResult.NOT_FOUND;
        if ("Arrived".equals(appt.getStatus()))    return CheckInResult.ALREADY_CHECKED_IN;
        if (!"Confirmed".equals(appt.getStatus())) return CheckInResult.WRONG_STATUS;

        appointmentDAO.updateStatus(appointmentID, "Arrived");

        // Auto-assign: với mỗi category trong appointment, gán staff ít lịch nhất
        java.util.Set<String> categories = new java.util.LinkedHashSet<>();
        for (AppointmentService s : appt.getServices()) {
            if (s.getCategoryName() != null) categories.add(s.getCategoryName());
        }
        for (String cat : categories) {
            Integer staffID = appointmentDAO.findLeastLoadedStaffByCategory(cat, LocalDate.now());
            if (staffID != null) {
                appointmentDAO.assignStaffToCategory(appointmentID, cat, staffID);
            }
        }
        return CheckInResult.SUCCESS;
    }

    /** Lễ tân đổi staff phụ trách toàn bộ dịch vụ của 1 category trong appointment. */
    public void reassignStaffByCategory(int appointmentID, String categoryName, int newStaffID)
            throws SQLException {
        appointmentDAO.assignStaffToCategory(appointmentID, categoryName, newStaffID);
    }

    /** Gán 1 nhân viên cho 1 dòng dịch vụ cụ thể trong appointment (dùng ở bảng check-in). */
    public void assignStaffToServiceLine(int appointmentServiceID, int staffID) throws SQLException {
        appointmentDAO.assignStaffToService(appointmentServiceID, staffID);
    }

    /** Gán 1 nhân viên cho TOÀN BỘ dịch vụ thuộc 1 category trong appointment (gán nhanh hàng loạt). */
    public void assignStaffToCategory(int appointmentID, String categoryName, int staffID) throws SQLException {
        appointmentDAO.assignStaffToCategory(appointmentID, categoryName, staffID);
    }

    //WALK-IN: LOOKUP CUSTOMER BY PHONE
    public Customer findCustomerByPhone(String phone) throws SQLException {
        return customerDAO.findByPhone(phone);
    }

    public List<Pet> getPetsByCustomer(int customerID) throws SQLException {
        return petDAO.findByCustomerId(customerID);
    }

    //WALK-IN: TẠO LỊCH HẸN NHIỀU DỊCH VỤ, MỖI DỊCH VỤ 1 STAFF RIÊNG

    /**
     * Walk-in cho khách/pet ĐÃ tồn tại. serviceIDs và staffIDs đi song song theo index
     * (staffIDs[i] có thể null nếu chưa gán). Các service có thể thuộc NHIỀU category
     * khác nhau trong cùng 1 lần gọi (VD vừa khám vừa grooming).
     * Trả về appointmentID, hoặc -1 nếu ca hiện tại đã đầy slot.
     */
    public int createWalkInExisting(int customerID, int petID,
                                    List<Integer> serviceIDs, List<Integer> staffIDs) throws SQLException {
        LocalDate today = LocalDate.now();
        int shift = AppointmentDAO.shiftOf(LocalTime.now());
        if (shift == -1) shift = 1;
        if (appointmentDAO.isSlotFull(today, shift)) return -1;

        List<BigDecimal> unitPrices = resolveUnitPrices(serviceIDs);
        return appointmentDAO.createWalkIn(customerID, petID, serviceIDs, unitPrices, staffIDs);
    }

    /** Walk-in cho khách ĐÃ tồn tại nhưng thú cưng MỚI (chưa có trong hệ thống). */
    public int createWalkInWithNewPet(int customerID, String petName, String species, String breed,
                                      List<Integer> serviceIDs, List<Integer> staffIDs) throws SQLException {
        LocalDate today = LocalDate.now();
        int shift = AppointmentDAO.shiftOf(LocalTime.now());
        if (shift == -1) shift = 1;
        if (appointmentDAO.isSlotFull(today, shift)) return -1;

        Pet pet = new Pet();
        pet.setCustomerID(customerID);
        pet.setName(petName);
        pet.setSpeciesName(species);
        pet.setBreedName(breed);
        int petID = petDAO.insert(pet);

        List<BigDecimal> unitPrices = resolveUnitPrices(serviceIDs);
        return appointmentDAO.createWalkIn(customerID, petID, serviceIDs, unitPrices, staffIDs);
    }

    /** Walk-in cho khách HOÀN TOÀN mới (chưa có SĐT trong hệ thống). */
    public int createWalkInWithNewCustomer(String fullName, String phone,
                                           String petName, String species, String breed,
                                           List<Integer> serviceIDs, List<Integer> staffIDs) throws SQLException {
        LocalDate today = LocalDate.now();
        int shift = AppointmentDAO.shiftOf(LocalTime.now());
        if (shift == -1) shift = 1;
        if (appointmentDAO.isSlotFull(today, shift)) return -1;

        int customerID = customerDAO.insertWalkIn(fullName, phone);

        Pet pet = new Pet();
        pet.setCustomerID(customerID);
        pet.setName(petName);
        pet.setSpeciesName(species);
        pet.setBreedName(breed);
        int petID = petDAO.insert(pet);

        List<BigDecimal> unitPrices = resolveUnitPrices(serviceIDs);
        return appointmentDAO.createWalkIn(customerID, petID, serviceIDs, unitPrices, staffIDs);
    }

    /** Lấy giá hiện tại của từng ServiceID để snapshot vào AppointmentServices.UnitPrice. */
    private List<BigDecimal> resolveUnitPrices(List<Integer> serviceIDs) throws SQLException {
        List<BigDecimal> prices = new ArrayList<>();
        for (int sid : serviceIDs) {
            Service s = serviceDAO.findById(sid);
            prices.add(s != null && s.getPrice() != null ? s.getPrice() : BigDecimal.ZERO);
        }
        return prices;
    }

    /**
     * BP-04 — Sau khi walk-in "Tạo & Check-in" thành công (Appointments +
     * AppointmentServices đã có), tạo luôn Invoice + InvoiceItems tương ứng
     * với các dịch vụ đã chọn, để chuyển sang màn hình tổng hợp hóa đơn thu
     * tiền ngay (tiền mặt/chuyển khoản). customerID được lấy lại từ chính
     * appointment vừa tạo (đơn giản hóa lời gọi cho 3 nhánh walk-in ở trên,
     * kể cả nhánh tạo khách/thú cưng mới).
     */
    public int createInvoiceForWalkIn(int appointmentID, List<Integer> serviceIDs) throws Exception {
        Appointment appt = appointmentDAO.findById(appointmentID);
        if (appt == null) throw new IllegalStateException("Không tìm thấy lịch hẹn #" + appointmentID);

        List<Service> services = new ArrayList<>();
        for (int sid : serviceIDs) {
            Service s = serviceDAO.findById(sid);
            if (s != null) services.add(s);
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Service s : services) {
            if (s.getPrice() != null) total = total.add(s.getPrice());
        }

        int invoiceId = paymentService.createInvoice(appt.getCustomerID(), appointmentID, total);
        for (Service s : services) {
            BigDecimal price = s.getPrice() != null ? s.getPrice() : BigDecimal.ZERO;
            paymentService.addInvoiceItem(invoiceId, "Service", s.getName(), BigDecimal.ONE, price);
        }
        return invoiceId;
    }

    //  SLOT INFO
    public int getCurrentShiftCount() throws SQLException {
        int shift = AppointmentDAO.shiftOf(LocalTime.now());
        if (shift == -1) return 0;
        return appointmentDAO.countSlotBookings(LocalDate.now(), shift);
    }

    public boolean isCurrentShiftFull() throws SQLException {
        int shift = AppointmentDAO.shiftOf(LocalTime.now());
        if (shift == -1) return false;
        return appointmentDAO.isSlotFull(LocalDate.now(), shift);
    }

    //VET QUEUE
    public enum StartExamResult { SUCCESS, NOT_FOUND, WRONG_STATUS }

    public StartExamResult startExamination(int appointmentID, int vetID) throws SQLException {
        Appointment appt = appointmentDAO.findById(appointmentID);
        if (appt == null)                        return StartExamResult.NOT_FOUND;
        if (!"Arrived".equals(appt.getStatus())) return StartExamResult.WRONG_STATUS;
        appointmentDAO.updateStatus(appointmentID, "InProgress");
        return StartExamResult.SUCCESS;
    }

    public enum CompleteResult { SUCCESS, NOT_FOUND, WRONG_STATUS, NO_RECORD }

    /** Bác sĩ xác nhận hoàn thành khám: chỉ chuyển status → Done, không save thêm gì. */
    public CompleteResult completeExamination(int appointmentID) throws SQLException {
        Appointment appt = appointmentDAO.findById(appointmentID);
        if (appt == null)                            return CompleteResult.NOT_FOUND;
        if (!"InProgress".equals(appt.getStatus())) return CompleteResult.WRONG_STATUS;
        if (medicalRecordDAO.findByAppointmentId(appointmentID) == null)
            return CompleteResult.NO_RECORD;
        appointmentDAO.updateStatus(appointmentID, "Done");
        return CompleteResult.SUCCESS;
    }

    // ══ SAVE MEDICAL RECORD ═══════════════════════════════════════════════════
    public enum SaveRecordResult {
        SUCCESS, APPOINTMENT_NOT_FOUND, WRONG_STATUS,
        RECORD_ALREADY_EXISTS, INSUFFICIENT_STOCK, DB_ERROR, INSUFFICIENT_SUPPLY_STOCK
    }

    public SaveRecordResult saveMedicalRecord(MedicalRecord record,
                                              List<PrescriptionItem> items,
                                              List<SupplyUsageItem> supplies,
                                              String followUpDate) throws SQLException {
        Appointment appt = appointmentDAO.findById(record.getAppointmentID());
        if (appt == null)                           return SaveRecordResult.APPOINTMENT_NOT_FOUND;
        if (!"InProgress".equals(appt.getStatus())) return SaveRecordResult.WRONG_STATUS;
        if (medicalRecordDAO.findByAppointmentId(record.getAppointmentID()) != null)
            return SaveRecordResult.RECORD_ALREADY_EXISTS;

        if (followUpDate != null && !followUpDate.isBlank()) {
            String plan = record.getTreatmentPlan() == null ? "" : record.getTreatmentPlan();
            record.setTreatmentPlan(plan + "\nTai kham: " + followUpDate);
        }

        if (items != null) {
            for (PrescriptionItem item : items) {
                if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(BigDecimal.ZERO) == 0) {
                    Medicine med = medicineDAO.findById(item.getMedicineID());
                    if (med != null) item.setUnitPrice(med.getUnitPrice());
                }
            }
        }

        // Snapshot giá vật tư trước khi lưu
        if (supplies != null) {
            for (SupplyUsageItem sup : supplies) {
                if (sup.getUnitPrice() == null || sup.getUnitPrice().compareTo(BigDecimal.ZERO) == 0) {
                    Supply s = supplyDAO.findById(sup.getSupplyID());
                    if (s != null) sup.setUnitPrice(s.getUnitPrice());
                }
            }
        }

        try {
            medicalRecordDAO.save(record, items);
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("Insufficient stock"))
                return SaveRecordResult.INSUFFICIENT_STOCK;
            throw e;
        }

        // Lưu vật tư tiêu hao (tách transaction riêng, sau khi MedicalRecord đã commit)
        if (supplies != null && !supplies.isEmpty()) {
            try {
                supplyDAO.saveUsageItems(record.getAppointmentID(), record.getStaffID(), supplies);
            } catch (SQLException e) {
                if (e.getMessage() != null && e.getMessage().contains("Insufficient stock"))
                    return SaveRecordResult.INSUFFICIENT_SUPPLY_STOCK;
                throw e;
            }
        }

        return SaveRecordResult.SUCCESS;
    }

    // ══ QUERY HELPERS ═════════════════════════════════════════════════════════
    public List<Appointment> getConfirmedByDate(LocalDate date, Integer shift, String categoryFilter) throws SQLException {
        return appointmentDAO.findConfirmedByDate(date == null ? LocalDate.now() : date, shift, categoryFilter);
    }

    public List<Appointment> searchForCheckIn(String keyword, LocalDate date, String categoryFilter) throws SQLException {
        return appointmentDAO.searchForCheckIn(keyword, date == null ? LocalDate.now() : date, categoryFilter);
    }

    /**
     * Hàng chờ bác sĩ: Arrived/InProgress có dịch vụ Chẩn đoán/Phác đồ gán cho vetID,
     * LOẠI TRỪ appointment mà vet này ĐÃ lưu MedicalRecord rồi (đã xong phần việc
     * của mình, dù Status vẫn "InProgress" chờ lễ tân xác nhận Done).
     */
    public List<Appointment> getVetQueue(int vetID, LocalDate date) throws SQLException {
        LocalDate d = date == null ? LocalDate.now() : date;
        List<Appointment> result = appointmentDAO.findStaffQueue(vetID, d, CAT_LAB_TEST, "MedicalRecords");
        mergeDistinctById(result, appointmentDAO.findStaffQueue(vetID, d, CAT_TREATMENT, "MedicalRecords"));
        mergeDistinctById(result, appointmentDAO.findStaffQueue(vetID, d, "Khám & Điều Trị Bệnh", "MedicalRecords"));
        return result;
    }

    /**
     * Các ca mà bác sĩ này ĐÃ lưu xong MedicalRecord trong 1 ngày — để xem lại
     * bệnh án. Dựa trên "record đã tồn tại", KHÔNG dựa Status='Done' (vì
     * appointment có thể còn dịch vụ Grooming khác đang chờ groomer xử lý).
     */
    public List<Appointment> getVetCompletedToday(int vetID, LocalDate date) throws SQLException {
        LocalDate d = date == null ? LocalDate.now() : date;
        List<Appointment> result = appointmentDAO.findStaffCompletedToday(vetID, d, CAT_LAB_TEST, "MedicalRecords");
        List<Appointment> treatDone = appointmentDAO.findStaffCompletedToday(vetID, d, CAT_TREATMENT, "MedicalRecords");
        mergeDistinctById(result, treatDone);
        mergeDistinctById(result, appointmentDAO.findStaffCompletedToday(vetID, d, "Khám & Điều Trị Bệnh", "MedicalRecords"));
        return result;
    }

    private void mergeDistinctById(List<Appointment> base, List<Appointment> extra) {
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        for (Appointment a : base) ids.add(a.getAppointmentID());
        for (Appointment a : extra) if (ids.add(a.getAppointmentID())) base.add(a);
    }

    public List<MedicalRecord> getPetMedicalHistory(int petID) throws SQLException {
        return medicalRecordDAO.findHistoryByPetId(petID);
    }

    public MedicalRecord getMedicalRecord(int recordID) throws SQLException {
        return medicalRecordDAO.findById(recordID);
    }

    public List<Medicine> getMedicinesInStock() throws SQLException {
        return medicineDAO.findAllInStock();
    }

    public Appointment getAppointment(int appointmentID) throws SQLException {
        return appointmentDAO.findById(appointmentID);
    }

    public List<Service> getLabTests() throws SQLException {
        return serviceDAO.findLabTests();
    }

    public List<Service> getTreatmentPlans() throws SQLException {
        return serviceDAO.findTreatmentPlans();
    }

    /** Tất cả dịch vụ (mọi category) — dùng cho walk-in đầy đủ. */
    public List<Service> getAllActiveServices() throws SQLException {
        return serviceDAO.findAllActive();
    }



    //LỊCH SỬ LỊCH HẸN + HOÀN TẤT (Receptionist)

    /** Toàn bộ lịch hẹn trong 1 ngày (mọi trạng thái) — dùng cho tab "Lịch sử" của lễ tân. */
    public List<Appointment> getAppointmentHistory(LocalDate date, Integer shift) throws SQLException {
        return appointmentDAO.findAppointmentHistory(date == null ? LocalDate.now() : date, shift);
    }

    public enum FinalizeResult { SUCCESS, NOT_FOUND, WRONG_STATUS, NOT_READY }

    /**
     * Lễ tân xác nhận hoàn tất 1 lịch hẹn. Chỉ cho phép khi MỌI category dịch vụ
     * trong appointment đã có record tương ứng (MedicalRecord cho Khám,
     * GroomingRecord cho Grooming). Nếu còn thiếu, trả về NOT_READY kèm danh
     * sách category còn thiếu để servlet hiển thị thông báo cụ thể.
     */
    public FinalizeResult finalizeAppointment(int appointmentID) throws SQLException {
        return finalizeAppointment(appointmentID, null);
    }

    /** Overload cho phép lấy ra danh sách category còn thiếu (để hiển thị chi tiết). */
    public FinalizeResult finalizeAppointment(int appointmentID, List<String> outMissingCategories) throws SQLException {
        Appointment appt = appointmentDAO.findById(appointmentID);
        if (appt == null) return FinalizeResult.NOT_FOUND;
        if (!"Arrived".equals(appt.getStatus()) && !"InProgress".equals(appt.getStatus()))
            return FinalizeResult.WRONG_STATUS;

        List<String> missing = appointmentDAO.findMissingRecordCategories(appointmentID);
        if (!missing.isEmpty()) {
            if (outMissingCategories != null) outMissingCategories.addAll(missing);
            return FinalizeResult.NOT_READY;
        }

        boolean updated = appointmentDAO.finalizeAppointment(appointmentID);
        return updated ? FinalizeResult.SUCCESS : FinalizeResult.WRONG_STATUS;
    }

    //VẬT TƯ TIÊU HAO

    /** Danh sách vật tư còn hàng — dùng cho dropdown trong form grooming/khám. */
    public List<Supply> getSuppliesInStock() throws SQLException {
        return supplyDAO.findAllInStock();
    }

    /**
     * Lưu vật tư đã dùng trong 1 appointment (trừ kho + ghi StockTransaction).
     * Gọi sau khi groomer/vet submit form.
     */
    public void saveSupplyUsage(int appointmentID, int staffID,
                                List<SupplyUsageItem> items) throws SQLException {
        supplyDAO.saveUsageItems(appointmentID, staffID, items);
    }

    /** Lấy danh sách vật tư đã dùng của 1 appointment — để hiện trên hóa đơn. */
    public List<SupplyUsageItem> getSupplyUsage(int appointmentID) throws SQLException {
        return supplyDAO.findByAppointmentId(appointmentID);
    }

    /** Tổng tiền vật tư của 1 appointment — để cộng vào InvoiceItems. */
    public java.math.BigDecimal getSupplyTotal(int appointmentID) throws SQLException {
        return supplyDAO.sumByAppointmentId(appointmentID);
    }
}