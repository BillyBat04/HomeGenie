package com.homegenie.marketplaceservice.dto;

import com.homegenie.marketplaceservice.model.BookingStatus;
import com.homegenie.marketplaceservice.model.PaymentStatus;
import com.homegenie.marketplaceservice.model.ServiceCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Booking response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponseDTO {
    private Long id;
    private Long userId;
    private Long serviceId;
    private Long providerId;
    
    private String title;
    private String description;
    private ServiceCategory category;
    
    private LocalDateTime scheduledAt;
    private Integer estimatedDurationMinutes;
    
    private String serviceAddress;
    private BigDecimal serviceLocationLat;
    private BigDecimal serviceLocationLng;
    
    private BigDecimal quotedPrice;
    private BigDecimal finalPrice;
    private String currency;
    
    private Long paymentId;
    private PaymentStatus paymentStatus;
    private BookingStatus status;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    
    // Nested DTOs (optional)
    private ProviderSummaryDTO provider;
    private ServiceSummaryDTO service;
}
