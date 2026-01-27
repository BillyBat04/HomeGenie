package com.homegenie.paymentservice.dto;

import com.homegenie.paymentservice.model.Payment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Mini-app payment response")
public class MiniAppPaymentResponse {
    
    @Schema(description = "Payment ID", example = "12345")
    private Long id;
    
    @Schema(description = "User ID", example = "1001")
    private Long userId;
    
    @Schema(description = "Mini-app identifier", example = "maintenance")
    private String miniAppId;
    
    @Schema(description = "Order ID", example = "2001")
    private Long orderId;
    
    @Schema(description = "Payment amount", example = "150.00")
    private BigDecimal amount;
    
    @Schema(description = "Platform commission", example = "15.00")
    private BigDecimal commission;
    
    @Schema(description = "Commission rate", example = "0.10")
    private BigDecimal commissionRate;
    
    @Schema(description = "Currency code", example = "USD")
    private String currency;
    
    @Schema(description = "Payment status", example = "SUCCEEDED")
    private String status;
    
    @Schema(description = "Payment method", example = "CREDIT_CARD")
    private String paymentMethod;
    
    @Schema(description = "Stripe payment intent ID")
    private String stripePaymentIntentId;
    
    @Schema(description = "Payment description")
    private String description;
    
    @Schema(description = "Receipt URL")
    private String receiptUrl;
    
    @Schema(description = "Payment created timestamp")
    private LocalDateTime createdAt;
    
    @Schema(description = "Payment paid timestamp")
    private LocalDateTime paidAt;
    
    public static MiniAppPaymentResponse fromPayment(Payment payment) {
        return MiniAppPaymentResponse.builder()
                .id(payment.getId())
                .userId(payment.getUserId())
                .miniAppId(payment.getMiniAppId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .commission(payment.getCommission())
                .commissionRate(payment.getCommissionRate())
                .currency(payment.getCurrency())
                .status(payment.getStatus().name())
                .paymentMethod(payment.getPaymentMethod().name())
                .stripePaymentIntentId(payment.getStripePaymentIntentId())
                .description(payment.getDescription())
                .receiptUrl(payment.getReceiptUrl())
                .createdAt(payment.getCreatedAt())
                .paidAt(payment.getPaidAt())
                .build();
    }
}
