package com.petclinic.backend.service;

import com.petclinic.backend.dao.*;
import com.petclinic.backend.model.GroomingRecord;
import com.petclinic.backend.model.MedicalRecord;
import com.petclinic.backend.model.VaccinationRecord;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PetService {
    private final MedicalRecordDAO medicalDAO = new MedicalRecordDAO();
    private final GroomingRecordDAO groomingDAO     = new GroomingRecordDAO();
    private final VaccinationRecordDAO vaccinationDAO       = new VaccinationRecordDAO();

    public Map<Integer, MedicalRecord> getMedicalRecordsByPet(int petID) throws SQLException {
        List<MedicalRecord> list = medicalDAO.findByPet(petID);
        return list.stream().collect(Collectors.toMap(
                MedicalRecord::getAppointmentID,
                record -> record,
                (existing, replacement) -> existing
        ));
    }
    public Map<Integer, VaccinationRecord> getVaccinationRecordsByPet(int petID) throws SQLException {
        List<VaccinationRecord> list = vaccinationDAO.findByPet(petID);
        return list.stream().collect(Collectors.toMap(
                VaccinationRecord::getAppointmentID,
                record -> record
        ));
    }

    public Map<Integer, GroomingRecord> getGroomingRecordsByPet(int petID) throws SQLException {
        List<GroomingRecord> list = groomingDAO.findByPet(petID);
        return list.stream().collect(Collectors.toMap(
                GroomingRecord::getAppointmentID,
                record -> record
        ));
    }
}
