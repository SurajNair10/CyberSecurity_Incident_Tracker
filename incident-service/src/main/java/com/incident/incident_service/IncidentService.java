package com.incident.incident_service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IncidentService {

    @Autowired
    private IncidentRepository repo;

    public Incident createIncident(IncidentRequest request) {
        Incident incident = new Incident();
        incident.setTitle(request.getTitle());
        incident.setDescription(request.getDescription());
        incident.setSeverity(request.getSeverity());
        return repo.save(incident);
    }

    public List<Incident> getAllIncidents() {
        return repo.findAll();
    }

    public Incident getIncidentById(Long id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("Incident not found"));
    }

    public Incident updateStatus(Long id, String status) {
        Incident incident = getIncidentById(id);
        incident.setStatus(status);
        return repo.save(incident);
    }



}
