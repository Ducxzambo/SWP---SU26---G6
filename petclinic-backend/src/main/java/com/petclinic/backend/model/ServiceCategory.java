package com.petclinic.backend.model;

import com.petclinic.backend.util.ServiceCategoryDescriptions;

import java.util.List;

public class ServiceCategory {
    private int    categoryID;
    private String name;
    private List<Service> services;

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

    public String getDescription() {
        return ServiceCategoryDescriptions.describe(name);
    }
}
