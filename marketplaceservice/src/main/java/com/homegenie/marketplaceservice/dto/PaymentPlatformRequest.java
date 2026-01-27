package com.homegenie.marketplaceservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Payment Platform API Request DTO
 * 
 * Maps to Payment Service MiniAppPaymentRequest
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentPlatformRequest {
    private Long userId;
    private String miniAppId;
    private Long orderId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String description;
    private String stripeCustomerId;
    private Map<String, String> metadata;
}
