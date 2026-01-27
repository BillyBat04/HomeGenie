package com.homegenie.maintenanceservice.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    
    private Long paymentId;
    private String status;
    private BigDecimal amount;
    private String currency;
    private String miniAppId;
    private Long orderId;
    private LocalDateTime createdAt;
    private String transactionId;
}
