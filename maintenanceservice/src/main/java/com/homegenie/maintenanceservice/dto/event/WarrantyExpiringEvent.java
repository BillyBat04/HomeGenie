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
    
    
    private String urgencyLevel;
    
    
    private String eventType; 
    private Instant timestamp;
}
