package com.homegenie.marketplaceservice.repository;

import com.homegenie.marketplaceservice.model.BookingStatus;
import com.homegenie.marketplaceservice.model.MarketplaceBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface MarketplaceBookingRepository extends JpaRepository<MarketplaceBooking, Long> {
    
    
    List<MarketplaceBooking> findByUserId(Long userId);
    
    
    List<MarketplaceBooking> findByUserIdAndStatus(Long userId, BookingStatus status);
    
    
    List<MarketplaceBooking> findByProviderId(Long providerId);
    
    
    List<MarketplaceBooking> findByProviderIdAndStatus(Long providerId, BookingStatus status);
    
    
    List<MarketplaceBooking> findByServiceId(Long serviceId);
    
    
    @Query("SELECT b FROM MarketplaceBooking b WHERE b.providerId = :providerId " +
           "AND b.status = 'CONFIRMED' AND b.scheduledAt > :now ORDER BY b.scheduledAt ASC")
    List<MarketplaceBooking> findUpcomingBookingsForProvider(@Param("providerId") Long providerId, 
                                                              @Param("now") LocalDateTime now);
    
    
    @Query("SELECT b FROM MarketplaceBooking b WHERE b.providerId = :providerId " +
           "AND b.status IN ('CONFIRMED', 'IN_PROGRESS')")
    List<MarketplaceBooking> findActiveBookingsForProvider(@Param("providerId") Long providerId);
    
    
    @Query("SELECT b FROM MarketplaceBooking b WHERE b.userId = :userId " +
           "ORDER BY b.createdAt DESC")
    List<MarketplaceBooking> findBookingHistoryForUser(@Param("userId") Long userId);
    
    
    long countByProviderId(Long providerId);
    
    
    long countByProviderIdAndStatus(Long providerId, BookingStatus status);
}
