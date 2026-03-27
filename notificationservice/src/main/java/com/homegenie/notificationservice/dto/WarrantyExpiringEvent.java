package com.homegenie.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Received from maintenance-service via the 'warranty-reminder' Kafka topic.
 * Structure must match WarrantyExpiringEvent published by MaintenanceEventPublisher.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarrantyExpiringEvent {
    private String eventId;
    private Long itemId;
    private String itemName;
    private String itemCategory;
    private String itemBrand;
    private String itemModel;
    private Long userId;
    private String userName;
    private String userEmail;
    private LocalDate warrantyExpiryDate;
    private Integer daysUntilExpiry;
    private LocalDate purchaseDate;
    private String warrantyDocumentUrl;
    /** EXPIRED | EXPIRING_SOON_1DAY | EXPIRING_SOON_7DAYS | EXPIRING_SOON_30DAYS */
    private String urgencyLevel;
    private String eventType;
    private Instant timestamp;
}
