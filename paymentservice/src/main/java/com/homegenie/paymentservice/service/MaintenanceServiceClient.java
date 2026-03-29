package com.homegenie.paymentservice.service;

import com.homegenie.paymentservice.dto.MaintenanceRequestDto;
import com.homegenie.paymentservice.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@Slf4j
public class MaintenanceServiceClient {

    private final WebClient webClient;

    @Value("${maintenance.service.url:http://localhost:8082}")
    private String maintenanceServiceUrl;

    public MaintenanceServiceClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @CircuitBreaker(name = "maintenanceService", fallbackMethod = "getMaintenanceRequestFallback")
    public MaintenanceRequestDto getMaintenanceRequest(Long requestId) {
        String url = maintenanceServiceUrl + "/api/maintenance/" + requestId;
        log.info("Fetching maintenance request from: {}", url);

        MaintenanceRequestDto request = webClient.get()
                .uri(url)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        response.createException().map(ex ->
                                new IllegalArgumentException("Maintenance request not found: " + requestId)))
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        response.createException().map(ex ->
                                new ServiceUnavailableException("Maintenance Service")))
                .bodyToMono(MaintenanceRequestDto.class)
                .block(); // virtual threads make blocking safe here

        if (request == null) {
            throw new IllegalArgumentException("Maintenance request not found: " + requestId);
        }
        log.info("Retrieved maintenance request {}: status={}, assignedTo={}",
                requestId, request.getStatus(), request.getAssignedTo());
        return request;
    }

    public void validatePaymentAllowed(Long requestId) {
        MaintenanceRequestDto request = getMaintenanceRequest(requestId);
        if (!request.canAcceptPayment()) {
            String reason = buildValidationErrorMessage(request);
            log.warn("Payment not allowed for request {}: {}", requestId, reason);
            throw new IllegalStateException(reason);
        }
        log.info("Payment validation passed for request {}", requestId);
    }

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

    @SuppressWarnings("unused")
    private MaintenanceRequestDto getMaintenanceRequestFallback(Long requestId, Exception ex) {
        log.error("Circuit breaker OPEN for Maintenance Service. Request ID: {}, cause: {}",
                requestId, ex.getMessage());
        throw new ServiceUnavailableException("Maintenance Service", ex);
    }
}

