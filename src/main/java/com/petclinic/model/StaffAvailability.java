package com.petclinic.model;

import java.time.LocalTime;

public class StaffAvailability {
    private int       availabilityID;
    private int       staffID;
    private int       dayOfWeek; // 0=Sun,1=Mon,...6=Sat
    private LocalTime startTime;
    private LocalTime endTime;

    public StaffAvailability() {}

    public int       getAvailabilityID()       { return availabilityID; }
    public void      setAvailabilityID(int v)  { availabilityID = v; }
    public int       getStaffID()              { return staffID; }
    public void      setStaffID(int v)         { staffID = v; }
    public int       getDayOfWeek()            { return dayOfWeek; }
    public void      setDayOfWeek(int v)       { dayOfWeek = v; }
    public LocalTime getStartTime()            { return startTime; }
    public void      setStartTime(LocalTime v) { startTime = v; }
    public LocalTime getEndTime()              { return endTime; }
    public void      setEndTime(LocalTime v)   { endTime = v; }
}
