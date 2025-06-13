package com.incident.incident_service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    @Autowired
    private IncidentService incidentService;

    @PostMapping
    public ResponseEntity<Incident> create(@RequestBody IncidentRequest req) {
        return ResponseEntity.ok(incidentService.createIncident(req));
    }

    @GetMapping
    public List<Incident> getAll() {
        return incidentService.getAllIncidents();
    }

    @GetMapping("/{id}")
    public Incident getIncident(@PathVariable Long id) {
        return incidentService.getIncidentById(id);
    }

    @PatchMapping("/{id}/status")
        public Incident updateStatus(@PathVariable Long id, @RequestParam String status){
        return incidentService.updateStatus(id, status);
    }

}
