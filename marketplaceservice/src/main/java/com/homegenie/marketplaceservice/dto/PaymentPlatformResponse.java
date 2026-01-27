package com.homegenie.marketplaceservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment Platform API Response DTO
 * 
 * Maps from Payment Service MiniAppPaymentResponse
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentPlatformResponse {
    private Long id;
    private Long userId;
    private String miniAppId;
    private Long orderId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String status;
    private String stripePaymentIntentId;
    private BigDecimal commission;
    private BigDecimal commissionRate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
