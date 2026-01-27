package com.homegenie.marketplaceservice.repository;

import com.homegenie.marketplaceservice.model.MarketplaceReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for MarketplaceReview entity
 */
@Repository
public interface MarketplaceReviewRepository extends JpaRepository<MarketplaceReview, Long> {
    
    /**
     * Find review by booking ID
     */
    Optional<MarketplaceReview> findByBookingId(Long bookingId);
    
    /**
     * Check if review exists for booking
     */
    boolean existsByBookingId(Long bookingId);
    
    /**
     * Find all reviews by provider
     */
    List<MarketplaceReview> findByProviderId(Long providerId);
    
    /**
     * Find all visible reviews by provider
     */
    List<MarketplaceReview> findByProviderIdAndIsVisible(Long providerId, Boolean isVisible);
    
    /**
     * Find all reviews by user
     */
    List<MarketplaceReview> findByUserId(Long userId);
    
    /**
     * Find reviews by provider and minimum rating
     */
    @Query("SELECT r FROM MarketplaceReview r WHERE r.providerId = :providerId " +
           "AND r.isVisible = true AND r.rating >= :minRating ORDER BY r.createdAt DESC")
    List<MarketplaceReview> findByProviderIdAndMinimumRating(@Param("providerId") Long providerId, 
                                                               @Param("minRating") Integer minRating);
    
    /**
     * Find recent reviews for provider (for display)
     */
    @Query("SELECT r FROM MarketplaceReview r WHERE r.providerId = :providerId " +
           "AND r.isVisible = true ORDER BY r.createdAt DESC")
    List<MarketplaceReview> findRecentReviewsForProvider(@Param("providerId") Long providerId);
    
    /**
     * Count reviews by provider
     */
    long countByProviderId(Long providerId);
    
    /**
     * Calculate average rating for provider
     */
    @Query("SELECT AVG(r.rating) FROM MarketplaceReview r WHERE r.providerId = :providerId AND r.isVisible = true")
    Double calculateAverageRatingForProvider(@Param("providerId") Long providerId);
}
