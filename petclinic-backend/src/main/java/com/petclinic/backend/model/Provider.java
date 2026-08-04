package com.petclinic.backend.model;

import java.time.LocalDateTime;

public class Provider {
    private int providerID;
    private String name, contactPerson, phone, email, address, taxCode, note;
    private boolean active;
    private LocalDateTime createdAt;
    public int getProviderID() { return providerID; } public void setProviderID(int v) { providerID = v; }
    public String getName() { return name; } public void setName(String v) { name = v; }
    public String getContactPerson() { return contactPerson; } public void setContactPerson(String v) { contactPerson = v; }
    public String getPhone() { return phone; } public void setPhone(String v) { phone = v; }
    public String getEmail() { return email; } public void setEmail(String v) { email = v; }
    public String getAddress() { return address; } public void setAddress(String v) { address = v; }
    public String getTaxCode() { return taxCode; } public void setTaxCode(String v) { taxCode = v; }
    public String getNote() { return note; } public void setNote(String v) { note = v; }
    public boolean isActive() { return active; } public void setActive(boolean v) { active = v; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime v) { createdAt = v; }
}
