package com.homegenie.marketplaceservice.repository;

import com.homegenie.marketplaceservice.model.BookingStatus;
import com.homegenie.marketplaceservice.model.MarketplaceBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for MarketplaceBooking entity
 */
@Repository
public interface MarketplaceBookingRepository extends JpaRepository<MarketplaceBooking, Long> {
    
    /**
     * Find all bookings by user
     */
    List<MarketplaceBooking> findByUserId(Long userId);
    
    /**
     * Find all bookings by user and status
     */
    List<MarketplaceBooking> findByUserIdAndStatus(Long userId, BookingStatus status);
    
    /**
     * Find all bookings by provider
     */
    List<MarketplaceBooking> findByProviderId(Long providerId);
    
    /**
     * Find all bookings by provider and status
     */
    List<MarketplaceBooking> findByProviderIdAndStatus(Long providerId, BookingStatus status);
    
    /**
     * Find all bookings by service
     */
    List<MarketplaceBooking> findByServiceId(Long serviceId);
    
    /**
     * Find upcoming bookings for provider (scheduled in future, confirmed status)
     */
    @Query("SELECT b FROM MarketplaceBooking b WHERE b.providerId = :providerId " +
           "AND b.status = 'CONFIRMED' AND b.scheduledAt > :now ORDER BY b.scheduledAt ASC")
    List<MarketplaceBooking> findUpcomingBookingsForProvider(@Param("providerId") Long providerId, 
                                                              @Param("now") LocalDateTime now);
    
    /**
     * Find active bookings for provider (confirmed or in progress)
     */
    @Query("SELECT b FROM MarketplaceBooking b WHERE b.providerId = :providerId " +
           "AND b.status IN ('CONFIRMED', 'IN_PROGRESS')")
    List<MarketplaceBooking> findActiveBookingsForProvider(@Param("providerId") Long providerId);
    
    /**
     * Find booking history for user (ordered by created date)
     */
    @Query("SELECT b FROM MarketplaceBooking b WHERE b.userId = :userId " +
           "ORDER BY b.createdAt DESC")
    List<MarketplaceBooking> findBookingHistoryForUser(@Param("userId") Long userId);
    
    /**
     * Count total bookings for provider
     */
    long countByProviderId(Long providerId);
    
    /**
     * Count completed bookings for provider
     */
    long countByProviderIdAndStatus(Long providerId, BookingStatus status);
}
