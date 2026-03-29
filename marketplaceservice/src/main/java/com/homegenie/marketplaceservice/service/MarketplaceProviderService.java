package com.homegenie.marketplaceservice.service;

import com.homegenie.marketplaceservice.dto.CreateProviderRequest;
import com.homegenie.marketplaceservice.dto.ProviderSummaryDTO;

import java.util.List;

public interface MarketplaceProviderService {

    List<ProviderSummaryDTO> getActiveProviders();

    ProviderSummaryDTO getProviderById(Long providerId);

    List<ProviderSummaryDTO> getTopRatedProviders();

    ProviderSummaryDTO createProvider(CreateProviderRequest request);

    ProviderSummaryDTO verifyProvider(Long providerId);
}
