package com.homegenie.marketplaceservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * External service provider entity
 * 
 * Represents third-party service providers (plumbers, electricians, cleaners)
 * who offer services through the Marketplace mini-app.
 * 
 * Status flow: PENDING_VERIFICATION → ACTIVE → SUSPENDED → BLOCKED
 * 
 * Performance metrics (average_rating, total_reviews) are denormalized
 * for quick access and updated on each new review submission.
 */
@Entity
@Table(name = "marketplace_providers", indexes = {
    @Index(name = "idx_mp_status", columnList = "status"),
    @Index(name = "idx_mp_email", columnList = "email"),
    @Index(name = "idx_mp_rating", columnList = "average_rating, total_reviews")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketplaceProvider {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Basic Info
    @Column(nullable = false, length = 255)
    private String name;
    
    @Column(nullable = false, unique = true, length = 255)
    private String email;
    
    @Column(nullable = false, length = 20)
    private String phone;
    
    // Business Details
    @Column(length = 255)
    private String companyName;
    
    @Column(length = 100)
    private String licenseNumber;
    
    // Profile
    @Column(columnDefinition = "TEXT")
    private String profilePhotoUrl;
    
    @Column(columnDefinition = "TEXT")
    private String bio;
    
    // Status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ProviderStatus status = ProviderStatus.PENDING_VERIFICATION;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean isVerified = false;
    
    // Performance Metrics (denormalized)
    @Column(nullable = false)
    @Builder.Default
    private Integer totalBookings = 0;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer completedBookings = 0;
    
    @Column(precision = 3, scale = 2)
    private BigDecimal averageRating; // 0.00 to 5.00
    
    @Column(nullable = false)
    @Builder.Default
    private Integer totalReviews = 0;
    
    // Availability (stored as JSON text)
    @Column(columnDefinition = "TEXT")
    private String serviceAreasJson; // JSON array: ["District 1", "District 3"]
    
    @Column(columnDefinition = "TEXT")
    private String workingHoursJson; // JSON object: {"monday": "09:00-17:00"}
    
    // Timestamps
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    private LocalDateTime verifiedAt;
    
    private LocalDateTime lastActiveAt;
    
    // ============================================================
    // Domain Logic Methods
    // ============================================================
    
    /**
     * Check if provider can accept new bookings
     */
    public boolean canAcceptBookings() {
        return status == ProviderStatus.ACTIVE && isVerified;
    }
    
    /**
     * Update average rating after new review
     */
    public void updateRating(BigDecimal newRating) {
        if (averageRating == null) {
            averageRating = newRating;
        } else {
            // Weighted average: (old_avg * old_count + new_rating) / new_count
            BigDecimal totalScore = averageRating.multiply(BigDecimal.valueOf(totalReviews));
            totalScore = totalScore.add(newRating);
            averageRating = totalScore.divide(BigDecimal.valueOf(totalReviews + 1), 2, RoundingMode.HALF_UP);
        }
        totalReviews++;
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Verify provider (admin action)
     */
    public void verify() {
        isVerified = true;
        verifiedAt = LocalDateTime.now();
        if (status == ProviderStatus.PENDING_VERIFICATION) {
            status = ProviderStatus.ACTIVE;
        }
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Suspend provider (admin action)
     */
    public void suspend(String reason) {
        status = ProviderStatus.SUSPENDED;
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Block provider permanently (admin action)
     */
    public void block(String reason) {
        status = ProviderStatus.BLOCKED;
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
