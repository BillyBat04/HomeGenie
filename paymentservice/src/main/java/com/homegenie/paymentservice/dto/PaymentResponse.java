package com.homegenie.paymentservice.dto;

import com.homegenie.paymentservice.model.Payment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payment response data")
public class PaymentResponse {
    
    @Schema(description = "Payment ID", example = "1")
    private Long id;
    
    @Schema(description = "User ID", example = "1001")
    private Long userId;
    
    @Schema(description = "Maintenance request ID", example = "2001")
    private Long requestId;
    
    @Schema(description = "Payment amount", example = "150.00")
    private BigDecimal amount;
    
    @Schema(description = "Currency code", example = "USD")
    private String currency;
    
    @Schema(description = "Payment status", example = "COMPLETED", allowableValues = {"PENDING", "COMPLETED", "REFUNDED", "CANCELLED", "FAILED"})
    private String status;
    
    @Schema(description = "Payment method", example = "CARD")
    private String paymentMethod;
    
    @Schema(description = "Stripe payment intent ID", example = "pi_1234567890")
    private String stripePaymentIntentId;
    
    @Schema(description = "Receipt URL", example = "https://stripe.com/receipts/...")
    private String receiptUrl;
    
    @Schema(description = "Payment description", example = "Payment for plumbing repair")
    private String description;
    
    @Schema(description = "Failure reason", example = "Insufficient funds")
    private String failureReason;
    
    @Schema(description = "Payment creation time", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "Payment completion time", example = "2024-01-15T10:35:00")
    private LocalDateTime paidAt;
    
    
    @Schema(description = "Stripe client secret", example = "pi_1234567890_secret_abcdef")
    private String clientSecret;
    
    public static PaymentResponse fromEntity(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .userId(payment.getUserId())
                .requestId(payment.getOrderId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus().name())
                .paymentMethod(payment.getPaymentMethod().name())
                .stripePaymentIntentId(payment.getStripePaymentIntentId())
                .receiptUrl(payment.getReceiptUrl())
                .description(payment.getDescription())
                .failureReason(payment.getFailureReason())
                .createdAt(payment.getCreatedAt())
                .paidAt(payment.getPaidAt())
                .build();
    }
}
