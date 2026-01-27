package com.homegenie.marketplaceservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Marketplace booking entity (analogous to MaintenanceRequest)
 * 
 * Represents a customer booking with an external service provider.
 * Integrates with Payment Platform v2 (mini-app aware) and Notification Platform.
 * 
 * Status flow:
 * PENDING → CONFIRMED (after payment) → IN_PROGRESS → COMPLETED → [CANCELLED/REFUNDED]
 * 
 * Payment integration:
 * - paymentId: Links to Payment Platform v2 with miniAppId="marketplace"
 * - paymentStatus: Separate tracking for payment state
 */
@Entity
@Table(name = "marketplace_bookings", indexes = {
    @Index(name = "idx_mb_user_status", columnList = "user_id, status"),
    @Index(name = "idx_mb_provider_status", columnList = "provider_id, status"),
    @Index(name = "idx_mb_service", columnList = "service_id"),
    @Index(name = "idx_mb_created_at", columnList = "created_at"),
    @Index(name = "idx_mb_scheduled_at", columnList = "scheduled_at"),
    @Index(name = "idx_mb_payment", columnList = "payment_id"),
    @Index(name = "idx_mb_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketplaceBooking {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Customer Reference
    @Column(nullable = false, name = "user_id")
    private Long userId; // HomeGenie user
    
    // Service Reference
    @Column(nullable = false, name = "service_id")
    private Long serviceId;
    
    @Column(nullable = false, name = "provider_id")
    private Long providerId;
    
    // Booking Details
    @Column(nullable = false, length = 255)
    private String title;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private ServiceCategory category; // Denormalized from service
    
    // Scheduling
    @Column(nullable = false)
    private LocalDateTime scheduledAt;
    
    private Integer estimatedDurationMinutes;
    
    // Location
    @Column(nullable = false, columnDefinition = "TEXT")
    private String serviceAddress;
    
    @Column(precision = 10, scale = 8)
    private BigDecimal serviceLocationLat;
    
    @Column(precision = 11, scale = 8)
    private BigDecimal serviceLocationLng;
    
    // Pricing
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quotedPrice; // Initial quote
    
    @Column(precision = 10, scale = 2)
    private BigDecimal finalPrice; // After job completion
    
    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";
    
    // Payment Integration (Mini-App Aware!)
    @Column(name = "payment_id")
    private Long paymentId; // Reference to payments table (Payment Platform v2)
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;
    
    // Status Tracking
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;
    
    // Assignment Timestamps
    private LocalDateTime assignedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    
    // Additional Info
    @Column(columnDefinition = "TEXT")
    private String imageUrlsJson; // JSON array of image URLs
    
    @Column(columnDefinition = "TEXT")
    private String providerNotes;
    
    @Column(columnDefinition = "TEXT")
    private String customerNotes;
    
    @Column(columnDefinition = "TEXT")
    private String cancellationReason;
    
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
     * Check if booking can be cancelled
     */
    public boolean canCancel() {
        // Can cancel if not yet started or completed
        return status == BookingStatus.PENDING || status == BookingStatus.CONFIRMED;
    }
    
    /**
     * Check if booking requires payment
     */
    public boolean requiresPayment() {
        return paymentStatus != PaymentStatus.PAID;
    }
    
    /**
     * Check if booking can be rated
     */
    public boolean canRate() {
        return status == BookingStatus.COMPLETED;
    }
    
    /**
     * Confirm booking (after payment success)
     */
    public void confirm(Long paymentId) {
        this.status = BookingStatus.CONFIRMED;
        this.paymentId = paymentId;
        this.paymentStatus = PaymentStatus.PAID;
        this.assignedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Start service (provider action)
     */
    public void start() {
        if (status != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Booking must be confirmed before starting");
        }
        this.status = BookingStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Complete service (provider action)
     */
    public void complete(BigDecimal finalPrice) {
        if (status != BookingStatus.IN_PROGRESS) {
            throw new IllegalStateException("Booking must be in progress before completing");
        }
        this.status = BookingStatus.COMPLETED;
        this.finalPrice = finalPrice != null ? finalPrice : quotedPrice;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Cancel booking
     */
    public void cancel(String reason) {
        if (!canCancel()) {
            throw new IllegalStateException("Booking cannot be cancelled in current status: " + status);
        }
        this.status = BookingStatus.CANCELLED;
        this.cancellationReason = reason;
        this.cancelledAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Refund booking (after cancellation with payment)
     */
    public void refund() {
        if (status != BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking must be cancelled before refunding");
        }
        this.status = BookingStatus.REFUNDED;
        this.paymentStatus = PaymentStatus.REFUNDED;
        this.updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
