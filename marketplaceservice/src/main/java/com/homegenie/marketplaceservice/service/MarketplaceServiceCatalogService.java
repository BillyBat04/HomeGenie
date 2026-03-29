package com.homegenie.marketplaceservice.service;

import com.homegenie.marketplaceservice.dto.CreateServiceRequest;
import com.homegenie.marketplaceservice.dto.ServiceSummaryDTO;
import com.homegenie.marketplaceservice.model.ServiceCategory;

import java.util.List;

public interface MarketplaceServiceCatalogService {

    List<ServiceSummaryDTO> getAllActiveServices();

    List<ServiceSummaryDTO> getServicesByCategory(ServiceCategory category);

    List<ServiceSummaryDTO> getServicesByProvider(Long providerId);

    ServiceSummaryDTO getServiceById(Long serviceId);

    List<ServiceSummaryDTO> getRecommendedServicesByCategory(ServiceCategory category);

    ServiceSummaryDTO createService(CreateServiceRequest request);
}
