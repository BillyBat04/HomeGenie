package com.homegenie.marketplaceservice.controller;

import com.homegenie.marketplaceservice.dto.CreateProviderRequest;
import com.homegenie.marketplaceservice.dto.ProviderSummaryDTO;
import com.homegenie.marketplaceservice.service.MarketplaceProviderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/marketplace/providers")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@Tag(name = "Marketplace Providers", description = "Provider management APIs")
public class MarketplaceProviderController {

    private final MarketplaceProviderService providerService;

    @GetMapping
    public ResponseEntity<List<ProviderSummaryDTO>> getAllProviders() {
        log.info("GET /api/marketplace/providers");
        return ResponseEntity.ok(providerService.getActiveProviders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProviderSummaryDTO> getProviderById(@PathVariable Long id) {
        return ResponseEntity.ok(providerService.getProviderById(id));
    }

    @GetMapping("/top-rated")
    public ResponseEntity<List<ProviderSummaryDTO>> getTopRatedProviders() {
        return ResponseEntity.ok(providerService.getTopRatedProviders());
    }

    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProviderSummaryDTO> createProvider(@Valid @RequestBody CreateProviderRequest request) {
        log.info("POST /api/marketplace/providers - Registering provider: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(providerService.createProvider(request));
    }

    
    @PatchMapping("/{id}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProviderSummaryDTO> verifyProvider(@PathVariable Long id) {
        log.info("PATCH /api/marketplace/providers/{}/verify", id);
        return ResponseEntity.ok(providerService.verifyProvider(id));
    }
}

