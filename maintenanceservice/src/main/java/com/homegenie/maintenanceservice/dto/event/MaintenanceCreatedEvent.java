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
public class MaintenanceCreatedEvent {
    
    private String eventId;
    private Long requestId;
    private Long userId;
    private String userName;
    private String userEmail;
    private String title;
    private String description;
    private String category;
    private String priority;
    private String status;
    private String imageUrl;
    private String eventType;
    private Instant timestamp;
}
