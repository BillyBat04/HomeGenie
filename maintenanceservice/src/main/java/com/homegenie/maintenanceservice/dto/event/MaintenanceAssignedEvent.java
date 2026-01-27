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
public class MaintenanceAssignedEvent {
    
    private String eventId;
    private Long requestId;
    private Long userId;
    private String userName;
    private String userEmail;
    private Long technicianId;
    private String technicianName;
    private String technicianEmail;
    private String title;
    private String category;
    private String priority;
    private String status;
    private String eventType;
    private Instant timestamp;
}
