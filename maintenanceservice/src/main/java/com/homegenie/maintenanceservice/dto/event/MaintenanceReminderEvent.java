package com.homegenie.maintenanceservice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceReminderEvent {
    private String eventId;
    private Long itemId;
    private String itemName;
    private String itemCategory;
    private String itemLocation;
    private Long userId;
    private String userName;
    private String userEmail;
    private LocalDate nextMaintenanceDate;
    private Integer daysUntilMaintenance;
    private LocalDate lastMaintenanceDate;
    private Integer maintenanceFrequencyDays;
    private String urgencyLevel;
    private String eventType; // Always "MAINTENANCE_REMINDER"
    private Instant timestamp;
}
