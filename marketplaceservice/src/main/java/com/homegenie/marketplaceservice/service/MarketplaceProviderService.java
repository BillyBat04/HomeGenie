package com.homegenie.marketplaceservice.service;

import com.homegenie.marketplaceservice.dto.ProviderSummaryDTO;
import com.homegenie.marketplaceservice.model.MarketplaceProvider;
import com.homegenie.marketplaceservice.model.ProviderStatus;
import com.homegenie.marketplaceservice.repository.MarketplaceProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import java.time.LocalDateTime;

/**
 * Marketplace Provider Service
 * 
 * Manages external service providers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MarketplaceProviderService {
    
    private final MarketplaceProviderRepository providerRepository;
    
    /**
     * Get all active and verified providers
     */
    public List<ProviderSummaryDTO> getActiveProviders() {
        return providerRepository.findByStatusAndIsVerified(ProviderStatus.ACTIVE, true).stream()
                .map(this::mapToSummaryDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get provider by ID
     */
    public ProviderSummaryDTO getProviderById(Long providerId) {
        MarketplaceProvider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + providerId));
        return mapToSummaryDTO(provider);
    }
    
    /**
     * Get top-rated providers
     */
    public List<ProviderSummaryDTO> getTopRatedProviders() {
        return providerRepository.findTopRatedProviders().stream()
                .limit(10)
                .map(this::mapToSummaryDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Create new provider (admin only)
     */
    @Transactional
    public ProviderSummaryDTO createProvider(MarketplaceProvider provider) {
        log.info("Creating new provider: email={}", provider.getEmail());
        
        if (providerRepository.existsByEmail(provider.getEmail())) {
            throw new IllegalArgumentException("Provider email already exists: " + provider.getEmail());
        }
        
        MarketplaceProvider saved = providerRepository.save(provider);
        log.info("Provider created successfully: id={}", saved.getId());
        
        return mapToSummaryDTO(saved);
    }
    
    /**
     * Verify provider (admin action)
     */
    @Transactional
    public ProviderSummaryDTO verifyProvider(Long providerId) {
        log.info("Verifying provider: id={}", providerId);
        
        MarketplaceProvider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + providerId));
        
        provider.verify();
        MarketplaceProvider saved = providerRepository.save(provider);
        
        log.info("Provider verified successfully: id={}", saved.getId());
        
        return mapToSummaryDTO(saved);
    }
    
    // ============================================================
    // Private Helper Methods
    // ============================================================
    
    private ProviderSummaryDTO mapToSummaryDTO(MarketplaceProvider provider) {
        return ProviderSummaryDTO.builder()
                .id(provider.getId())
                .name(provider.getName())
                .companyName(provider.getCompanyName())
                .status(provider.getStatus())
                .isVerified(provider.getIsVerified())
                .averageRating(provider.getAverageRating())
                .totalReviews(provider.getTotalReviews())
                .completedBookings(provider.getCompletedBookings())
                .profilePhotoUrl(provider.getProfilePhotoUrl())
                .build();
    }
}
