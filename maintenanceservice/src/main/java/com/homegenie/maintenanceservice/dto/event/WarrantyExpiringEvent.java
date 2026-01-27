package com.homegenie.maintenanceservice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Event published when an item's warranty is expiring soon
 * Used for warranty renewal reminders to users
 * 
 * Triggered by ItemMaintenanceScheduler daily cron job
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarrantyExpiringEvent {
    
    /**
     * Unique event identifier
     */
    private String eventId;
    
    /**
     * Item with expiring warranty
     */
    private Long itemId;
    private String itemName;
    private String itemCategory;
    private String itemBrand;
    private String itemModel;
    
    /**
     * Owner of the item
     */
    private Long userId;
    private String userName;
    private String userEmail;
    
    /**
     * Warranty details
     */
    private LocalDate warrantyExpiryDate;
    private Integer daysUntilExpiry;
    private LocalDate purchaseDate;
    private String warrantyDocumentUrl;
    
    /**
     * Alert urgency level
     * - EXPIRED: warranty already expired
     * - EXPIRING_SOON_1DAY: expires within 1 day
     * - EXPIRING_SOON_7DAYS: expires within 7 days
     * - EXPIRING_SOON_30DAYS: expires within 30 days
     */
    private String urgencyLevel;
    
    /**
     * Event metadata
     */
    private String eventType; // Always "WARRANTY_EXPIRING"
    private Instant timestamp;
}
