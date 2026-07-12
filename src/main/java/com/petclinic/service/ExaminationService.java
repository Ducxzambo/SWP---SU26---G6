package com.petclinic.service;

import com.petclinic.dao.*;
import com.petclinic.model.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ExaminationService {

    public static final String CAT_LAB_TEST  = "Chẩn đoán";
    public static final String CAT_TREATMENT = "Phác đồ điều trị";
    public static final String CAT_GROOMING  = "Grooming";

    private final AppointmentDAO   appointmentDAO   = new AppointmentDAO();
    private final MedicalRecordDAO medicalRecordDAO = new MedicalRecordDAO();
    private final MedicineDAO      medicineDAO      = new MedicineDAO();
    private final ServiceDAO       serviceDAO       = new ServiceDAO();
    private final CustomerDAO      customerDAO      = new CustomerDAO();
    private final PetDAO           petDAO           = new PetDAO();
    private final StaffDAO         staffDAO         = new StaffDAO();

    // ══ CHECK-IN ══════════════════════════════════════════════════════════════
    public enum CheckInResult { SUCCESS, NOT_FOUND, WRONG_STATUS, ALREADY_CHECKED_IN }

    /**
     * Check-in đơn thuần: đổi Confirmed → Arrived. KHÔNG còn gán staff kèm theo
     * (vì staff giờ gán riêng theo từng dòng dịch vụ — dùng assignStaffToServiceLine
     * hoặc assignStaffToCategory sau khi check-in).
     */
    public CheckInResult checkIn(int appointmentID) throws SQLException {
        Appointment appt = appointmentDAO.findById(appointmentID);
        if (appt == null)                       return CheckInResult.NOT_FOUND;
        if ("Arrived".equals(appt.getStatus())) return CheckInResult.ALREADY_CHECKED_IN;
        if (!"Confirmed".equals(appt.getStatus())) return CheckInResult.WRONG_STATUS;
        appointmentDAO.updateStatus(appointmentID, "Arrived");
        return CheckInResult.SUCCESS;
    }

    /** Gán 1 nhân viên cho 1 dòng dịch vụ cụ thể trong appointment (dùng ở bảng check-in). */
    public void assignStaffToServiceLine(int appointmentServiceID, int staffID) throws SQLException {
        appointmentDAO.assignStaffToService(appointmentServiceID, staffID);
    }

    /** Gán 1 nhân viên cho TOÀN BỘ dịch vụ thuộc 1 category trong appointment (gán nhanh hàng loạt). */
    public void assignStaffToCategory(int appointmentID, String categoryName, int staffID) throws SQLException {
        appointmentDAO.assignStaffToCategory(appointmentID, categoryName, staffID);
    }

    // ══ WALK-IN: LOOKUP CUSTOMER BY PHONE ════════════════════════════════════
    public Customer findCustomerByPhone(String phone) throws SQLException {
        return customerDAO.findByPhone(phone);
    }

    public List<Pet> getPetsByCustomer(int customerID) throws SQLException {
        return petDAO.findByCustomerId(customerID);
    }

    // ══ WALK-IN: TẠO LỊCH HẸN NHIỀU DỊCH VỤ, MỖI DỊCH VỤ 1 STAFF RIÊNG ═══════

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

    // ══ SLOT INFO ══════════════════════════════════════════════════════════════
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

    // ══ VET QUEUE (category = Chẩn đoán / Phác đồ điều trị) ═══════════════════
    public enum StartExamResult { SUCCESS, NOT_FOUND, WRONG_STATUS }

    public StartExamResult startExamination(int appointmentID, int vetID) throws SQLException {
        Appointment appt = appointmentDAO.findById(appointmentID);
        if (appt == null)                        return StartExamResult.NOT_FOUND;
        if (!"Arrived".equals(appt.getStatus())) return StartExamResult.WRONG_STATUS;
        appointmentDAO.updateStatus(appointmentID, "InProgress");
        return StartExamResult.SUCCESS;
    }

    // ══ SAVE MEDICAL RECORD ═══════════════════════════════════════════════════
    public enum SaveRecordResult {
        SUCCESS, APPOINTMENT_NOT_FOUND, WRONG_STATUS,
        RECORD_ALREADY_EXISTS, INSUFFICIENT_STOCK, DB_ERROR
    }

    public SaveRecordResult saveMedicalRecord(MedicalRecord record,
                                              List<PrescriptionItem> items,
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

        try {
            medicalRecordDAO.save(record, items);
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("Insufficient stock"))
                return SaveRecordResult.INSUFFICIENT_STOCK;
            throw e;
        }
        appointmentDAO.updateStatus(record.getAppointmentID(), "Done");
        return SaveRecordResult.SUCCESS;
    }

    // ══ QUERY HELPERS ═════════════════════════════════════════════════════════
    public List<Appointment> getConfirmedByDate(LocalDate date, Integer shift, String categoryFilter) throws SQLException {
        return appointmentDAO.findConfirmedByDate(date == null ? LocalDate.now() : date, shift, categoryFilter);
    }

    public List<Appointment> searchForCheckIn(String keyword, LocalDate date, String categoryFilter) throws SQLException {
        return appointmentDAO.searchForCheckIn(keyword, date == null ? LocalDate.now() : date, categoryFilter);
    }

    /** Hàng chờ bác sĩ: Arrived/InProgress có dịch vụ Chẩn đoán/Phác đồ gán cho vetID. */
    public List<Appointment> getVetQueue(int vetID, LocalDate date) throws SQLException {
        List<Appointment> result = appointmentDAO.findStaffQueue(vetID, date == null ? LocalDate.now() : date, CAT_LAB_TEST);
        List<Appointment> treatQueue = appointmentDAO.findStaffQueue(vetID, date == null ? LocalDate.now() : date, CAT_TREATMENT);
        mergeDistinctById(result, treatQueue);
        return result;
    }

    /** Các ca khám đã hoàn thành (Done) của 1 bác sĩ trong 1 ngày — để xem lại bệnh án. */
    public List<Appointment> getVetCompletedToday(int vetID, LocalDate date) throws SQLException {
        List<Appointment> result = appointmentDAO.findStaffCompletedToday(
                vetID, date == null ? LocalDate.now() : date, CAT_LAB_TEST, "MedicalRecords");
        List<Appointment> treatDone = appointmentDAO.findStaffCompletedToday(
                vetID, date == null ? LocalDate.now() : date, CAT_TREATMENT, "MedicalRecords");
        mergeDistinctById(result, treatDone);
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


}