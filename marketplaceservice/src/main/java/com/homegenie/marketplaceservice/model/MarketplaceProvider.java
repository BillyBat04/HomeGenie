package com.homegenie.marketplaceservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;


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
    
    
    @Column(nullable = false, length = 255)
    private String name;
    
    @Column(nullable = false, unique = true, length = 255)
    private String email;
    
    @Column(nullable = false, length = 20)
    private String phone;
    
    
    @Column(length = 255)
    private String companyName;
    
    @Column(length = 100)
    private String licenseNumber;
    
    
    @Column(columnDefinition = "TEXT")
    private String profilePhotoUrl;
    
    @Column(columnDefinition = "TEXT")
    private String bio;
    
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ProviderStatus status = ProviderStatus.PENDING_VERIFICATION;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean isVerified = false;
    
    
    @Column(nullable = false)
    @Builder.Default
    private Integer totalBookings = 0;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer completedBookings = 0;
    
    @Column(precision = 3, scale = 2)
    private BigDecimal averageRating; 
    
    @Column(nullable = false)
    @Builder.Default
    private Integer totalReviews = 0;
    
    
    @Column(columnDefinition = "TEXT")
    private String serviceAreasJson; 
    
    @Column(columnDefinition = "TEXT")
    private String workingHoursJson; 
    
    
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    private LocalDateTime verifiedAt;
    
    private LocalDateTime lastActiveAt;
    
    
    
    
    
    
    public boolean canAcceptBookings() {
        return status == ProviderStatus.ACTIVE && isVerified;
    }
    
    
    public void updateRating(BigDecimal newRating) {
        if (averageRating == null) {
            averageRating = newRating;
        } else {
            
            BigDecimal totalScore = averageRating.multiply(BigDecimal.valueOf(totalReviews));
            totalScore = totalScore.add(newRating);
            averageRating = totalScore.divide(BigDecimal.valueOf(totalReviews + 1), 2, RoundingMode.HALF_UP);
        }
        totalReviews++;
        updatedAt = LocalDateTime.now();
    }
    
    
    public void verify() {
        isVerified = true;
        verifiedAt = LocalDateTime.now();
        if (status == ProviderStatus.PENDING_VERIFICATION) {
            status = ProviderStatus.ACTIVE;
        }
        updatedAt = LocalDateTime.now();
    }
    
    
    public void suspend(String reason) {
        status = ProviderStatus.SUSPENDED;
        updatedAt = LocalDateTime.now();
    }
    
    
    public void block(String reason) {
        status = ProviderStatus.BLOCKED;
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
