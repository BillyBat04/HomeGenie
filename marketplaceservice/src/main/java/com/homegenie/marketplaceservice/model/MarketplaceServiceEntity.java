package com.homegenie.marketplaceservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Service catalog entity
 * 
 * Represents a service offered by a provider (e.g., "Emergency Plumbing Repair").
 * Each provider can have multiple services in different categories.
 * 
 * Pricing model supports different units: PER_JOB, PER_HOUR, PER_SQFT, PER_DAY
 */
@Entity
@Table(name = "marketplace_services", indexes = {
    @Index(name = "idx_ms_provider", columnList = "provider_id"),
    @Index(name = "idx_ms_category_status", columnList = "category, status"),
    @Index(name = "idx_ms_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketplaceServiceEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Service Details
    @Column(nullable = false, length = 255)
    private String name;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private ServiceCategory category;
    
    // Provider Reference
    @Column(nullable = false, name = "provider_id")
    private Long providerId;
    
    // Pricing
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private PriceUnit priceUnit = PriceUnit.PER_JOB;
    
    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";
    
    // Availability
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ServiceStatus status = ServiceStatus.ACTIVE;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean isFeatured = false;
    
    // Metadata
    @Column(columnDefinition = "TEXT")
    private String imageUrl;
    
    private Integer durationMinutes; // Estimated duration
    
    // Timestamps
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // ============================================================
    // Domain Logic Methods
    // ============================================================
    
    /**
     * Check if service is available for booking
     */
    public boolean isAvailable() {
        return status == ServiceStatus.ACTIVE;
    }
    
    /**
     * Activate service
     */
    public void activate() {
        status = ServiceStatus.ACTIVE;
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Deactivate service (soft delete)
     */
    public void deactivate() {
        status = ServiceStatus.INACTIVE;
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Suspend service (admin action)
     */
    public void suspend() {
        status = ServiceStatus.SUSPENDED;
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
