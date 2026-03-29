package com.homegenie.maintenanceservice.dto;

import com.homegenie.maintenanceservice.model.Item;
import com.homegenie.maintenanceservice.model.ItemCategory;
import com.homegenie.maintenanceservice.model.ItemStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemSummaryDTO {

    private Long id;
    private String name;
    private ItemCategory category;
    private String brand;
    private String model;
    private ItemStatus status;
    private LocalDate nextMaintenanceDate;
    private LocalDate warrantyExpiryDate;
    private String location;

    private Boolean needsMaintenance;
    private Boolean maintenanceDueSoon;
    private Boolean isWarrantyExpiring;
    private Boolean isWarrantyValid;

    public static ItemSummaryDTO fromEntity(Item item) {
        return ItemSummaryDTO.builder()
                .id(item.getId())
                .name(item.getName())
                .category(item.getCategory())
                .brand(item.getBrand())
                .model(item.getModel())
                .status(item.getStatus())
                .nextMaintenanceDate(item.getNextMaintenanceDate())
                .warrantyExpiryDate(item.getWarrantyExpiryDate())
                .location(item.getLocation())
                .needsMaintenance(item.needsMaintenance())
                .maintenanceDueSoon(item.maintenanceDueSoon())
                .isWarrantyExpiring(item.isWarrantyExpiring())
                .isWarrantyValid(item.isWarrantyValid())
                .build();
    }
}
