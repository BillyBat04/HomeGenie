package com.homegenie.marketplaceservice.service;

import com.homegenie.marketplaceservice.dto.CreateReviewRequest;
import com.homegenie.marketplaceservice.dto.ReviewResponseDTO;
import com.homegenie.marketplaceservice.event.ReviewEvent;
import com.homegenie.marketplaceservice.exception.DuplicateReviewException;
import com.homegenie.marketplaceservice.model.MarketplaceBooking;
import com.homegenie.marketplaceservice.model.MarketplaceProvider;
import com.homegenie.marketplaceservice.model.MarketplaceReview;
import com.homegenie.marketplaceservice.repository.MarketplaceBookingRepository;
import com.homegenie.marketplaceservice.repository.MarketplaceProviderRepository;
import com.homegenie.marketplaceservice.repository.MarketplaceReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Marketplace Review Service
 * 
 * Manages customer reviews for service providers.
 * Updates provider average rating automatically.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class MarketplaceReviewService {
    
    private final MarketplaceReviewRepository reviewRepository;
    private final MarketplaceBookingRepository bookingRepository;
    private final MarketplaceProviderRepository providerRepository;
    private final KafkaTemplate<String, ReviewEvent> kafkaTemplate;
    
    @Value("${marketplace.mini-app-id}")
    private String miniAppId;
    
    /**
     * Create review (customer action after booking completion)
     * 
     * Automatically updates provider average rating.
     */
    @Transactional
    public ReviewResponseDTO createReview(CreateReviewRequest request) {
        try {
            log.info("Creating review for bookingId={}, providerId={}, rating={}",
                    request.getBookingId(), request.getProviderId(), request.getRating());
            
            // 1. Validate booking exists and is completed
            MarketplaceBooking booking = bookingRepository.findById(request.getBookingId())
                    .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + request.getBookingId()));
            
            if (!booking.canRate()) {
                throw new IllegalStateException("Booking must be completed before rating: " + booking.getStatus());
            }
            
            // 2. Check if review already exists
            if (reviewRepository.existsByBookingId(request.getBookingId())) {
                throw new DuplicateReviewException(
                    "Review already exists for booking ID: " + request.getBookingId()
                );
            }
            
            // 3. Create review entity
            MarketplaceReview review = MarketplaceReview.builder()
                    .bookingId(request.getBookingId())
                    .userId(request.getUserId())
                    .providerId(request.getProviderId())
                    .rating(request.getRating())
                    .title(request.getTitle())
                    .comment(request.getComment())
                    .photoUrlsJson(request.getPhotoUrlsJson())
                    .build();
            
            MarketplaceReview saved = reviewRepository.save(review);
            
            // 4. Update provider average rating
            MarketplaceProvider provider = providerRepository.findById(request.getProviderId())
                    .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + request.getProviderId()));
            
            provider.updateRating(BigDecimal.valueOf(request.getRating()));
            providerRepository.save(provider);
            
            log.info("Review created successfully: id={}, provider new rating={}", 
                    saved.getId(), provider.getAverageRating());
            
            // Publish ReviewSubmittedEvent to Kafka
            publishReviewEvent("SUBMITTED", saved);
            
            return mapToResponseDTO(saved);
            
        } catch (DataIntegrityViolationException e) {
            // Catch database constraint violation (unique constraint on booking_id)
            log.warn("DataIntegrityViolationException caught: {}", e.getMessage());
            throw new DuplicateReviewException(
                "Review already exists for booking ID: " + request.getBookingId(), e
            );
        }
    }
    
    /**
     * Get reviews for provider
     */
    public List<ReviewResponseDTO> getReviewsForProvider(Long providerId) {
        return reviewRepository.findByProviderIdAndIsVisible(providerId, true).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get review by booking ID
     */
    public ReviewResponseDTO getReviewByBookingId(Long bookingId) {
        MarketplaceReview review = reviewRepository.findByBookingId(bookingId)
                .orElse(null);
        
        return review != null ? mapToResponseDTO(review) : null;
    }
    
    /**
     * Get reviews by user
     */
    public List<ReviewResponseDTO> getReviewsByUser(Long userId) {
        return reviewRepository.findByUserId(userId).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }
    
    // ============================================================
    // Private Helper Methods
    // ============================================================
    
    /**
     * Publish review event to Kafka
     */
    private void publishReviewEvent(String eventType, MarketplaceReview review) {
        try {
            ReviewEvent event = ReviewEvent.builder()
                    .eventType(eventType)
                    .reviewId(review.getId())
                    .bookingId(review.getBookingId())
                    .providerId(review.getProviderId())
                    .userId(review.getUserId())
                    .rating(review.getRating())
                    .comment(review.getComment())
                    .eventTimestamp(LocalDateTime.now())
                    .miniAppId(miniAppId)
                    .build();
            
            kafkaTemplate.send("marketplace.review.events", event);
            log.info("Published {} event for review {}", eventType, review.getId());
        } catch (Exception e) {
            log.error("Failed to publish review event: {}", e.getMessage(), e);
        }
    }
    
    private ReviewResponseDTO mapToResponseDTO(MarketplaceReview review) {
        return ReviewResponseDTO.builder()
                .id(review.getId())
                .bookingId(review.getBookingId())
                .userId(review.getUserId())
                .providerId(review.getProviderId())
                .rating(review.getRating())
                .title(review.getTitle())
                .comment(review.getComment())
                .photoUrlsJson(review.getPhotoUrlsJson())
                .isVerified(review.getIsVerified())
                .isVisible(review.getIsVisible())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
