package com.homegenie.marketplaceservice.service.impl;


import com.homegenie.marketplaceservice.service.MarketplaceProviderService;
import com.homegenie.marketplaceservice.dto.CreateProviderRequest;
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


@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class MarketplaceProviderServiceImpl implements MarketplaceProviderService {
    
    private final MarketplaceProviderRepository providerRepository;
    
    
    public List<ProviderSummaryDTO> getActiveProviders() {
        return providerRepository.findByStatusAndIsVerified(ProviderStatus.ACTIVE, true).stream()
                .map(this::mapToSummaryDTO)
                .collect(Collectors.toList());
    }
    
    
    public ProviderSummaryDTO getProviderById(Long providerId) {
        MarketplaceProvider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + providerId));
        return mapToSummaryDTO(provider);
    }
    
    
    public List<ProviderSummaryDTO> getTopRatedProviders() {
        return providerRepository.findTopRatedProviders().stream()
                .limit(10)
                .map(this::mapToSummaryDTO)
                .collect(Collectors.toList());
    }
    
    
    @Transactional
    public ProviderSummaryDTO createProvider(CreateProviderRequest request) {
        log.info("Creating new provider: email={}", request.getEmail());

        if (providerRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Provider email already exists: " + request.getEmail());
        }

        MarketplaceProvider provider = MarketplaceProvider.builder()
                .name(request.getName()).email(request.getEmail()).phone(request.getPhone())
                .companyName(request.getCompanyName()).licenseNumber(request.getLicenseNumber())
                .bio(request.getBio()).profilePhotoUrl(request.getProfilePhotoUrl())
                .build();

        MarketplaceProvider saved = providerRepository.save(provider);
        log.info("Provider created successfully: id={}", saved.getId());

        return mapToSummaryDTO(saved);
    }
    
    
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
