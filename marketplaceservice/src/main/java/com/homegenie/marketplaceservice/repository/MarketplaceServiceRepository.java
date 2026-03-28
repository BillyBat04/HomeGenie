package com.homegenie.marketplaceservice.repository;

import com.homegenie.marketplaceservice.model.MarketplaceServiceEntity;
import com.homegenie.marketplaceservice.model.ServiceCategory;
import com.homegenie.marketplaceservice.model.ServiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface MarketplaceServiceRepository extends JpaRepository<MarketplaceServiceEntity, Long> {
    
    
    List<MarketplaceServiceEntity> findByStatus(ServiceStatus status);
    List<MarketplaceServiceEntity> findByProviderId(Long providerId);
    
    
    List<MarketplaceServiceEntity> findByProviderIdAndStatus(Long providerId, ServiceStatus status);
    
    
    List<MarketplaceServiceEntity> findByCategory(ServiceCategory category);
    
    
    List<MarketplaceServiceEntity> findByCategoryAndStatus(ServiceCategory category, ServiceStatus status);
    
    
    List<MarketplaceServiceEntity> findByIsFeaturedAndStatus(Boolean isFeatured, ServiceStatus status);
    
    
    @Query("SELECT s FROM MarketplaceServiceEntity s " +
           "JOIN MarketplaceProvider p ON s.providerId = p.id " +
           "WHERE s.category = :category AND s.status = 'ACTIVE' " +
           "AND p.status = 'ACTIVE' AND p.isVerified = true " +
           "ORDER BY p.averageRating DESC, p.totalReviews DESC")
    List<MarketplaceServiceEntity> findRecommendedServicesByCategory(@Param("category") ServiceCategory category);
}
