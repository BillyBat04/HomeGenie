package com.homegenie.marketplaceservice.controller;

import com.homegenie.marketplaceservice.dto.CreateServiceRequest;
import com.homegenie.marketplaceservice.dto.ServiceSummaryDTO;
import com.homegenie.marketplaceservice.model.ServiceCategory;
import com.homegenie.marketplaceservice.service.MarketplaceServiceCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/marketplace")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class MarketplaceServiceController {

    private final MarketplaceServiceCatalogService serviceCatalogService;

    @GetMapping("/services")
    public ResponseEntity<List<ServiceSummaryDTO>> getAllServices(
            @RequestParam(required = false) ServiceCategory category,
            @RequestParam(required = false) Long providerId) {
        log.info("GET /api/marketplace/services - category={}, providerId={}", category, providerId);
        List<ServiceSummaryDTO> services;
        if (category != null)        services = serviceCatalogService.getServicesByCategory(category);
        else if (providerId != null) services = serviceCatalogService.getServicesByProvider(providerId);
        else                         services = serviceCatalogService.getAllActiveServices();
        return ResponseEntity.ok(services);
    }

    @GetMapping("/services/{id}")
    public ResponseEntity<ServiceSummaryDTO> getServiceById(@PathVariable Long id) {
        return ResponseEntity.ok(serviceCatalogService.getServiceById(id));
    }

    /** Add a new service to the catalog. Provider must be ACTIVE and verified. ADMIN only. */
    @PostMapping("/services")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceSummaryDTO> createService(@Valid @RequestBody CreateServiceRequest request) {
        log.info("POST /api/marketplace/services - category={}, providerId={}", request.getCategory(), request.getProviderId());
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceCatalogService.createService(request));
    }

    /** Cross-domain endpoint — called by Maintenance Service to find external providers. */
    @GetMapping("/recommendations")
    public ResponseEntity<List<ServiceSummaryDTO>> getRecommendations(@RequestParam ServiceCategory category) {
        log.info("GET /api/marketplace/recommendations - category={} (CROSS-DOMAIN CALL)", category);
        List<ServiceSummaryDTO> recommendations = serviceCatalogService.getRecommendedServicesByCategory(category);
        log.info("Returning {} recommendations for category={}", recommendations.size(), category);
        return ResponseEntity.ok(recommendations);
    }
}

