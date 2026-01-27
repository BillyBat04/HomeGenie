package com.homegenie.marketplaceservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Marketplace review entity
 * 
 * Customer reviews for service providers after booking completion.
 * One review per booking (enforced by UNIQUE constraint on booking_id).
 * 
 * Rating: 1-5 stars (1=poor, 5=excellent)
 * Updates provider's average_rating when submitted.
 */
@Entity
@Table(name = "marketplace_reviews", indexes = {
    @Index(name = "idx_mr_provider_visible", columnList = "provider_id, is_visible"),
    @Index(name = "idx_mr_rating", columnList = "rating"),
    @Index(name = "idx_mr_created_at", columnList = "created_at"),
    @Index(name = "idx_mr_user", columnList = "user_id"),
    @Index(name = "idx_mr_booking", columnList = "booking_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketplaceReview {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Relationships
    @Column(nullable = false, unique = true, name = "booking_id")
    private Long bookingId; // One review per booking
    
    @Column(nullable = false, name = "user_id")
    private Long userId;
    
    @Column(nullable = false, name = "provider_id")
    private Long providerId;
    
    // Rating
    @Column(nullable = false)
    private Integer rating; // 1-5 stars
    
    // Review Content
    @Column(length = 255)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String comment;
    
    // Media
    @Column(columnDefinition = "TEXT")
    private String photoUrlsJson; // JSON array
    
    // Status
    @Column(nullable = false)
    @Builder.Default
    private Boolean isVerified = false; // Admin verified
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean isVisible = true;
    
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
     * Validate rating value
     */
    public boolean isValidRating() {
        return rating != null && rating >= 1 && rating <= 5;
    }
    
    /**
     * Verify review (admin action)
     */
    public void verify() {
        isVerified = true;
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Hide review (admin action - spam/abuse)
     */
    public void hide() {
        isVisible = false;
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Show review (restore after hiding)
     */
    public void show() {
        isVisible = true;
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    @PrePersist
    protected void onCreate() {
        if (!isValidRating()) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
    }
}
