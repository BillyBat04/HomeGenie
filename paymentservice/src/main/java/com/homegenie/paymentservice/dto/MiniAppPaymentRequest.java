package com.homegenie.paymentservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
@Schema(description = "Mini-app payment creation request")
public class MiniAppPaymentRequest {
    
    @Schema(description = "User ID", example = "1001")
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @Schema(description = "Mini-app identifier", example = "maintenance", 
            allowableValues = {"maintenance", "marketplace", "booking"})
    @NotBlank(message = "Mini-app ID is required")
    @Size(max = 50, message = "Mini-app ID cannot exceed 50 characters")
    private String miniAppId;
    
    @Schema(description = "Order ID (maintenance request ID, booking ID, etc.)", example = "2001")
    @NotNull(message = "Order ID is required")
    private Long orderId;
    
    @Schema(description = "Payment amount", example = "150.00")
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Amount must be at least 1.00")
    @DecimalMax(value = "1000000.00", message = "Amount cannot exceed 1,000,000.00")
    private BigDecimal amount;
    
    @Schema(description = "Currency code", example = "USD")
    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters (e.g., USD)")
    private String currency = "USD";
    
    @Schema(description = "Payment method type", example = "card")
    @NotNull(message = "Payment method is required")
    private String paymentMethod;
    
    @Schema(description = "Payment description", example = "Payment for plumbing repair")
    private String description;
    
    @Schema(description = "Mini-app specific metadata", example = "{\"technicianId\": 501, \"serviceType\": \"plumbing\"}")
    private Map<String, Object> metadata;
    
    // Stripe specific fields
    @Schema(description = "Stripe payment method ID", example = "pm_1234567890")
    private String paymentMethodId; // Stripe payment method ID
    
    @Schema(description = "Stripe customer ID (optional, will be created if not provided)", 
            example = "cus_1234567890")
    private String stripeCustomerId;
}
