package com.petclinic.dto;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

/**
 * 1 dòng cấu hình của bookingJson gửi từ booking-new.js — đại diện cho
 * lựa chọn dịch vụ/vaccine của 1 thú cưng trong cùng 1 lần đặt lịch
 * (mỗi pet có thể chọn bộ dịch vụ khác nhau, dùng chung 1 slotKey).
 *
 * Không lưu DB — chỉ là DTO trung gian giữa JSON submit và
 * BookingService.createAppointmentsForPets().
 */
public class PetBookingRequest {
    private int petId;
    private List<Integer> serviceIds = new ArrayList<>();
    private List<Integer> vaccineIds = new ArrayList<>();

    public PetBookingRequest(int petId, List<Integer> serviceIds, List<Integer> vaccineIds) {
        this.petId = petId;
        this.serviceIds = serviceIds != null ? serviceIds : new ArrayList<>();
        this.vaccineIds = vaccineIds != null ? vaccineIds : new ArrayList<>();
    }

    public int            getPetId()                  { return petId; }
    public List<Integer>  getServiceIds()              { return serviceIds; }
    public List<Integer>  getVaccineIds()              { return vaccineIds; }

    public boolean isEmpty() {
        return serviceIds.isEmpty() && vaccineIds.isEmpty();
    }

    // ── Parser (bookingPayload JSON -> List<PetBookingRequest>) ────────────────
    //
    // Dùng Gson để parse JSON dạng mảng:
    //   [{"petId":N,"serviceIds":[..],"vaccineIds":[..]}, ...]

    public static List<PetBookingRequest> parseList(String json) {
        List<PetBookingRequest> result = new ArrayList<>();
        if (json == null || json.isBlank()) return result;

        JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
        for (JsonElement el : arr) {
            JsonObject obj = el.getAsJsonObject();
            if (!obj.has("petId") || obj.get("petId").isJsonNull()) continue;

            int petId = obj.get("petId").getAsInt();
            List<Integer> serviceIds = toIntList(obj.getAsJsonArray("serviceIds"));
            List<Integer> vaccineIds = toIntList(obj.getAsJsonArray("vaccineIds"));

            result.add(new PetBookingRequest(petId, serviceIds, vaccineIds));
        }
        return result;
    }

    private static List<Integer> toIntList(JsonArray arr) {
        List<Integer> out = new ArrayList<>();
        if (arr == null) return out;
        for (JsonElement el : arr) {
            if (!el.isJsonNull()) out.add(el.getAsInt());
        }
        return out;
    }
}
