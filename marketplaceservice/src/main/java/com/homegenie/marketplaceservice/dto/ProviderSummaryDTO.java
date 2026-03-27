package com.homegenie.marketplaceservice.dto;

import com.homegenie.marketplaceservice.model.ProviderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Provider summary DTO for list view
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderSummaryDTO {
    private Long id;
    private String name;
    private String companyName;
    private ProviderStatus status;
    private Boolean isVerified;
    private BigDecimal averageRating;
    private Integer totalReviews;
    private Integer completedBookings;
    private String profilePhotoUrl;
}
