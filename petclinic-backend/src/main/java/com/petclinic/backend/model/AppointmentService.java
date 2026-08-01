package com.petclinic.backend.model;

import java.math.BigDecimal;

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
