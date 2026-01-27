package com.homegenie.marketplaceservice.controller;

import com.homegenie.marketplaceservice.dto.CreateReviewRequest;
import com.homegenie.marketplaceservice.dto.ReviewResponseDTO;
import com.homegenie.marketplaceservice.service.MarketplaceReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Marketplace Review Controller
 * 
 * REST API for customer reviews.
 * 
 * Key endpoints:
 * - POST /api/marketplace/reviews - Submit review
 * - GET /api/marketplace/reviews/provider/{id} - Get provider reviews
 * - GET /api/marketplace/reviews/booking/{id} - Get review by booking
 */
@RestController
@RequestMapping("/api/marketplace/reviews")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class MarketplaceReviewController {
    
    private final MarketplaceReviewService reviewService;
    
    /**
     * Create review (customer action)
     * 
     * POST /api/marketplace/reviews
     * 
     * Requires completed booking.
     * Automatically updates provider average rating.
     */
    @PostMapping
    public ResponseEntity<ReviewResponseDTO> createReview(@Valid @RequestBody CreateReviewRequest request) {
        log.info("POST /api/marketplace/reviews - bookingId={}, rating={}", request.getBookingId(), request.getRating());
        
        ReviewResponseDTO response = reviewService.createReview(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Get reviews for provider
     */
    @GetMapping("/provider/{providerId}")
    public ResponseEntity<List<ReviewResponseDTO>> getReviewsForProvider(@PathVariable Long providerId) {
        log.info("GET /api/marketplace/reviews/provider/{}", providerId);
        
        List<ReviewResponseDTO> reviews = reviewService.getReviewsForProvider(providerId);
        
        return ResponseEntity.ok(reviews);
    }
    
    /**
     * Get review by booking ID
     */
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<ReviewResponseDTO> getReviewByBookingId(@PathVariable Long bookingId) {
        log.info("GET /api/marketplace/reviews/booking/{}", bookingId);
        
        ReviewResponseDTO review = reviewService.getReviewByBookingId(bookingId);
        
        if (review == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(review);
    }
    
    /**
     * Get reviews by user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ReviewResponseDTO>> getReviewsByUser(@PathVariable Long userId) {
        log.info("GET /api/marketplace/reviews/user/{}", userId);
        
        List<ReviewResponseDTO> reviews = reviewService.getReviewsByUser(userId);
        
        return ResponseEntity.ok(reviews);
    }
}
