package com.petclinic.backend.dto;

public class StaffAttendanceSummary {
    private int staffID;
    private String staffName;
    private String roleName;
    private int lateCount;
    private int absentCount;
    private int onLeaveCount;

    public int getStaffID() { return staffID; }
    public void setStaffID(int v) { staffID = v; }
    public String getStaffName() { return staffName; }
    public void setStaffName(String v) { staffName = v; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String v) { roleName = v; }
    public int getLateCount() { return lateCount; }
    public void setLateCount(int v) { lateCount = v; }
    public int getAbsentCount() { return absentCount; }
    public void setAbsentCount(int v) { absentCount = v; }
    public int getOnLeaveCount() { return onLeaveCount; }
    public void setOnLeaveCount(int v) { onLeaveCount = v; }

    public boolean isAbsentAlert()  { return absentCount > 9; }
    public boolean isOnLeaveAlert() { return onLeaveCount > 3; }
    public boolean isLateAlert()    { return lateCount > 15; }
    public boolean isAnyAlert()     { return isAbsentAlert() || isOnLeaveAlert() || isLateAlert(); }
}