package com.homegenie.maintenanceservice.dto;

import com.homegenie.maintenanceservice.model.ItemCategory;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemRequest {

    
    @NotBlank(message = "Item name is required")
    @Size(min = 3, max = 255, message = "Item name must be 3-255 characters")
    private String name;

    
    @NotNull(message = "Item category is required")
    private ItemCategory category;

    
    @Size(max = 100, message = "Brand name must not exceed 100 characters")
    private String brand;

    
    @Size(max = 100, message = "Model must not exceed 100 characters")
    private String model;

    
    @PastOrPresent(message = "Purchase date cannot be in the future")
    private LocalDate purchaseDate;

    
    private LocalDate warrantyExpiryDate;

    
    private String warrantyDocumentBase64;

    
    @Min(value = 1, message = "Maintenance frequency must be at least 1 day")
    @Max(value = 3650, message = "Maintenance frequency must not exceed 3650 days (10 years)")
    private Integer maintenanceFrequencyDays;

    
    @Size(max = 255, message = "Location must not exceed 255 characters")
    private String location;

    
    @Size(max = 2000, message = "Notes must not exceed 2000 characters")
    private String notes;

    
    private com.homegenie.maintenanceservice.model.ItemStatus status;
}
