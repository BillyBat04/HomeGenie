package com.homegenie.marketplaceservice.controller;

import com.homegenie.marketplaceservice.dto.ServiceSummaryDTO;
import com.homegenie.marketplaceservice.model.ServiceCategory;
import com.homegenie.marketplaceservice.service.MarketplaceServiceCatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Marketplace Service Catalog Controller
 * 
 * REST API for browsing service catalog.
 * Provides cross-domain recommendation endpoint for Maintenance Service.
 * 
 * Key endpoints:
 * - GET /api/marketplace/services - List all services
 * - GET /api/marketplace/services/{id} - Get service details
 * - GET /api/marketplace/recommendations - Cross-domain recommendations
 */
@RestController
@RequestMapping("/api/marketplace")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class MarketplaceServiceController {
    
    private final MarketplaceServiceCatalogService serviceCatalogService;
    
    /**
     * Get all active services
     */
    @GetMapping("/services")
    public ResponseEntity<List<ServiceSummaryDTO>> getAllServices(
            @RequestParam(required = false) ServiceCategory category,
            @RequestParam(required = false) Long providerId) {
        log.info("GET /api/marketplace/services - category={}, providerId={}", category, providerId);
        
        List<ServiceSummaryDTO> services;
        
        if (category != null) {
            services = serviceCatalogService.getServicesByCategory(category);
        } else if (providerId != null) {
            services = serviceCatalogService.getServicesByProvider(providerId);
        } else {
            services = serviceCatalogService.getAllActiveServices();
        }
        
        return ResponseEntity.ok(services);
    }
    
    /**
     * Get service by ID
     */
    @GetMapping("/services/{id}")
    public ResponseEntity<ServiceSummaryDTO> getServiceById(@PathVariable Long id) {
        log.info("GET /api/marketplace/services/{}", id);
        
        ServiceSummaryDTO service = serviceCatalogService.getServiceById(id);
        
        return ResponseEntity.ok(service);
    }
    
    /**
     * Get recommended services by category
     * 
     * ✅ CROSS-DOMAIN ENDPOINT
     * 
     * Called by Maintenance Service when internal team not available.
     * Returns top-rated services from verified providers.
     * 
     * Example call from MaintenanceService:
     * GET /api/marketplace/recommendations?category=PLUMBING
     */
    @GetMapping("/recommendations")
    public ResponseEntity<List<ServiceSummaryDTO>> getRecommendations(
            @RequestParam ServiceCategory category) {
        log.info("GET /api/marketplace/recommendations - category={} (CROSS-DOMAIN CALL)", category);
        
        List<ServiceSummaryDTO> recommendations = serviceCatalogService.getRecommendedServicesByCategory(category);
        
        log.info("Returning {} recommendations for category={}", recommendations.size(), category);
        
        return ResponseEntity.ok(recommendations);
    }
}
