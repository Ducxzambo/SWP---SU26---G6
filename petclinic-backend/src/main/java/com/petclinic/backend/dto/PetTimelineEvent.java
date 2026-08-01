package com.petclinic.backend.dto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class PetTimelineEvent {
    public enum Type { DONE, CONFIRMED, VACCINE, FOLLOWUP }

    private final LocalDate date;
    private final Type type;
    private final String label;
    private final Integer appointmentId;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public PetTimelineEvent(LocalDate date, Type type, String label, Integer appointmentId) {
        this.date = date;
        this.type = type;
        this.label = label;
        this.appointmentId = appointmentId;
    }

    public LocalDate getDate()        { return date; }
    public Type getType()             { return type; }
    public String getLabel()          { return label; }
    public Integer getAppointmentId() { return appointmentId; }
    public String getFormattedDate()  { return date.format(FMT); }

    public String getDotClass() {
        switch (type) {
            case DONE:      return "tl-dot-green";
            case CONFIRMED: return "tl-dot-amber";
            default:        return "tl-dot-black";
        }
    }

    public String getTypeLabel() {
        switch (type) {
            case DONE:      return "Đã hoàn thành";
            case CONFIRMED: return "Đã xác nhận";
            case VACCINE:   return "Tiêm vaccine";
            case FOLLOWUP:  return "Ngày tái khám";
            default:        return "";
        }
    }
}