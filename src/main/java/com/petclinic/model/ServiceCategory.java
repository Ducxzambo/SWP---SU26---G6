package com.petclinic.model;

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
}