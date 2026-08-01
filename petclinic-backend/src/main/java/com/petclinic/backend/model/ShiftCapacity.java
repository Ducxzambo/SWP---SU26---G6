package com.petclinic.backend.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ShiftCapacity {
    private int           shiftCapacityID;
    private LocalDate     capacityDate;
    private int           slotShift; // 1-4
    private int           groomCap;
    private int           vetCap;
    private Integer       updatedByID;
    private LocalDateTime updatedAt;

    public ShiftCapacity() {}

    public int           getShiftCapacityID()      { return shiftCapacityID; }
    public void          setShiftCapacityID(int v)  { shiftCapacityID = v; }
    public LocalDate      getCapacityDate()          { return capacityDate; }
    public void           setCapacityDate(LocalDate v){ capacityDate = v; }
    public int            getSlotShift()             { return slotShift; }
    public void            setSlotShift(int v)        { slotShift = v; }
    public int            getGroomCap()              { return groomCap; }
    public void            setGroomCap(int v)         { groomCap = v; }
    public int            getVetCap()                { return vetCap; }
    public void            setVetCap(int v)           { vetCap = v; }
    public Integer         getUpdatedByID()            { return updatedByID; }
    public void            setUpdatedByID(Integer v)    { updatedByID = v; }
    public LocalDateTime   getUpdatedAt()               { return updatedAt; }
    public void            setUpdatedAt(LocalDateTime v) { updatedAt = v; }
}