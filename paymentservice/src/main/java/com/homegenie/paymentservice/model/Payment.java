package com.homegenie.paymentservice.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payment_user", columnList = "userId"),
    @Index(name = "idx_payment_order_id", columnList = "orderId"),
    @Index(name = "idx_payment_mini_app_id", columnList = "miniAppId"),
    @Index(name = "idx_payment_user_miniapp", columnList = "userId, miniAppId"),
    @Index(name = "idx_payment_status_created", columnList = "status, createdAt"),
    @Index(name = "idx_payment_stripe_intent", columnList = "stripePaymentIntentId"),
    @Index(name = "idx_payment_commission", columnList = "miniAppId, status, createdAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long orderId; // Generic order ID (maintenance request ID, booking ID, etc.)

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String miniAppId = "maintenance"; // Mini-app identifier

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal commission = BigDecimal.ZERO; // Platform commission

    @Column(precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal commissionRate = BigDecimal.ZERO; // Commission rate (0.0 - 1.0)

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Column(unique = true)
    private String stripePaymentIntentId;

    @Column(unique = true)
    private String stripeChargeId;

    private String stripeCustomerId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String receiptUrl;

    private String failureReason;

    @Column(name = "metadata_json", columnDefinition = "jsonb")
    private String metadataJson; // Mini-app specific metadata

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    private LocalDateTime paidAt;

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum PaymentStatus {
        PENDING,
        PROCESSING,
        SUCCEEDED,
        FAILED,
        CANCELED,
        REFUNDED,
        PARTIALLY_REFUNDED
    }

    public enum PaymentMethod {
        CREDIT_CARD,
        DEBIT_CARD,
        UPI,
        NET_BANKING,
        WALLET,
        BANK_TRANSFER
    }
}
