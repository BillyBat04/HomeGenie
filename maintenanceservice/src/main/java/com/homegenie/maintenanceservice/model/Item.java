package com.homegenie.maintenanceservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Entity
@Table(name = "items", indexes = {
    @Index(name = "idx_items_user_status", columnList = "userId, status"),
    @Index(name = "idx_items_next_maintenance", columnList = "nextMaintenanceDate"),
    @Index(name = "idx_items_warranty_expiry", columnList = "warrantyExpiryDate"),
    @Index(name = "idx_items_category", columnList = "category")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long userId;
    @Column(nullable = false, length = 255)
    private String name;
   
    @Enumerated(EnumType.STRING)    
    @Column(nullable = false, length = 50)
    private ItemCategory category;
    
    @Column(length = 100)
    private String brand;
    
    @Column(length = 100)
    private String model;
    private LocalDate purchaseDate;

    private LocalDate warrantyExpiryDate;

    @Column(length = 500)
    private String warrantyDocumentUrl;

    @Column(nullable = false)
    @Builder.Default
    private Integer maintenanceFrequencyDays = 180;

    private LocalDate lastMaintenanceDate;

    private LocalDate nextMaintenanceDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ItemStatus status = ItemStatus.ACTIVE;

    @Column(length = 255)
    private String location;

    @Column(length = 2000)
    private String notes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;


    public boolean isWarrantyExpiring() {
        if (warrantyExpiryDate == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysFromNow = today.plusDays(30);
        return !warrantyExpiryDate.isBefore(today) 
            && warrantyExpiryDate.isBefore(thirtyDaysFromNow);
    }

    public boolean isWarrantyValid() {
        if (warrantyExpiryDate == null) {
            return false;
        }
        return warrantyExpiryDate.isAfter(LocalDate.now());
    }

    public boolean isWarrantyExpired() {
        if (warrantyExpiryDate == null) {
            return false; 
        }
        return warrantyExpiryDate.isBefore(LocalDate.now());
    }
    public boolean needsMaintenance() {
        if (nextMaintenanceDate == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        return nextMaintenanceDate.isBefore(today) 
            || nextMaintenanceDate.isEqual(today);
    }
    public boolean maintenanceDueSoon() {
        if (nextMaintenanceDate == null) {
            return false;
        }
        LocalDate sevenDaysFromNow = LocalDate.now().plusDays(7);
        return nextMaintenanceDate.isAfter(LocalDate.now()) 
            && nextMaintenanceDate.isBefore(sevenDaysFromNow);
    }
    public void calculateNextMaintenanceDate() {
        if (lastMaintenanceDate != null && maintenanceFrequencyDays != null) {
            this.nextMaintenanceDate = lastMaintenanceDate.plusDays(maintenanceFrequencyDays);
        } else if (purchaseDate != null && maintenanceFrequencyDays != null) {
            // If no maintenance yet, calculate from purchase date
            this.nextMaintenanceDate = purchaseDate.plusDays(maintenanceFrequencyDays);
        }
    }
    public void recordMaintenanceCompleted() {
        this.lastMaintenanceDate = LocalDate.now();
        calculateNextMaintenanceDate();
        this.updatedAt = LocalDateTime.now();
        
        // If item was under repair, mark as active
        if (this.status == ItemStatus.UNDER_REPAIR) {
            this.status = ItemStatus.ACTIVE;
        }
    }
    public void markUnderRepair() {
        if (this.status == ItemStatus.RETIRED) {
            throw new IllegalStateException("Cannot repair a retired item");
        }
        this.status = ItemStatus.UNDER_REPAIR;
        this.updatedAt = LocalDateTime.now();
    }
    public void markActive() {
        if (this.status == ItemStatus.RETIRED) {
            throw new IllegalStateException("Cannot activate a retired item");
        }
        this.status = ItemStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }
    public void retire() {
        this.status = ItemStatus.RETIRED;
        this.updatedAt = LocalDateTime.now();
    }
    public Category getMaintenanceCategory() {
        return category.toMaintenanceCategory();
    }
    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
        
        if (this.nextMaintenanceDate == null && this.status == ItemStatus.ACTIVE) {
            calculateNextMaintenanceDate();
        }
        validateBusinessRules();
    }
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        validateBusinessRules();
    }
    private void validateBusinessRules() {
        if (maintenanceFrequencyDays != null && 
            (maintenanceFrequencyDays <= 0 || maintenanceFrequencyDays > 3650)) {
            throw new IllegalArgumentException("Maintenance frequency must be 1-3650 days");
        }
        
        if (warrantyExpiryDate != null && purchaseDate != null 
            && warrantyExpiryDate.isBefore(purchaseDate)) {
            throw new IllegalArgumentException("Warranty expiry cannot be before purchase date");
        }
        
        if (lastMaintenanceDate != null && purchaseDate != null 
            && lastMaintenanceDate.isBefore(purchaseDate)) {
            throw new IllegalArgumentException("Last maintenance cannot be before purchase date");
        }
    }
}
