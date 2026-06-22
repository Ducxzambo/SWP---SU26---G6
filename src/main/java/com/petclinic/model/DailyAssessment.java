package com.petclinic.model;

import java.time.LocalDate;

/**
 * Maps exactly to dbo.DailyAssessments
 *
 * Columns:
 *   AssessmentID   int IDENTITY PK
 *   AdmissionID    int NOT NULL  FK → InpatientAdmissions
 *   VetID          int NOT NULL  FK → Staff
 *   AssessmentDate date NOT NULL
 *   Condition      nvarchar(1000) NULL
 *   TreatmentToday nvarchar(1000) NULL
 */
public class DailyAssessment {

    // ── DB columns ────────────────────────────────────────────────────────────
    private int       assessmentID;
    private int       admissionID;
    private int       vetID;
    private LocalDate assessmentDate;
    private String    condition;
    private String    treatmentToday;

    // ── Joined fields ─────────────────────────────────────────────────────────
    private String vetName;   // from Staff.FullName

    public DailyAssessment() {}

    // ── Getters & Setters ─────────────────────────────────────────────────────
    public int       getAssessmentID()               { return assessmentID; }
    public void      setAssessmentID(int v)          { assessmentID = v; }

    public int       getAdmissionID()                { return admissionID; }
    public void      setAdmissionID(int v)           { admissionID = v; }

    public int       getVetID()                      { return vetID; }
    public void      setVetID(int v)                 { vetID = v; }

    public LocalDate getAssessmentDate()             { return assessmentDate; }
    public void      setAssessmentDate(LocalDate v)  { assessmentDate = v; }

    public String    getCondition()                  { return condition; }
    public void      setCondition(String v)          { condition = v; }

    public String    getTreatmentToday()             { return treatmentToday; }
    public void      setTreatmentToday(String v)     { treatmentToday = v; }

    // joined
    public String    getVetName()                    { return vetName; }
    public void      setVetName(String v)            { vetName = v; }
}
