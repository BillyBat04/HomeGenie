package com.homegenie.maintenanceservice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Event published when an item needs maintenance soon
 * Used for proactive maintenance reminders to users
 * 
 * Triggered by ItemMaintenanceScheduler daily cron job
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceReminderEvent {
    
    /**
     * Unique event identifier
     */
    private String eventId;
    
    /**
     * Item that needs maintenance
     */
    private Long itemId;
    private String itemName;
    private String itemCategory;
    private String itemLocation;
    
    /**
     * Owner of the item
     */
    private Long userId;
    private String userName;
    private String userEmail;
    
    /**
     * Maintenance schedule details
     */
    private LocalDate nextMaintenanceDate;
    private Integer daysUntilMaintenance;
    private LocalDate lastMaintenanceDate;
    private Integer maintenanceFrequencyDays;
    
    /**
     * Reminder urgency level
     * - OVERDUE: maintenance date has passed
     * - DUE_NOW: maintenance due today
     * - DUE_SOON: maintenance due within 7 days
     */
    private String urgencyLevel;
    
    /**
     * Event metadata
     */
    private String eventType; // Always "MAINTENANCE_REMINDER"
    private Instant timestamp;
}
