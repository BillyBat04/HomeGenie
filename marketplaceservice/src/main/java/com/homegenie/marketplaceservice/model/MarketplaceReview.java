package com.homegenie.marketplaceservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


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
    
    
    @Column(nullable = false, unique = true, name = "booking_id")
    private Long bookingId; 
    
    @Column(nullable = false, name = "user_id")
    private Long userId;
    
    @Column(nullable = false, name = "provider_id")
    private Long providerId;
    
    
    @Column(nullable = false)
    private Integer rating; 
    
    
    @Column(length = 255)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String comment;
    
    
    @Column(columnDefinition = "TEXT")
    private String photoUrlsJson; 
    
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean isVerified = false; 
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean isVisible = true;
    
    
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    
    
    
    
    
    public boolean isValidRating() {
        return rating != null && rating >= 1 && rating <= 5;
    }
    
    
    public void verify() {
        isVerified = true;
        updatedAt = LocalDateTime.now();
    }
    
    
    public void hide() {
        isVisible = false;
        updatedAt = LocalDateTime.now();
    }
    
    
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
