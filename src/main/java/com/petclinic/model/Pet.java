package com.petclinic.model;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

public class Pet {
    private int       petID;
    private int       customerID;
    private String    name;
    private String    speciesName;
    private String    breedName;
    private String    gender;
    private LocalDate dateOfBirth;
    private java.math.BigDecimal weight;
    private boolean   isDeleted;

    // Aggregated stats (populated by PetDAO or PetService)
    private int    totalAppointments;
    private int    doneAppointments;
    private String lastVisitDate;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public Pet() {}

    public int       getPetID()         { return petID; }
    public void      setPetID(int v)     { petID = v; }
    public int       getCustomerID()    { return customerID; }
    public void      setCustomerID(int v){ customerID = v; }
    public String    getName()          { return name; }
    public void      setName(String v)   { name = v; }
    public String    getSpeciesName()   { return speciesName; }
    public void      setSpeciesName(String v) { speciesName = v; }
    public String    getBreedName()     { return breedName; }
    public void      setBreedName(String v) { breedName = v; }
    public String    getGender()        { return gender; }
    public void      setGender(String v) { gender = v; }
    public LocalDate getDateOfBirth()   { return dateOfBirth; }
    public void      setDateOfBirth(LocalDate v) { dateOfBirth = v; }
    public java.math.BigDecimal getWeight() { return weight; }
    public void      setWeight(java.math.BigDecimal v) { weight = v; }
    public boolean   isDeleted()        { return isDeleted; }
    public void      setDeleted(boolean v) { isDeleted = v; }

    public int        getTotalAppointments()    { return totalAppointments; }
    public void       setTotalAppointments(int v){ totalAppointments = v; }
    public int        getDoneAppointments()     { return doneAppointments; }
    public void       setDoneAppointments(int v){ doneAppointments = v; }
    public String     getLastVisitDate()        { return lastVisitDate; }
    public void       setLastVisitDate(String v){ lastVisitDate = v; }

    /** Display label: "Bella (Chó - Poodle)" */
    public String getDisplayLabel() {
        return name + " (" + speciesName
                + (breedName != null && !breedName.isBlank() ? " - " + breedName : "")
                + ")";
    }

    // ── Display helpers ───────────────────────────────────────────────────────
    public String getFormattedDateOfBirth() {
        return dateOfBirth != null ? dateOfBirth.format(DATE_FMT) : "Không rõ";
    }

    /** "2 tuổi 3 tháng" or "Không rõ" */
    public String getAgeDisplay() {
        if (dateOfBirth == null) return "Không rõ";
        Period p = Period.between(dateOfBirth, LocalDate.now());
        if (p.getYears() == 0 && p.getMonths() == 0) return p.getDays() + " ngày tuổi";
        if (p.getYears() == 0) return p.getMonths() + " tháng tuổi";
        if (p.getMonths() == 0) return p.getYears() + " tuổi";
        return p.getYears() + " tuổi " + p.getMonths() + " tháng";
    }

    /** Emoji icon by species name */
    public String getSpeciesEmoji() {
        if (speciesName == null) return "🐾";
        String s = speciesName.toLowerCase();
        if (s.contains("mèo") || s.contains("cat"))  return "🐱";
        if (s.contains("chó") || s.contains("dog"))  return "🐶";
        if (s.contains("chim") || s.contains("bird")) return "🐦";
        if (s.contains("thỏ") || s.contains("rabbit")) return "🐰";
        if (s.contains("cá") || s.contains("fish"))   return "🐟";
        return "🐾";
    }

    public String getGenderDisplay() {
        if ("Male".equals(gender))   return "Đực";
        if ("Female".equals(gender)) return "Cái";
        return "Không rõ";
    }
}