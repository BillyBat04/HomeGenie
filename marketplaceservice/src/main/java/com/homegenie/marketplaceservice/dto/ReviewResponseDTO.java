package com.homegenie.marketplaceservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Review response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponseDTO {
    private Long id;
    private Long bookingId;
    private Long userId;
    private Long providerId;
    private Integer rating;
    private String title;
    private String comment;
    private String photoUrlsJson;
    private Boolean isVerified;
    private Boolean isVisible;
    private LocalDateTime createdAt;
    
    // Optional nested data
    private String userName;
    private String providerName;
}
