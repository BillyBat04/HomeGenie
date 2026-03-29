package com.homegenie.marketplaceservice.dto;

import com.homegenie.marketplaceservice.model.PriceUnit;
import com.homegenie.marketplaceservice.model.ServiceCategory;
import com.homegenie.marketplaceservice.model.ServiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceSummaryDTO {
    private Long id;
    private String name;
    private String description;
    private ServiceCategory category;
    private Long providerId;
    private String providerName;
    private BigDecimal basePrice;
    private PriceUnit priceUnit;
    private String currency;
    private ServiceStatus status;
    private Boolean isFeatured;
    private String imageUrl;
    private Integer durationMinutes;
}
