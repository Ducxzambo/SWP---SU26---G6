package com.petclinic.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 1 dòng cấu hình của bookingJson gửi từ booking-new.js — đại diện cho
 * lựa chọn dịch vụ/vaccine RIÊNG của 1 thú cưng trong cùng 1 lần đặt lịch
 * (mỗi pet có thể chọn bộ dịch vụ khác nhau, dùng chung 1 slotKey).
 *
 * Không lưu DB — chỉ là DTO trung gian giữa JSON submit và
 * BookingService.createAppointmentsForPets().
 */
public class PetBookingRequest {
    private int petId;
    private List<Integer> serviceIds = new ArrayList<>();
    private List<Integer> vaccineIds = new ArrayList<>();

    public PetBookingRequest(int petId) { this.petId = petId; }

    public PetBookingRequest(int petId, List<Integer> serviceIds, List<Integer> vaccineIds) {
        this.petId = petId;
        this.serviceIds = serviceIds != null ? serviceIds : new ArrayList<>();
        this.vaccineIds = vaccineIds != null ? vaccineIds : new ArrayList<>();
    }

    public int            getPetId()                  { return petId; }
    public void           setPetId(int v)              { petId = v; }
    public List<Integer>  getServiceIds()              { return serviceIds; }
    public void           setServiceIds(List<Integer> v){ serviceIds = v; }
    public List<Integer>  getVaccineIds()              { return vaccineIds; }
    public void           setVaccineIds(List<Integer> v){ vaccineIds = v; }

    public boolean isEmpty() {
        return serviceIds.isEmpty() && vaccineIds.isEmpty();
    }
}