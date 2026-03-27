package com.homegenie.paymentservice.service;

import com.homegenie.paymentservice.dto.MaintenanceRequestDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Service to communicate with Maintenance Service
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MaintenanceServiceClient {

    private final RestTemplate restTemplate;

    @Value("${maintenance.service.url:http://localhost:8082}")
    private String maintenanceServiceUrl;

    /**
     * Get maintenance request by ID from Maintenance Service
     * 
     * @param requestId The maintenance request ID
     * @return MaintenanceRequestDto details
     * @throws RuntimeException if request not found or service unavailable
     */
    @CircuitBreaker(name = "maintenanceService", fallbackMethod = "getMaintenanceRequestFallback")
    public MaintenanceRequestDto getMaintenanceRequest(Long requestId) {
        try {
            String url = maintenanceServiceUrl + "/api/maintenance/" + requestId;
            log.info("Fetching maintenance request from: {}", url);
            
            MaintenanceRequestDto request = restTemplate.getForObject(url, MaintenanceRequestDto.class);
            
            if (request == null) {
                throw new RuntimeException("Maintenance request not found: " + requestId);
            }
            
            log.info("Retrieved maintenance request {}: status={}, assignedTo={}", 
                requestId, request.getStatus(), request.getAssignedTo());
            
            return request;
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Maintenance request not found: {}", requestId);
            throw new RuntimeException("Maintenance request not found: " + requestId);
        } catch (Exception e) {
            log.error("Failed to fetch maintenance request {}: {}", requestId, e.getMessage());
            throw new RuntimeException("Failed to communicate with Maintenance Service: " + e.getMessage());
        }
    }

    /**
     * Validate if payment is allowed for this maintenance request
     * 
     * @param requestId The maintenance request ID
     * @throws IllegalStateException if payment is not allowed
     */
    public void validatePaymentAllowed(Long requestId) {
        MaintenanceRequestDto request = getMaintenanceRequest(requestId);
        
        if (!request.canAcceptPayment()) {
            String reason = buildValidationErrorMessage(request);
            log.warn("Payment not allowed for request {}: {}", requestId, reason);
            throw new IllegalStateException(reason);
        }
        
        log.info("Payment validation passed for request {}", requestId);
    }

    /**
     * Build detailed error message for payment validation failure
     */
    private String buildValidationErrorMessage(MaintenanceRequestDto request) {
        if (request.isPending()) {
            return "Cannot create payment for PENDING maintenance request. " +
                   "Request must be assigned to a technician first.";
        }
        
        if (!request.hasAssignedTechnician()) {
            return "Cannot create payment for unassigned maintenance request. " +
                   "Request must have an assigned technician.";
        }
        
        if ("REJECTED".equalsIgnoreCase(request.getStatus())) {
            return "Cannot create payment for REJECTED maintenance request.";
        }
        
        return "Payment not allowed for maintenance request in status: " + request.getStatus();
    }

    /**
     * Fallback method when Maintenance Service circuit breaker opens.
     * Propagates the error — payment cannot proceed without maintenance data.
     */
    @SuppressWarnings("unused") // invoked by Resilience4j AOP
    private MaintenanceRequestDto getMaintenanceRequestFallback(Long requestId, Exception ex) {
        log.error("Circuit breaker OPEN for Maintenance Service. Request ID: {}, cause: {}", requestId, ex.getMessage());
        throw new RuntimeException("Maintenance Service is currently unavailable. Please try again later.");
    }
}
