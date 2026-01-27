package com.homegenie.maintenanceservice.dto;

import com.homegenie.maintenanceservice.model.ItemCategory;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for creating or updating an Item
 * Used for both POST /api/items and PUT /api/items/{id}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemRequest {

    /**
     * User-friendly name for the item
     * Required, 3-255 characters
     */
    @NotBlank(message = "Item name is required")
    @Size(min = 3, max = 255, message = "Item name must be 3-255 characters")
    private String name;

    /**
     * Category of the item
     * Required
     */
    @NotNull(message = "Item category is required")
    private ItemCategory category;

    /**
     * Brand name (e.g., "Samsung", "LG")
     * Optional, max 100 characters
     */
    @Size(max = 100, message = "Brand name must not exceed 100 characters")
    private String brand;

    /**
     * Model number or name
     * Optional, max 100 characters
     */
    @Size(max = 100, message = "Model must not exceed 100 characters")
    private String model;

    /**
     * Date when item was purchased
     * Optional, must not be in the future
     */
    @PastOrPresent(message = "Purchase date cannot be in the future")
    private LocalDate purchaseDate;

    /**
     * Date when warranty expires
     * Optional, must be after purchase date (validated in service)
     */
    private LocalDate warrantyExpiryDate;

    /**
     * Base64-encoded warranty document or URL
     * Optional, will be uploaded to S3 if base64
     * Note: Size validation done in Service layer (max 5MB decoded)
     */
    private String warrantyDocumentBase64;

    /**
     * How often maintenance should be done (in days)
     * Optional, default: 180 days (6 months)
     * Range: 1-3650 days (max 10 years)
     */
    @Min(value = 1, message = "Maintenance frequency must be at least 1 day")
    @Max(value = 3650, message = "Maintenance frequency must not exceed 3650 days (10 years)")
    private Integer maintenanceFrequencyDays;

    /**
     * Physical location of the item
     * Optional, max 255 characters
     */
    @Size(max = 255, message = "Location must not exceed 255 characters")
    private String location;

    /**
     * Additional notes about the item
     * Optional, max 2000 characters
     */
    @Size(max = 2000, message = "Notes must not exceed 2000 characters")
    private String notes;

    /**
     * Item status (for update operations)
     * Optional, only used in PUT /api/items/{id}
     * For status transitions, prefer dedicated endpoints
     */
    private com.homegenie.maintenanceservice.model.ItemStatus status;
}
