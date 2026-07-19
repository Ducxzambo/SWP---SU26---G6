package com.petclinic.model;

import com.petclinic.util.ServiceCategoryDescriptions;

import java.util.List;

public class ServiceCategory {
    private int    categoryID;
    private String name;
    private List<Service> services; // populated on demand

    public ServiceCategory() {}
    public ServiceCategory(int categoryID, String name) {
        this.categoryID = categoryID;
        this.name = name;
    }

    public int    getCategoryID()  { return categoryID; }
    public void   setCategoryID(int v) { categoryID = v; }
    public String getName()        { return name; }
    public void   setName(String v){ name = v; }
    public List<Service> getServices()           { return services; }
    public void          setServices(List<Service> v) { services = v; }

    /**
     * Mô tả ngắn cho nhóm dịch vụ, dùng trên /home và /services. Được suy ra
     * từ tên nhóm (không lưu trong CSDL) — xem ServiceCategoryDescriptions.
     */
    public String getDescription() {
        return ServiceCategoryDescriptions.describe(name);
    }
}
