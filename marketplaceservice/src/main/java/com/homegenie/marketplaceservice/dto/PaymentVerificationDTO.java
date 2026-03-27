package com.homegenie.marketplaceservice.dto;

import lombok.*;

/**
 * Minimal projection of a Payment record returned by Payment Service.
 * Only the fields we need to verify the payment are included.
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentVerificationDTO {
    private Long id;
    private Long orderId;
    private String miniAppId;
    private String status;   // "SUCCESS", "PENDING", "FAILED", etc.
}
