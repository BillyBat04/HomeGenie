package com.homegenie.marketplaceservice.event;

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
public class BookingEvent {
    private String eventType; 
    private Long bookingId;
    private Long userId;
    private Long providerId;
    private Long serviceId;
    private String category;
    private String status;
    private String paymentStatus;
    private Long paymentId;
    private BigDecimal amount;
    private String currency;
    private LocalDateTime scheduledAt;
    private LocalDateTime eventTimestamp;
    private String miniAppId;
}
