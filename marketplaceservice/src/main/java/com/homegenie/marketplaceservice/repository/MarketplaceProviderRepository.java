package com.homegenie.marketplaceservice.repository;

import com.homegenie.marketplaceservice.model.MarketplaceProvider;
import com.homegenie.marketplaceservice.model.ProviderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for MarketplaceProvider entity
 */
@Repository
public interface MarketplaceProviderRepository extends JpaRepository<MarketplaceProvider, Long> {
    
    /**
     * Find provider by email
     */
    Optional<MarketplaceProvider> findByEmail(String email);
    
    /**
     * Find all providers by status
     */
    List<MarketplaceProvider> findByStatus(ProviderStatus status);
    
    /**
     * Find all active and verified providers
     */
    List<MarketplaceProvider> findByStatusAndIsVerified(ProviderStatus status, Boolean isVerified);
    
    /**
     * Find top-rated providers (for featured display)
     */
    @Query("SELECT p FROM MarketplaceProvider p WHERE p.status = 'ACTIVE' AND p.isVerified = true " +
           "ORDER BY p.averageRating DESC, p.totalReviews DESC")
    List<MarketplaceProvider> findTopRatedProviders();
    
    /**
     * Find providers with minimum rating
     */
    @Query("SELECT p FROM MarketplaceProvider p WHERE p.status = 'ACTIVE' AND p.isVerified = true " +
           "AND p.averageRating >= :minRating ORDER BY p.averageRating DESC")
    List<MarketplaceProvider> findByMinimumRating(@Param("minRating") Double minRating);
    
    /**
     * Check if provider email already exists
     */
    boolean existsByEmail(String email);
}
