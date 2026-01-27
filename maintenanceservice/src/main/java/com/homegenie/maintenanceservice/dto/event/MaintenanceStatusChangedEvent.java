package com.homegenie.maintenanceservice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceStatusChangedEvent {
    
    private String eventId;
    private Long requestId;
    private Long userId;
    private String userName;
    private String userEmail;
    private Long technicianId;
    private String technicianName;
    private String title;
    private String category;
    private String oldStatus;
    private String newStatus;
    private String priority;
    private String eventType;
    private Instant timestamp;
}
