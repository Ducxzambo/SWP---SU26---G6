package com.petclinic.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a 120-minute bookable slot.
 * maxCapacity = floor( sum(serviceDuration) / 120 ) / staffCount
 * A slot is greyed-out only when currentLoad >= maxCapacity.
 */
public class TimeSlot {

    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime; // startTime + 120 min
    private boolean available; // false only when currentLoad >= maxCapacity
    private double maxCapacity; // computed per service set
    private int currentLoad; // confirmed appointments in this slot
    private boolean inpatient; // inpatient slots: Morning / Afternoon only

    private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public TimeSlot() {}

    public TimeSlot(LocalDate date, LocalTime startTime, LocalTime endTime, boolean available) {
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.available = available;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate v) { date = v; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime v) { startTime = v; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime v) { endTime = v; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean v) { available = v; }

    public double getMaxCapacity() { return maxCapacity; }
    public void setMaxCapacity(double v) { maxCapacity = v; }

    public int getCurrentLoad() { return currentLoad; }
    public void setCurrentLoad(int v) { currentLoad = v; }

    public boolean isInpatient() { return inpatient; }
    public void setInpatient(boolean v) { inpatient = v; }

    /** "yyyy-MM-dd|HH:mm" — used as HTML value. */
    public String getSlotKey() {
        return date.format(DF) + "|" + startTime.format(TF);
    }

    public String getDisplayTime() {
        return startTime.format(TF) + " – " + endTime.format(TF);
    }

    public String getDisplayDate() {
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    /** Percentage fill for progress bar display (capped at 100). */
    public int getFillPercent() {
        if (maxCapacity <= 0) return 0;
        return Math.min(100, (int) Math.round(currentLoad / maxCapacity * 100));
    }

    /** Human-readable label: "Sáng" / "Chiều" for inpatient slots. */
    public String getPeriodLabel() {
        if (startTime.isBefore(LocalTime.NOON)) return "Sáng";
        return "Chiều";
    }
}