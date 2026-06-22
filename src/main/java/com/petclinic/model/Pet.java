package com.petclinic.model;

import java.time.LocalDate;

public class Pet {
    private int       petID;
    private int       customerID;
    private String    name;
    private String    speciesName;
    private String    breedName;
    private String    gender;
    private LocalDate dateOfBirth;
    private java.math.BigDecimal weight;
    private boolean   isDeleted;

    public Pet() {}

    public int       getPetID()         { return petID; }
    public void      setPetID(int v)     { petID = v; }
    public int       getCustomerID()    { return customerID; }
    public void      setCustomerID(int v){ customerID = v; }
    public String    getName()          { return name; }
    public void      setName(String v)   { name = v; }
    public String    getSpeciesName()   { return speciesName; }
    public void      setSpeciesName(String v) { speciesName = v; }
    public String    getBreedName()     { return breedName; }
    public void      setBreedName(String v) { breedName = v; }
    public String    getGender()        { return gender; }
    public void      setGender(String v) { gender = v; }
    public LocalDate getDateOfBirth()   { return dateOfBirth; }
    public void      setDateOfBirth(LocalDate v) { dateOfBirth = v; }
    public java.math.BigDecimal getWeight() { return weight; }
    public void      setWeight(java.math.BigDecimal v) { weight = v; }
    public boolean   isDeleted()        { return isDeleted; }
    public void      setDeleted(boolean v) { isDeleted = v; }

    /** Display label: "Bella (Chó - Poodle)" */
    public String getDisplayLabel() {
        return name + " (" + speciesName
                + (breedName != null && !breedName.isBlank() ? " - " + breedName : "")
                + ")";
    }
}