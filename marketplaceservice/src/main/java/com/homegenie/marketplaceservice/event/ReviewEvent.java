package com.homegenie.marketplaceservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewEvent {
    private String eventType; 
    private Long reviewId;
    private Long bookingId;
    private Long providerId;
    private Long userId;
    private Integer rating;
    private String comment;
    private LocalDateTime eventTimestamp;
    private String miniAppId;
}
