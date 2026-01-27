package com.homegenie.marketplaceservice.controller;

import com.homegenie.marketplaceservice.dto.ProviderSummaryDTO;
import com.homegenie.marketplaceservice.service.MarketplaceProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Marketplace Provider Controller
 * 
 * REST API for provider management.
 * 
 * Key endpoints:
 * - GET /api/marketplace/providers - List providers
 * - GET /api/marketplace/providers/{id} - Get provider details
 * - GET /api/marketplace/providers/top-rated - Top rated providers
 */
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/marketplace/providers")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@Tag(name = "Marketplace Providers", description = "Provider management APIs")
public class MarketplaceProviderController {
    
    private final MarketplaceProviderService providerService;
    
    /**
     * Get all active providers
     */
    @GetMapping
    public ResponseEntity<List<ProviderSummaryDTO>> getAllProviders() {
        log.info("GET /api/marketplace/providers");
        
        List<ProviderSummaryDTO> providers = providerService.getActiveProviders();
        
        return ResponseEntity.ok(providers);
    }
    
    /**
     * Get provider by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProviderSummaryDTO> getProviderById(@PathVariable Long id) {
        log.info("GET /api/marketplace/providers/{}", id);
        
        ProviderSummaryDTO provider = providerService.getProviderById(id);
        
        return ResponseEntity.ok(provider);
    }
    
    /**
     * Get top-rated providers
     */
    @GetMapping("/top-rated")
    public ResponseEntity<List<ProviderSummaryDTO>> getTopRatedProviders() {
        log.info("GET /api/marketplace/providers/top-rated");
        
        List<ProviderSummaryDTO> providers = providerService.getTopRatedProviders();
        
        return ResponseEntity.ok(providers);
    }
}
