package com.homegenie.marketplaceservice.repository;

import com.homegenie.marketplaceservice.model.MarketplaceReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface MarketplaceReviewRepository extends JpaRepository<MarketplaceReview, Long> {
    
    
    Optional<MarketplaceReview> findByBookingId(Long bookingId);
    
    
    boolean existsByBookingId(Long bookingId);
    
    
    List<MarketplaceReview> findByProviderId(Long providerId);
    
    
    List<MarketplaceReview> findByProviderIdAndIsVisible(Long providerId, Boolean isVisible);
    
    
    List<MarketplaceReview> findByUserId(Long userId);
    
    
    @Query("SELECT r FROM MarketplaceReview r WHERE r.providerId = :providerId " +
           "AND r.isVisible = true AND r.rating >= :minRating ORDER BY r.createdAt DESC")
    List<MarketplaceReview> findByProviderIdAndMinimumRating(@Param("providerId") Long providerId, 
                                                               @Param("minRating") Integer minRating);
    
    
    @Query("SELECT r FROM MarketplaceReview r WHERE r.providerId = :providerId " +
           "AND r.isVisible = true ORDER BY r.createdAt DESC")
    List<MarketplaceReview> findRecentReviewsForProvider(@Param("providerId") Long providerId);
    
    
    long countByProviderId(Long providerId);
    
    
    @Query("SELECT AVG(r.rating) FROM MarketplaceReview r WHERE r.providerId = :providerId AND r.isVisible = true")
    Double calculateAverageRatingForProvider(@Param("providerId") Long providerId);
}
