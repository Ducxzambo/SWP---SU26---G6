package com.petclinic.model;

import java.time.LocalDate;

/**
 * Maps exactly to dbo.DailyAssessments (updated schema)
 *
 * Columns:
 *   AssessmentID   int IDENTITY PK
 *   AdmissionID    int NOT NULL  FK → InpatientAdmissions
 *   StaffID        int NOT NULL  FK → Staff  (was VetID in old schema)
 *   AssessmentDate date NOT NULL
 *   Condition      nvarchar NULL
 *   TreatmentToday nvarchar NULL
 */
public class DailyAssessment {

    private int       assessmentID;
    private int       admissionID;
    private int       staffID;         // renamed from vetID — matches new schema
    private LocalDate assessmentDate;
    private String    condition;
    private String    treatmentToday;

    // joined from Staff.FullName
    private String vetName;

    public DailyAssessment() {}

    public int       getAssessmentID()               { return assessmentID; }
    public void      setAssessmentID(int v)          { assessmentID = v; }

    public int       getAdmissionID()                { return admissionID; }
    public void      setAdmissionID(int v)           { admissionID = v; }

    public int       getStaffID()                    { return staffID; }
    public void      setStaffID(int v)               { staffID = v; }

    // backward-compat aliases
    public int       getVetID()                      { return staffID; }
    public void      setVetID(int v)                 { staffID = v; }

    public LocalDate getAssessmentDate()             { return assessmentDate; }
    public void      setAssessmentDate(LocalDate v)  { assessmentDate = v; }

    public String    getCondition()                  { return condition; }
    public void      setCondition(String v)          { condition = v; }

    public String    getTreatmentToday()             { return treatmentToday; }
    public void      setTreatmentToday(String v)     { treatmentToday = v; }

    public String    getVetName()                    { return vetName; }
    public void      setVetName(String v)            { vetName = v; }
}