package com.homegenie.marketplaceservice.repository;

import com.homegenie.marketplaceservice.model.MarketplaceProvider;
import com.homegenie.marketplaceservice.model.ProviderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface MarketplaceProviderRepository extends JpaRepository<MarketplaceProvider, Long> {
    
    
    Optional<MarketplaceProvider> findByEmail(String email);
    
    
    List<MarketplaceProvider> findByStatus(ProviderStatus status);
    
    
    List<MarketplaceProvider> findByStatusAndIsVerified(ProviderStatus status, Boolean isVerified);
    
    
    @Query("SELECT p FROM MarketplaceProvider p WHERE p.status = 'ACTIVE' AND p.isVerified = true " +
           "ORDER BY p.averageRating DESC, p.totalReviews DESC")
    List<MarketplaceProvider> findTopRatedProviders();
    
    
    @Query("SELECT p FROM MarketplaceProvider p WHERE p.status = 'ACTIVE' AND p.isVerified = true " +
           "AND p.averageRating >= :minRating ORDER BY p.averageRating DESC")
    List<MarketplaceProvider> findByMinimumRating(@Param("minRating") Double minRating);
    
    
    boolean existsByEmail(String email);
}
