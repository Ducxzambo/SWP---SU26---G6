package com.petclinic.model;

public class ServiceCategory {
    private int    categoryID;
    private String name;

    public ServiceCategory() {}

    public int    getCategoryID()        { return categoryID; }
    public void   setCategoryID(int v)    { categoryID = v; }
    public String getName()              { return name; }
    public void   setName(String v)      { name = v; }
}