package com.homegenie.maintenanceservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Item (Aggregate Root) - Household item/appliance tracked in the system
 * 
 * Business Rules:
 * - Item is the ONLY aggregate root for maintenance lifecycle
 * - MaintenanceRequest CANNOT update Item directly
 * - Only ItemService can modify item state
 * - Domain logic is encapsulated in this entity
 */
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

    /**
     * Owner of the item
     */
    @Column(nullable = false)
    private Long userId;

    /**
     * User-friendly name (e.g., "Living Room AC", "Kitchen Fridge")
     */
    @Column(nullable = false, length = 255)
    private String name;

    /**
     * Category of the item
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ItemCategory category;

    /**
     * Brand name (e.g., "Samsung", "LG")
     */
    @Column(length = 100)
    private String brand;

    /**
     * Model number/name
     */
    @Column(length = 100)
    private String model;

    /**
     * Date when item was purchased
     */
    private LocalDate purchaseDate;

    /**
     * Date when warranty expires
     */
    private LocalDate warrantyExpiryDate;

    /**
     * URL to warranty document (stored in S3)
     */
    @Column(length = 500)
    private String warrantyDocumentUrl;

    /**
     * How often maintenance should be done (in days)
     * Default: 180 days (6 months)
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer maintenanceFrequencyDays = 180;

    /**
     * When was the last maintenance performed
     */
    private LocalDate lastMaintenanceDate;

    /**
     * When is the next maintenance due (calculated)
     */
    private LocalDate nextMaintenanceDate;

    /**
     * Current status of the item
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ItemStatus status = ItemStatus.ACTIVE;

    /**
     * Physical location of the item (e.g., "Living Room Floor 2")
     */
    @Column(length = 255)
    private String location;

    /**
     * Additional notes about the item
     */
    @Column(length = 2000)
    private String notes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // ==================== DOMAIN LOGIC ====================

    /**
     * Check if warranty is expiring soon (within 30 days)
     */
    public boolean isWarrantyExpiring() {
        if (warrantyExpiryDate == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysFromNow = today.plusDays(30);
        return !warrantyExpiryDate.isBefore(today) 
            && warrantyExpiryDate.isBefore(thirtyDaysFromNow);
    }

    /**
     * Check if warranty is still valid
     */
    public boolean isWarrantyValid() {
        if (warrantyExpiryDate == null) {
            return false;
        }
        return warrantyExpiryDate.isAfter(LocalDate.now());
    }

    /**
     * Check if warranty has expired
     */
    public boolean isWarrantyExpired() {
        if (warrantyExpiryDate == null) {
            return false; // No warranty = not expired
        }
        return warrantyExpiryDate.isBefore(LocalDate.now());
    }

    /**
     * Check if maintenance is due now
     */
    public boolean needsMaintenance() {
        if (nextMaintenanceDate == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        return nextMaintenanceDate.isBefore(today) 
            || nextMaintenanceDate.isEqual(today);
    }

    /**
     * Check if maintenance is due soon (within 7 days)
     */
    public boolean maintenanceDueSoon() {
        if (nextMaintenanceDate == null) {
            return false;
        }
        LocalDate sevenDaysFromNow = LocalDate.now().plusDays(7);
        return nextMaintenanceDate.isAfter(LocalDate.now()) 
            && nextMaintenanceDate.isBefore(sevenDaysFromNow);
    }

    /**
     * Calculate next maintenance date based on last maintenance and frequency
     * This is the ONLY method that updates nextMaintenanceDate
     */
    public void calculateNextMaintenanceDate() {
        if (lastMaintenanceDate != null && maintenanceFrequencyDays != null) {
            this.nextMaintenanceDate = lastMaintenanceDate.plusDays(maintenanceFrequencyDays);
        } else if (purchaseDate != null && maintenanceFrequencyDays != null) {
            // If no maintenance yet, calculate from purchase date
            this.nextMaintenanceDate = purchaseDate.plusDays(maintenanceFrequencyDays);
        }
    }

    /**
     * Record that maintenance was completed today
     * This is called by ItemService when consuming MaintenanceCompletedEvent
     * 
     * IMPORTANT: Only ItemService should call this method
     */
    public void recordMaintenanceCompleted() {
        this.lastMaintenanceDate = LocalDate.now();
        calculateNextMaintenanceDate();
        this.updatedAt = LocalDateTime.now();
        
        // If item was under repair, mark as active
        if (this.status == ItemStatus.UNDER_REPAIR) {
            this.status = ItemStatus.ACTIVE;
        }
    }

    /**
     * Mark item as under repair
     */
    public void markUnderRepair() {
        if (this.status == ItemStatus.RETIRED) {
            throw new IllegalStateException("Cannot repair a retired item");
        }
        this.status = ItemStatus.UNDER_REPAIR;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Mark item as active
     */
    public void markActive() {
        if (this.status == ItemStatus.RETIRED) {
            throw new IllegalStateException("Cannot activate a retired item");
        }
        this.status = ItemStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Retire the item (cannot be undone)
     */
    public void retire() {
        this.status = ItemStatus.RETIRED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Get maintenance category for creating requests
     */
    public Category getMaintenanceCategory() {
        return category.toMaintenanceCategory();
    }

    /**
     * Set timestamps on entity creation
     */
    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
        
        // Calculate next maintenance date if not set
        if (this.nextMaintenanceDate == null && this.status == ItemStatus.ACTIVE) {
            calculateNextMaintenanceDate();
        }
        
        // Run validations
        validateBusinessRules();
    }

    /**
     * Update timestamp on entity update
     */
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        validateBusinessRules();
    }
    
    /**
     * Validate business rules
     */
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
