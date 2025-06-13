package com.incident.incident_service;

import java.time.LocalDateTime;

public class IncidentResponse {
    private Long id;
    private String title;
    private String description;
    private String severity;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
