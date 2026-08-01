package com.petclinic.backend.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class TimeSlot {
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;       // startTime + 120 min
    private boolean   available;
    private double    maxCapacity;
    private int       currentLoad;
    private boolean    placeholder;

    // Per-role-group breakdown
    private int groomLoad;
    private int groomCap;
    private int vetLoad;
    private int vetCap;

    private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public TimeSlot() {}

    public TimeSlot(LocalDate date, LocalTime startTime, LocalTime endTime, boolean available) {
        this.date      = date;
        this.startTime = startTime;
        this.endTime   = endTime;
        this.available = available;
    }

    public LocalDate getDate()           { return date; }
    public void      setDate(LocalDate v){ date = v; }
    public LocalTime getStartTime()      { return startTime; }
    public void      setStartTime(LocalTime v){ startTime = v; }
    public LocalTime getEndTime()        { return endTime; }
    public void      setEndTime(LocalTime v){ endTime = v; }
    public boolean   isAvailable()       { return available; }
    public void      setAvailable(boolean v){ available = v; }
    public double    getMaxCapacity()    { return maxCapacity; }
    public void      setMaxCapacity(double v){ maxCapacity = v; }
    public int       getCurrentLoad()    { return currentLoad; }
    public void      setCurrentLoad(int v){ currentLoad = v; }
    public boolean   isPlaceholder()      { return placeholder; }
    public void      setPlaceholder(boolean v){ placeholder = v; }
    public int       getGroomLoad()      { return groomLoad; }
    public void      setGroomLoad(int v) { groomLoad = v; }
    public int       getGroomCap()       { return groomCap; }
    public void      setGroomCap(int v)  { groomCap = v; }
    public int       getVetLoad()        { return vetLoad; }
    public void      setVetLoad(int v)   { vetLoad = v; }
    public int       getVetCap()         { return vetCap; }
    public void      setVetCap(int v)    { vetCap = v; }

    public String getSlotKey()     { return date.format(DF) + "|" + startTime.format(TF); }
    public String getDisplayTime() { return startTime.format(TF) + " – " + endTime.format(TF); }

    public int getFillPercent() {
        if (maxCapacity <= 0) return 0;
        return Math.min(100, (int) Math.round(currentLoad / maxCapacity * 100));
    }
}
