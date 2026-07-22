package com.petclinic.dto;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Lựa chọn dịch vụ/vaccine của 1 lần đặt lịch, gửi từ booking-new.js.
 * Không lưu DB — chỉ là DTO trung gian giữa JSON submit và
 * BookingService.createNormalAppointment().
 */
public class BookingSelection {
    private List<Integer> serviceIds = new ArrayList<>();
    private List<Integer> vaccineIds = new ArrayList<>();

    public BookingSelection(List<Integer> serviceIds, List<Integer> vaccineIds) {
        this.serviceIds = serviceIds != null ? serviceIds : new ArrayList<>();
        this.vaccineIds = vaccineIds != null ? vaccineIds : new ArrayList<>();
    }

    public List<Integer>  getServiceIds()              { return serviceIds; }
    public List<Integer>  getVaccineIds()              { return vaccineIds; }

    public boolean isEmpty() {
        return serviceIds.isEmpty() && vaccineIds.isEmpty();
    }

    // ── Parser (bookingPayload JSON -> BookingSelection) ────────────────────
    //
    // Dùng Gson để parse JSON dạng object:
    //   {"serviceIds":[..],"vaccineIds":[..]}

    public static BookingSelection parse(String json) {
        if (json == null || json.isBlank()) return new BookingSelection(null, null);

        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        List<Integer> serviceIds = toIntList(obj.getAsJsonArray("serviceIds"));
        List<Integer> vaccineIds = toIntList(obj.getAsJsonArray("vaccineIds"));
        return new BookingSelection(serviceIds, vaccineIds);
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
