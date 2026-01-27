package com.homegenie.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment event from Payment Service via Kafka
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {
    private Long paymentId;
    private Long userId;
    private Long requestId;
    private BigDecimal amount;
    private String currency;
    private String status; // SUCCEEDED, FAILED, REFUNDED
    private String paymentMethod;
    private String receiptUrl;
    private String failureReason;
    private LocalDateTime eventTime;
    private String eventType; // PAYMENT_CONFIRMED, PAYMENT_FAILED, PAYMENT_REFUNDED
}
