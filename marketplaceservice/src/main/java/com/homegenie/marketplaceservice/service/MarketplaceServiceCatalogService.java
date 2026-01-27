package com.homegenie.marketplaceservice.service;

import com.homegenie.marketplaceservice.dto.ServiceSummaryDTO;
import com.homegenie.marketplaceservice.model.MarketplaceProvider;
import com.homegenie.marketplaceservice.model.MarketplaceServiceEntity;
import com.homegenie.marketplaceservice.model.ServiceCategory;
import com.homegenie.marketplaceservice.model.ServiceStatus;
import com.homegenie.marketplaceservice.repository.MarketplaceProviderRepository;
import com.homegenie.marketplaceservice.repository.MarketplaceServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Marketplace Service Catalog Service
 * 
 * Manages service catalog for providers.
 * Provides cross-domain recommendations to Maintenance Service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MarketplaceServiceCatalogService {
    
    private final MarketplaceServiceRepository serviceRepository;
    private final MarketplaceProviderRepository providerRepository;
    
    /**
     * Get all active services
     */
    public List<ServiceSummaryDTO> getAllActiveServices() {
        return serviceRepository.findAll().stream()
                .filter(MarketplaceServiceEntity::isAvailable)
                .map(this::mapToSummaryDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get services by category
     */
    public List<ServiceSummaryDTO> getServicesByCategory(ServiceCategory category) {
        return serviceRepository.findByCategoryAndStatus(category, ServiceStatus.ACTIVE).stream()
                .map(this::mapToSummaryDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get services by provider
     */
    public List<ServiceSummaryDTO> getServicesByProvider(Long providerId) {
        return serviceRepository.findByProviderIdAndStatus(providerId, ServiceStatus.ACTIVE).stream()
                .map(this::mapToSummaryDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get service by ID
     */
    public ServiceSummaryDTO getServiceById(Long serviceId) {
        MarketplaceServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId));
        return mapToSummaryDTO(service);
    }
    
    /**
     * Get recommended services by category (for cross-domain recommendations)
     * 
     * Called by Maintenance Service when internal team not available.
     * Returns services from top-rated, verified providers.
     */
    public List<ServiceSummaryDTO> getRecommendedServicesByCategory(ServiceCategory category) {
        log.info("Getting recommended services for category: {}", category);
        
        return serviceRepository.findRecommendedServicesByCategory(category).stream()
                .limit(5) // Top 5 recommendations
                .map(this::mapToSummaryDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Create new service (provider action)
     */
    @Transactional
    public ServiceSummaryDTO createService(MarketplaceServiceEntity service) {
        log.info("Creating new service: name={}, providerId={}", service.getName(), service.getProviderId());
        
        // Validate provider exists and is active
        MarketplaceProvider provider = providerRepository.findById(service.getProviderId())
                .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + service.getProviderId()));
        
        if (!provider.canAcceptBookings()) {
            throw new IllegalStateException("Provider cannot offer services: " + provider.getStatus());
        }
        
        MarketplaceServiceEntity saved = serviceRepository.save(service);
        log.info("Service created successfully: id={}", saved.getId());
        
        return mapToSummaryDTO(saved);
    }
    
    // ============================================================
    // Private Helper Methods
    // ============================================================
    
    private ServiceSummaryDTO mapToSummaryDTO(MarketplaceServiceEntity service) {
        // Get provider name
        String providerName = providerRepository.findById(service.getProviderId())
                .map(MarketplaceProvider::getName)
                .orElse("Unknown Provider");
        
        return ServiceSummaryDTO.builder()
                .id(service.getId())
                .name(service.getName())
                .description(service.getDescription())
                .category(service.getCategory())
                .providerId(service.getProviderId())
                .providerName(providerName)
                .basePrice(service.getBasePrice())
                .priceUnit(service.getPriceUnit())
                .currency(service.getCurrency())
                .status(service.getStatus())
                .isFeatured(service.getIsFeatured())
                .imageUrl(service.getImageUrl())
                .durationMinutes(service.getDurationMinutes())
                .build();
    }
}
