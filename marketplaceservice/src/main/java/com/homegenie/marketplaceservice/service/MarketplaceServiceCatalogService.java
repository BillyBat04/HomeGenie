package com.homegenie.marketplaceservice.service;

import com.homegenie.marketplaceservice.dto.CreateServiceRequest;
import com.homegenie.marketplaceservice.dto.ServiceSummaryDTO;
import com.homegenie.marketplaceservice.model.MarketplaceProvider;
import com.homegenie.marketplaceservice.model.MarketplaceServiceEntity;
import com.homegenie.marketplaceservice.model.PriceUnit;
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


@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class MarketplaceServiceCatalogService {
    
    private final MarketplaceServiceRepository serviceRepository;
    private final MarketplaceProviderRepository providerRepository;
    
    
    public List<ServiceSummaryDTO> getAllActiveServices() {
        return serviceRepository.findAll().stream()
                .filter(MarketplaceServiceEntity::isAvailable)
                .map(this::mapToSummaryDTO)
                .collect(Collectors.toList());
    }
    
    
    public List<ServiceSummaryDTO> getServicesByCategory(ServiceCategory category) {
        return serviceRepository.findByCategoryAndStatus(category, ServiceStatus.ACTIVE).stream()
                .map(this::mapToSummaryDTO)
                .collect(Collectors.toList());
    }
    
    
    public List<ServiceSummaryDTO> getServicesByProvider(Long providerId) {
        return serviceRepository.findByProviderIdAndStatus(providerId, ServiceStatus.ACTIVE).stream()
                .map(this::mapToSummaryDTO)
                .collect(Collectors.toList());
    }
    
    
    public ServiceSummaryDTO getServiceById(Long serviceId) {
        MarketplaceServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId));
        return mapToSummaryDTO(service);
    }
    
    
    public List<ServiceSummaryDTO> getRecommendedServicesByCategory(ServiceCategory category) {
        log.info("Getting recommended services for category: {}", category);
        
        return serviceRepository.findRecommendedServicesByCategory(category).stream()
                .limit(5) 
                .map(this::mapToSummaryDTO)
                .collect(Collectors.toList());
    }
    
    
    @Transactional
    public ServiceSummaryDTO createService(CreateServiceRequest request) {
        log.info("Creating new service: name={}, providerId={}", request.getName(), request.getProviderId());

        MarketplaceProvider provider = providerRepository.findById(request.getProviderId())
                .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + request.getProviderId()));

        if (!provider.canAcceptBookings()) {
            throw new IllegalStateException("Provider cannot offer services: " + provider.getStatus());
        }

        MarketplaceServiceEntity service = MarketplaceServiceEntity.builder()
                .name(request.getName()).description(request.getDescription())
                .category(request.getCategory()).providerId(request.getProviderId())
                .basePrice(request.getBasePrice())
                .priceUnit(request.getPriceUnit() != null ? request.getPriceUnit() : PriceUnit.PER_JOB)
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .isFeatured(request.getIsFeatured() != null ? request.getIsFeatured() : false)
                .imageUrl(request.getImageUrl())
                .durationMinutes(request.getDurationMinutes())
                .build();

        MarketplaceServiceEntity saved = serviceRepository.save(service);
        log.info("Service created successfully: id={}", saved.getId());

        return mapToSummaryDTO(saved);
    }
    
    
    
    
    
    private ServiceSummaryDTO mapToSummaryDTO(MarketplaceServiceEntity service) {
        
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
