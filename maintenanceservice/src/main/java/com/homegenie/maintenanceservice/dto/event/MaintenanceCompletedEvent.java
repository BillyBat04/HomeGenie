package com.homegenie.maintenanceservice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceCompletedEvent {

    private String eventId;
    private Long requestId;
    private Long itemId;
    private Long userId;    
    private String userName;
    private String userEmail;
    private Long technicianId;
    private String technicianName;
    private String title;
    private String description;
    private String category;
    private String priority;
    private String requestType;
    private LocalDateTime completedAt;
    private String eventType; // Always "MAINTENANCE_COMPLETED"
    private Instant timestamp;
}
