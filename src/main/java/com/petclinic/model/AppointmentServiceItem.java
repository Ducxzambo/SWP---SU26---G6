package com.petclinic.model;

import java.math.BigDecimal;

/**
 * 1 dòng trong bảng AppointmentServices — biểu diễn 1 dịch vụ được chọn
 * trong 1 lịch hẹn cụ thể, kèm giá snapshot và NHÂN VIÊN PHỤ TRÁCH riêng
 * cho chính dịch vụ đó (AssignedStaffID nằm ở đây, không nằm ở Appointments).
 *
 * Nhờ vậy 1 appointment có thể trộn nhiều category dịch vụ khác nhau
 * (VD: vừa khám bệnh vừa grooming) mà mỗi dịch vụ vẫn có đúng 1 nhân viên
 * phù hợp phụ trách (bác sĩ cho dịch vụ khám, groomer cho dịch vụ grooming).
 */
public class AppointmentServiceItem {
    private int        appointmentServiceID;
    private int        appointmentID;
    private int        serviceID;
    private BigDecimal unitPrice;
    private Integer    assignedStaffID;   // nullable — chưa gán thì null

    // transient join fields
    private String serviceName;
    private String categoryName;
    private String staffName;             // tên nhân viên đang phụ trách dòng này (nếu có)

    public AppointmentServiceItem() {}

    public AppointmentServiceItem(int serviceID, BigDecimal unitPrice) {
        this.serviceID = serviceID;
        this.unitPrice = unitPrice;
    }

    public int        getAppointmentServiceID()   { return appointmentServiceID; }
    public void       setAppointmentServiceID(int v){ appointmentServiceID = v; }
    public int        getAppointmentID()          { return appointmentID; }
    public void       setAppointmentID(int v)      { appointmentID = v; }
    public int        getServiceID()              { return serviceID; }
    public void       setServiceID(int v)          { serviceID = v; }
    public BigDecimal getUnitPrice()              { return unitPrice; }
    public void       setUnitPrice(BigDecimal v)   { unitPrice = v; }
    public Integer    getAssignedStaffID()        { return assignedStaffID; }
    public void       setAssignedStaffID(Integer v){ assignedStaffID = v; }

    public String getServiceName()                { return serviceName; }
    public void   setServiceName(String v)        { serviceName = v; }
    public String getCategoryName()               { return categoryName; }
    public void   setCategoryName(String v)       { categoryName = v; }
    public String getStaffName()                  { return staffName; }
    public void   setStaffName(String v)          { staffName = v; }

    public boolean isAssigned() { return assignedStaffID != null; }
}