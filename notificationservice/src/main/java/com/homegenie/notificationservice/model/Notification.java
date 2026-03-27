package com.homegenie.notificationservice.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Notification entity")
public class Notification {

    @Schema(description = "Notification ID", example = "1")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Schema(description = "Notification type", example = "PAYMENT_CONFIRMATION")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Schema(description = "Notification channel", example = "EMAIL")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationChannel channel;

    @Schema(description = "Notification status", example = "DELIVERED")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    @Schema(description = "Recipient email or phone", example = "user@example.com")
    @Column(nullable = false)
    private String recipient; // Email or phone number

    @Schema(description = "Recipient name", example = "John Doe")
    private String recipientName;

    @Schema(description = "Notification subject", example = "Payment Confirmed")
    @Column(nullable = false)
    private String subject;

    @Schema(description = "Plain text content", example = "Your payment of $150 has been confirmed")
    @Column(columnDefinition = "TEXT")
    private String content;

    @Schema(description = "HTML content for email", example = "<p>Your payment has been confirmed</p>")
    @Column(name = "html_content", columnDefinition = "TEXT")
    private String htmlContent;

    // Related entities
    @Schema(description = "User ID", example = "1001")
    private Long userId;
    
    @Schema(description = "Mini-app identifier", example = "maintenance")
    @Column(name = "mini_app_id", nullable = false, length = 50)
    @Builder.Default
    private String miniAppId = "maintenance";
    
    @Schema(description = "Maintenance request ID", example = "2001")
    private Long requestId;
    
    @Schema(description = "Payment ID", example = "3001")
    private Long paymentId;
    
    @Schema(description = "Invoice ID", example = "4001")
    private Long invoiceId;

    // Template reference
    @Schema(description = "Email template ID", example = "payment-confirmation")
    private String templateId;

    /**
     * Idempotency key — stores the eventId from the Kafka message.
     * Before saving a new notification we check if one with this eventId already exists.
     * This prevents duplicate emails when Kafka re-delivers a message (at-least-once delivery).
     * Example: Kafka consumer crashes after processing but before committing offset →
     *          Kafka retries → without this check the email would be sent twice.
     */
    @Schema(description = "Kafka event ID used for idempotency", example = "550e8400-e29b-41d4-a716-446655440000")
    @Column(name = "event_id", unique = true)
    private String eventId;

    // Metadata
    @Schema(description = "Additional metadata in JSON", example = "{\"amount\": 150}")
    @Column(columnDefinition = "TEXT")
    private String metadata; // JSON string for additional data

    // Retry tracking
    @Schema(description = "Current retry count", example = "0")
    @Builder.Default
    private Integer retryCount = 0;
    
    @Schema(description = "Maximum retry attempts", example = "3")
    @Builder.Default
    private Integer maxRetries = 3;

    @Schema(description = "Error message if failed", example = "SMTP connection timeout")
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    // Timestamps
    @Schema(description = "Creation timestamp", example = "2024-01-15T10:00:00")
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Schema(description = "Sent timestamp", example = "2024-01-15T10:00:05")
    private LocalDateTime sentAt;
    
    @Schema(description = "Delivered timestamp", example = "2024-01-15T10:00:10")
    private LocalDateTime deliveredAt;
    
    @Schema(description = "Failed timestamp", example = "2024-01-15T10:00:15")
    private LocalDateTime failedAt;
    
    @Schema(description = "Read timestamp", example = "2024-01-15T10:30:00")
    private LocalDateTime readAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = NotificationStatus.PENDING;
        }
        if (retryCount == null) {
            retryCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (status == NotificationStatus.SENT && sentAt == null) {
            sentAt = LocalDateTime.now();
        }
        if (status == NotificationStatus.DELIVERED && deliveredAt == null) {
            deliveredAt = LocalDateTime.now();
        }
        if (status == NotificationStatus.FAILED && failedAt == null) {
            failedAt = LocalDateTime.now();
        }
        if (status == NotificationStatus.READ && readAt == null) {
            readAt = LocalDateTime.now();
        }
    }

    // Enums
    public enum NotificationType {
        PAYMENT_CONFIRMATION,
        PAYMENT_FAILED,
        PAYMENT_REFUNDED,
        INVOICE_SENT,
        INVOICE_OVERDUE,
        INVOICE_PAID,
        MAINTENANCE_REQUEST_CREATED,
        MAINTENANCE_REQUEST_ASSIGNED,
        MAINTENANCE_REQUEST_UPDATED,
        MAINTENANCE_REQUEST_COMPLETED,
        MAINTENANCE_REMINDER,
        WARRANTY_EXPIRY_ALERT,
        SYSTEM_ALERT,
        WELCOME_EMAIL,
        PASSWORD_RESET
    }

    public enum NotificationChannel {
        EMAIL,
        SMS,
        PUSH_NOTIFICATION,
        IN_APP
    }

    public enum NotificationStatus {
        PENDING,      // Waiting to be sent
        SENDING,      // Currently being sent
        SENT,         // Successfully sent
        DELIVERED,    // Delivered to recipient
        READ,         // Opened/read by recipient
        FAILED,       // Failed to send
        RETRYING,     // Retrying after failure
        CANCELLED     // Cancelled before sending
    }
}
