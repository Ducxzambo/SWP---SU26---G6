package com.petclinic.backend.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class StaffAttendance {
    private int            attendanceID;
    private int            staffID;
    private LocalDate      workDate;
    private Integer        slotShift;
    private LocalDateTime  checkInTime;
    private LocalDateTime  checkOutTime;
    private String         status; // Present | Late | Absent | OnLeave
    private String         notes;
    private LocalDateTime  createdAt;

    private String staffName;
    private String roleName;

    public StaffAttendance() {}

    public int           getAttendanceID()        { return attendanceID; }
    public void          setAttendanceID(int v)    { attendanceID = v; }
    public int           getStaffID()              { return staffID; }
    public void          setStaffID(int v)         { staffID = v; }
    public LocalDate      getWorkDate()              { return workDate; }
    public void           setWorkDate(LocalDate v)    { workDate = v; }
    public Integer         getSlotShift()              { return slotShift; }
    public void            setSlotShift(Integer v)      { slotShift = v; }
    public LocalDateTime   getCheckInTime()             { return checkInTime; }
    public void            setCheckInTime(LocalDateTime v){ checkInTime = v; }
    public LocalDateTime   getCheckOutTime()            { return checkOutTime; }
    public void            setCheckOutTime(LocalDateTime v){ checkOutTime = v; }
    public String          getStatus()                    { return status; }
    public void            setStatus(String v)            { status = v; }
    public String          getNotes()                     { return notes; }
    public void            setNotes(String v)             { notes = v; }
    public LocalDateTime   getCreatedAt()                 { return createdAt; }
    public void            setCreatedAt(LocalDateTime v)  { createdAt = v; }
    public String          getStaffName()                 { return staffName; }
    public void            setStaffName(String v)         { staffName = v; }
    public String          getRoleName()                  { return roleName; }
    public void            setRoleName(String v)          { roleName = v; }

    public boolean isWholeDay() { return slotShift == null; }
}