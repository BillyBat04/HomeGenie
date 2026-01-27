package com.homegenie.maintenanceservice.dto;

import com.homegenie.maintenanceservice.model.Item;
import com.homegenie.maintenanceservice.model.ItemCategory;
import com.homegenie.maintenanceservice.model.ItemStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Full response DTO for Item
 * Used for GET /api/items/{id} and POST/PUT responses
 * Contains all item details including calculated fields
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemResponse {

    private Long id;
    private Long userId;
    private String name;
    private ItemCategory category;
    private String brand;
    private String model;
    private LocalDate purchaseDate;
    private LocalDate warrantyExpiryDate;
    private String warrantyDocumentUrl;
    private Integer maintenanceFrequencyDays;
    private LocalDate lastMaintenanceDate;
    private LocalDate nextMaintenanceDate;
    private ItemStatus status;
    private String location;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Calculated fields for frontend convenience
    private Boolean isWarrantyValid;
    private Boolean isWarrantyExpiring;
    private Boolean needsMaintenance;
    private Boolean maintenanceDueSoon;
    private Boolean isMaintenanceOverdue;
    private Integer daysUntilWarrantyExpiry;
    private Integer daysUntilMaintenance;
    private Long totalMaintenanceCount;

    /**
     * Convert Item entity to ItemResponse DTO
     */
    public static ItemResponse fromEntity(Item item) {
        return fromEntity(item, null);
    }

    /**
     * Convert Item entity to ItemResponse DTO with maintenance count
     */
    public static ItemResponse fromEntity(Item item, Long maintenanceCount) {
        ItemResponse response = ItemResponse.builder()
                .id(item.getId())
                .userId(item.getUserId())
                .name(item.getName())
                .category(item.getCategory())
                .brand(item.getBrand())
                .model(item.getModel())
                .purchaseDate(item.getPurchaseDate())
                .warrantyExpiryDate(item.getWarrantyExpiryDate())
                .warrantyDocumentUrl(item.getWarrantyDocumentUrl())
                .maintenanceFrequencyDays(item.getMaintenanceFrequencyDays())
                .lastMaintenanceDate(item.getLastMaintenanceDate())
                .nextMaintenanceDate(item.getNextMaintenanceDate())
                .status(item.getStatus())
                .location(item.getLocation())
                .notes(item.getNotes())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();

        // Calculate convenience fields
        response.setIsWarrantyValid(item.isWarrantyValid());
        response.setIsWarrantyExpiring(item.isWarrantyExpiring());
        response.setNeedsMaintenance(item.needsMaintenance());
        response.setMaintenanceDueSoon(item.maintenanceDueSoon());

        // Calculate days until warranty expiry (negative if expired)
        if (item.getWarrantyExpiryDate() != null) {
            response.setDaysUntilWarrantyExpiry(
                (int) java.time.temporal.ChronoUnit.DAYS.between(
                    LocalDate.now(), 
                    item.getWarrantyExpiryDate()
                )
            );
        }

        // Calculate days until maintenance (negative if overdue)
        if (item.getNextMaintenanceDate() != null) {
            int daysUntil = (int) java.time.temporal.ChronoUnit.DAYS.between(
                LocalDate.now(), 
                item.getNextMaintenanceDate()
            );
            response.setDaysUntilMaintenance(daysUntil);
            response.setIsMaintenanceOverdue(daysUntil < 0);
        }

        // Set maintenance count if provided
        if (maintenanceCount != null) {
            response.setTotalMaintenanceCount(maintenanceCount);
        }

        return response;
    }
}
