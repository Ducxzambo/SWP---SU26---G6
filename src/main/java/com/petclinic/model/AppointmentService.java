package com.petclinic.model;

import java.math.BigDecimal;

/**
 * 1 dòng trong bảng join AppointmentServices — đại diện cho 1 dịch vụ được
 * chọn trong 1 appointment (N-N giữa Appointments và Services).
 *
 * UnitPrice được "chốt" tại thời điểm đặt lịch (snapshot giá Service lúc
 * đó) — không đổi kể cả khi Service.Price thay đổi về sau, để hoá đơn luôn
 * khớp với số tiền đã thu.
 */
public class AppointmentService {
    private int        appointmentServiceID;
    private int        appointmentID;
    private int        serviceID;
    private BigDecimal unitPrice;
    private Integer    assignedStaffID;

    // Joined display fields (populated by DAO khi cần hiển thị)
    private String serviceName;
    private int    categoryID;
    private String categoryName;
    private String staffName;

    public AppointmentService() {}

    public int        getAppointmentServiceID()      { return appointmentServiceID; }
    public void        setAppointmentServiceID(int v) { appointmentServiceID = v; }
    public int        getAppointmentID()              { return appointmentID; }
    public void        setAppointmentID(int v)         { appointmentID = v; }
    public int        getServiceID()                   { return serviceID; }
    public void        setServiceID(int v)              { serviceID = v; }
    public BigDecimal getUnitPrice()                    { return unitPrice; }
    public void        setUnitPrice(BigDecimal v)        { unitPrice = v; }
    public Integer     getAssignedStaffID()               { return assignedStaffID; }
    public void        setAssignedStaffID(Integer v)       { assignedStaffID = v; }
    public String      getServiceName()                     { return serviceName; }
    public void        setServiceName(String v)              { serviceName = v; }
    public int         getCategoryID()                        { return categoryID; }
    public void        setCategoryID(int v)                    { categoryID = v; }
    public String      getCategoryName()                        { return categoryName; }
    public void        setCategoryName(String v)                 { categoryName = v; }
    public String      getStaffName()                             { return staffName; }
    public void        setStaffName(String v)                      { staffName = v; }
}
