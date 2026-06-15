package com.petclinic.model;

import java.time.LocalDateTime;

public class Notification {
    private int           notificationID;
    private Integer       customerID;
    private Integer       staffID;
    private String        title;
    private String        body;
    private boolean       isRead;
    private LocalDateTime createdAt;

    public Notification() {}

    public int           getNotificationID()       { return notificationID; }
    public void          setNotificationID(int v)  { notificationID = v; }
    public Integer       getCustomerID()           { return customerID; }
    public void          setCustomerID(Integer v)  { customerID = v; }
    public Integer       getStaffID()              { return staffID; }
    public void          setStaffID(Integer v)     { staffID = v; }
    public String        getTitle()                { return title; }
    public void          setTitle(String v)        { title = v; }
    public String        getBody()                 { return body; }
    public void          setBody(String v)         { body = v; }
    public boolean       isRead()                  { return isRead; }
    public void          setRead(boolean v)        { isRead = v; }
    public LocalDateTime getCreatedAt()            { return createdAt; }
    public void          setCreatedAt(LocalDateTime v){ createdAt = v; }
}
