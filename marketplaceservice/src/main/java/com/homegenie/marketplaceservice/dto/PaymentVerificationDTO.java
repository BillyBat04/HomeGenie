package com.homegenie.marketplaceservice.dto;

import lombok.*;


@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentVerificationDTO {
    private Long id;
    private Long orderId;
    private String miniAppId;
    private String status;   
}
