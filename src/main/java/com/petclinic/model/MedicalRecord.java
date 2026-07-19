package com.petclinic.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MedicalRecord {
    private int           recordID;
    private int           appointmentID;
    private int           petID;
    private int           staffID;
    private BigDecimal    weight;
    private BigDecimal    temperature;
    private String        symptoms;
    private String        diagnosis;
    private String        treatmentPlan;
    private LocalDateTime createdAt;

    // Joined
    private String        staffName;
    private List<PrescriptionItem> prescriptions;

    public MedicalRecord() {}

    public int           getRecordID()          { return recordID; }
    public void          setRecordID(int v)     { recordID = v; }
    public int           getAppointmentID()     { return appointmentID; }
    public void          setAppointmentID(int v){ appointmentID = v; }
    public int           getPetID()             { return petID; }
    public void          setPetID(int v)        { petID = v; }
    public int           getStaffID()             { return staffID; }
    public void          setStaffID(int v)        { staffID = v; }
    public BigDecimal    getWeight()            { return weight; }
    public void          setWeight(BigDecimal v){ weight = v; }
    public BigDecimal    getTemperature()       { return temperature; }
    public void          setTemperature(BigDecimal v){ temperature = v; }
    public String        getSymptoms()          { return symptoms; }
    public void          setSymptoms(String v)  { symptoms = v; }
    public String        getDiagnosis()         { return diagnosis; }
    public void          setDiagnosis(String v) { diagnosis = v; }
    public String        getTreatmentPlan()     { return treatmentPlan; }
    public void          setTreatmentPlan(String v){ treatmentPlan = v; }
    public LocalDateTime getCreatedAt()         { return createdAt; }
    public void          setCreatedAt(LocalDateTime v){ createdAt = v; }
    public String        getStaffName()           { return staffName; }
    public void          setStaffName(String v)   { staffName = v; }
    public List<PrescriptionItem> getPrescriptions()              { return prescriptions; }
    public void                   setPrescriptions(List<PrescriptionItem> v){ prescriptions = v; }

    // ── Diagnosis / TreatmentPlan (checklist string) ────────────────────────
    // Format do staff-side build (buildDiagnosis/buildTreatmentPlan):
    //   mỗi dòng "[Tên hạng mục]: Ghi chú" (ghi chú có thể vắng), các dòng
    //   phân tách bằng "\n". Riêng Diagnosis còn có thể là chuỗi triệu chứng
    //   tự do (không có dấu "[") khi staff không chọn xét nghiệm nào
    //   (fallback = req.getParameter("symptoms")) — TreatmentPlan thì luôn
    //   là "" hoặc đúng định dạng checklist, không có fallback tự do.

    /** true nếu Diagnosis ở dạng checklist có cấu trúc (hiển thị bảng được);
     *  false nếu là văn bản triệu chứng tự do (hiển thị đoạn văn bình thường). */
    public boolean isDiagnosisStructured() {
        return diagnosis != null && diagnosis.trim().startsWith("[");
    }

    public List<LabTestEntry> getDiagnosisEntries() {
        return parseEntries(diagnosis);
    }

    public List<LabTestEntry> getTreatmentEntries() {
        return parseEntries(treatmentPlan);
    }

    private static List<LabTestEntry> parseEntries(String raw) {
        List<LabTestEntry> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) return out;
        for (String line : raw.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            int close = line.startsWith("[") ? line.indexOf(']') : -1;
            if (close > 0) {
                String name = line.substring(1, close).trim();
                String rest = line.substring(close + 1).trim();
                if (rest.startsWith(":")) rest = rest.substring(1).trim();
                out.add(new LabTestEntry(name, rest));
            } else {
                out.add(new LabTestEntry(line, ""));
            }
        }
        return out;
    }

    /** 1 dòng trong bảng Diagnosis/TreatmentPlan: tên hạng mục + ghi chú. */
    public static class LabTestEntry {
        private final String name;
        private final String note;
        public LabTestEntry(String name, String note) { this.name = name; this.note = note; }
        public String getName() { return name; }
        public String getNote() { return note; }
    }
}