package com.homegenie.maintenanceservice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Event published when a maintenance request is marked as COMPLETED
 * Consumed by ItemService to update item maintenance dates
 * 
 * This is the event-driven bridge between MaintenanceService and ItemService
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceCompletedEvent {
    
    /**
     * Unique event identifier
     */
    private String eventId;
    
    /**
     * Maintenance request ID that was completed
     */
    private Long requestId;
    
    /**
     * Item ID linked to this maintenance (null if no item linked)
     * CRITICAL: ItemService uses this to know which item to update
     */
    private Long itemId;
    
    /**
     * User who owns the item
     */
    private Long userId;
    
    /**
     * User details for notification
     */
    private String userName;
    private String userEmail;
    
    /**
     * Technician who completed the maintenance
     */
    private Long technicianId;
    private String technicianName;
    
    /**
     * Maintenance request details
     */
    private String title;
    private String description;
    private String category;
    private String priority;
    
    /**
     * Request type (SCHEDULED, AD_HOC, EMERGENCY)
     */
    private String requestType;
    
    /**
     * When the maintenance was completed
     */
    private LocalDateTime completedAt;
    
    /**
     * Event metadata
     */
    private String eventType; // Always "MAINTENANCE_COMPLETED"
    private Instant timestamp;
}
