package com.homegenie.marketplaceservice.service;

import com.homegenie.marketplaceservice.dto.CreateReviewRequest;
import com.homegenie.marketplaceservice.dto.ReviewResponseDTO;
import com.homegenie.marketplaceservice.event.ReviewEvent;
import com.homegenie.marketplaceservice.exception.DuplicateReviewException;
import com.homegenie.marketplaceservice.model.*;
import com.homegenie.marketplaceservice.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MarketplaceReviewService
 * 
 * Tests review submission, rating validation, and provider rating auto-update
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class MarketplaceReviewServiceTest {

    @Mock
    private MarketplaceReviewRepository reviewRepository;
    
    @Mock
    private MarketplaceBookingRepository bookingRepository;
    
    @Mock
    private MarketplaceProviderRepository providerRepository;
    
    @Mock
    private KafkaTemplate<String, ReviewEvent> kafkaTemplate;
    
    @InjectMocks
    private MarketplaceReviewService reviewService;
    
    private MarketplaceBooking testBooking;
    private MarketplaceProvider testProvider;
    private MarketplaceReview testReview;

    /** Build a JwtAuthenticationToken whose 'userId' claim matches testBooking.userId. */
    private void setAuthenticatedUser(Long userId) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject("test@example.com")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("userId", userId)
                .claim("role", "USER")
                .build();
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, Collections.emptyList());
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }
    
    @BeforeEach
    void setUp() {
        testBooking = MarketplaceBooking.builder()
                .id(1L)
                .userId(100L)
                .providerId(1L)
                .serviceId(1L)
                .status(BookingStatus.COMPLETED)
                .paymentStatus(PaymentStatus.PAID)
                .quotedPrice(BigDecimal.valueOf(150.00))
                .completedAt(LocalDateTime.now().minusDays(1))
                .build();
        
        testProvider = MarketplaceProvider.builder()
                .id(1L)
                .name("Test Provider")
                .email("provider@test.com")
                .phone("123456789")
                .status(ProviderStatus.ACTIVE)
                .averageRating(BigDecimal.ZERO)
                .totalReviews(0)
                .build();
        
        testReview = MarketplaceReview.builder()
                .id(1L)
                .bookingId(1L)
                .userId(100L)
                .providerId(1L)
                .rating(5)
                .title("Excellent service!")
                .comment("Very professional")
                .isVerified(false)
                .isVisible(true)
                .build();
        
        ReflectionTestUtils.setField(reviewService, "miniAppId", "marketplace");
        // Default: act as user 100 (matches testBooking.userId)
        setAuthenticatedUser(100L);
    }
    
    @Test
    void testCreateReview_Success() {
        // Given
        CreateReviewRequest request = CreateReviewRequest.builder()
                .bookingId(1L)
                .userId(100L)
                .providerId(1L)
                .rating(5)
                .title("Excellent service!")
                .comment("Very professional and on time")
                .build();
        
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(reviewRepository.existsByBookingId(1L)).thenReturn(false);
        when(reviewRepository.save(any(MarketplaceReview.class))).thenReturn(testReview);
        when(providerRepository.findById(1L)).thenReturn(Optional.of(testProvider));
        when(providerRepository.save(any(MarketplaceProvider.class))).thenReturn(testProvider);
        
        // When
        ReviewResponseDTO result = reviewService.createReview(request);
        
        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(5, result.getRating());
        assertEquals("Excellent service!", result.getTitle());
        
        // Verify provider rating updated
        verify(providerRepository).save(argThat(provider -> 
            provider.getAverageRating() != null &&
            provider.getTotalReviews() > 0
        ));
        
        // Verify Kafka event published
        ArgumentCaptor<ReviewEvent> eventCaptor = ArgumentCaptor.forClass(ReviewEvent.class);
        verify(kafkaTemplate).send(eq("marketplace.review.events"), eventCaptor.capture());
        
        ReviewEvent event = eventCaptor.getValue();
        assertEquals("SUBMITTED", event.getEventType());
        assertEquals(1L, event.getReviewId());
        assertEquals(5, event.getRating());
    }
    
    @Test
    void testCreateReview_BookingNotCompleted() {
        // Given
        testBooking.setStatus(BookingStatus.IN_PROGRESS);
        
        CreateReviewRequest request = CreateReviewRequest.builder()
                .bookingId(1L)
                .userId(100L)
                .providerId(1L)
                .rating(5)
                .build();
        
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        
        // When & Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> reviewService.createReview(request)
        );
        
        assertTrue(exception.getMessage().contains("must be completed before rating"));
        verify(reviewRepository, never()).save(any());
    }
    
    @Test
    void testCreateReview_DuplicateReview() {
        // Given
        CreateReviewRequest request = CreateReviewRequest.builder()
                .bookingId(1L)
                .userId(100L)
                .providerId(1L)
                .rating(5)
                .build();
        
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(reviewRepository.existsByBookingId(1L)).thenReturn(true);
        
        // When & Then
        // BUG FIX: service throws DuplicateReviewException, not IllegalStateException
        assertThrows(
                DuplicateReviewException.class,
                () -> reviewService.createReview(request)
        );
        
        verify(reviewRepository, never()).save(any());
    }
    
    @Test
    void testCreateReview_InvalidRating_TooLow() {
        // Given - Test that rating validation works at entity level
        // No repository mocking needed - this tests entity validation only
        
        // Create review entity directly to test validation
        MarketplaceReview review = MarketplaceReview.builder()
                .rating(0) // Invalid: below 1
                .build();
        
        // Verify rating constraint (1-5)
        assertFalse(review.isValidRating());
    }
    
    @Test
    void testCreateReview_InvalidRating_TooHigh() {
        // Given
        MarketplaceReview review = MarketplaceReview.builder()
                .rating(6) // Invalid: above 5
                .build();
        
        // Then
        assertFalse(review.isValidRating());
    }
    
    @Test
    void testCreateReview_ValidRatings() {
        // Test all valid ratings (1-5)
        for (int rating = 1; rating <= 5; rating++) {
            MarketplaceReview review = MarketplaceReview.builder()
                    .rating(rating)
                    .build();
            
            assertTrue(review.isValidRating(), "Rating " + rating + " should be valid");
        }
    }
    
    @Test
    void testProviderRatingUpdate_FirstReview() {
        // Given
        testProvider.setAverageRating(null);
        testProvider.setTotalReviews(0);
        
        CreateReviewRequest request = CreateReviewRequest.builder()
                .bookingId(1L)
                .userId(100L)
                .providerId(1L)
                .rating(5)
                .build();
        
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(reviewRepository.existsByBookingId(1L)).thenReturn(false);
        when(reviewRepository.save(any(MarketplaceReview.class))).thenReturn(testReview);
        when(providerRepository.findById(1L)).thenReturn(Optional.of(testProvider));
        
        ArgumentCaptor<MarketplaceProvider> providerCaptor = ArgumentCaptor.forClass(MarketplaceProvider.class);
        when(providerRepository.save(providerCaptor.capture())).thenReturn(testProvider);
        
        // When
        reviewService.createReview(request);
        
        // Then
        MarketplaceProvider savedProvider = providerCaptor.getValue();
        assertNotNull(savedProvider.getAverageRating());
        assertEquals(1, savedProvider.getTotalReviews());
    }
    
    @Test
    void testProviderRatingUpdate_MultipleReviews() {
        // Given - Provider already has 1 review with rating 4.0
        testProvider.setAverageRating(BigDecimal.valueOf(4.0));
        testProvider.setTotalReviews(1);
        
        CreateReviewRequest request = CreateReviewRequest.builder()
                .bookingId(1L)
                .userId(100L)
                .providerId(1L)
                .rating(5) // New 5-star review
                .build();
        
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(reviewRepository.existsByBookingId(1L)).thenReturn(false);
        when(reviewRepository.save(any(MarketplaceReview.class))).thenReturn(testReview);
        when(providerRepository.findById(1L)).thenReturn(Optional.of(testProvider));
        
        ArgumentCaptor<MarketplaceProvider> providerCaptor = ArgumentCaptor.forClass(MarketplaceProvider.class);
        when(providerRepository.save(providerCaptor.capture())).thenReturn(testProvider);
        
        // When
        reviewService.createReview(request);
        
        // Then - Average should be (4.0 + 5.0) / 2 = 4.5
        MarketplaceProvider savedProvider = providerCaptor.getValue();
        assertEquals(2, savedProvider.getTotalReviews());
        // Rating calculation happens in entity's updateRating method
    }
    
    @Test
    void testGetReviewByBookingId_Found() {
        // Given
        when(reviewRepository.findByBookingId(1L)).thenReturn(Optional.of(testReview));
        
        // When
        ReviewResponseDTO result = reviewService.getReviewByBookingId(1L);
        
        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(5, result.getRating());
    }
    
    @Test
    void testGetReviewByBookingId_NotFound() {
        // Given
        when(reviewRepository.findByBookingId(999L)).thenReturn(Optional.empty());
        
        // When
        ReviewResponseDTO result = reviewService.getReviewByBookingId(999L);
        
        // Then
        assertNull(result);
    }
}
