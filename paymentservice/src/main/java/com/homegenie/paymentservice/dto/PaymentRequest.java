package com.homegenie.paymentservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Schema(description = "Payment creation request")
public class PaymentRequest {
    
    @Schema(description = "User ID", example = "1001")
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @Schema(description = "Maintenance request ID", example = "2001")
    @NotNull(message = "Request ID is required")
    private Long requestId;
    
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
    
    // Stripe specific fields
    @Schema(description = "Stripe payment method ID", example = "pm_1234567890")
    private String paymentMethodId; // Stripe payment method ID
    
    @Schema(description = "Stripe customer ID", example = "cus_1234567890")
    private String customerId; // Existing Stripe customer ID
    
    @Schema(description = "Auto-capture payment", example = "true")
    private Boolean autoCapture = true;
}
