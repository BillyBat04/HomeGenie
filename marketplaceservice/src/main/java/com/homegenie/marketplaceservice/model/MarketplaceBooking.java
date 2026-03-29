package com.homegenie.marketplaceservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;


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
    
    
    @Column(nullable = false, name = "user_id")
    private Long userId; 
    
    
    @Column(nullable = false, name = "service_id")
    private Long serviceId;
    
    @Column(nullable = false, name = "provider_id")
    private Long providerId;
    
    
    @Column(nullable = false, length = 255)
    private String title;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private ServiceCategory category; 
    
    
    @Column(nullable = false)
    private LocalDateTime scheduledAt;
    
    private Integer estimatedDurationMinutes;
    
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String serviceAddress;
    
    @Column(precision = 10, scale = 8)
    private BigDecimal serviceLocationLat;
    
    @Column(precision = 11, scale = 8)
    private BigDecimal serviceLocationLng;
    
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quotedPrice; 
    
    @Column(precision = 10, scale = 2)
    private BigDecimal finalPrice; 
    
    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";
    
    
    @Column(name = "payment_id")
    private Long paymentId; 
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;
    
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;
    
    
    private LocalDateTime assignedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    
    
    @Column(columnDefinition = "TEXT")
    private String imageUrlsJson; 
    
    @Column(columnDefinition = "TEXT")
    private String providerNotes;
    
    @Column(columnDefinition = "TEXT")
    private String customerNotes;
    
    @Column(columnDefinition = "TEXT")
    private String cancellationReason;
    
    
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    
    
    
    
    
    public boolean canCancel() {
        
        return status == BookingStatus.PENDING || status == BookingStatus.CONFIRMED;
    }
    
    
    public boolean requiresPayment() {
        return paymentStatus != PaymentStatus.PAID;
    }
    
    
    public boolean canRate() {
        return status == BookingStatus.COMPLETED;
    }
    
    
    public void confirm(Long paymentId) {
        this.status = BookingStatus.CONFIRMED;
        this.paymentId = paymentId;
        this.paymentStatus = PaymentStatus.PAID;
        this.assignedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    
    public void start() {
        if (status != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Booking must be confirmed before starting");
        }
        this.status = BookingStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    
    public void complete(BigDecimal finalPrice) {
        if (status != BookingStatus.IN_PROGRESS) {
            throw new IllegalStateException("Booking must be in progress before completing");
        }
        this.status = BookingStatus.COMPLETED;
        this.finalPrice = finalPrice != null ? finalPrice : quotedPrice;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    
    public void cancel(String reason) {
        if (!canCancel()) {
            throw new IllegalStateException("Booking cannot be cancelled in current status: " + status);
        }
        this.status = BookingStatus.CANCELLED;
        this.cancellationReason = reason;
        this.cancelledAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    
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
