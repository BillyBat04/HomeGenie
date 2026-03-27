package com.homegenie.maintenanceservice.client;

import com.homegenie.maintenanceservice.dto.payment.MiniAppPaymentRequest;
import com.homegenie.maintenanceservice.dto.payment.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Client for calling Payment Service Platform v2 API
 */
@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings("null")
public class PaymentServiceClient {

    private final RestTemplate restTemplate;

    @Value("${payment.service.url:http://localhost:8083}")
    private String paymentServiceUrl;

    /**
     * Create payment via Payment Platform v2 mini-app endpoint
     */
    public PaymentResponse createPayment(MiniAppPaymentRequest request) {
        String url = paymentServiceUrl + "/api/payments/mini-app";
        
        log.info("Creating payment for maintenance request: userId={}, orderId={}, amount={}", 
                request.getUserId(), request.getOrderId(), request.getAmount());
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<MiniAppPaymentRequest> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<PaymentResponse> response = restTemplate.postForEntity(url, entity, PaymentResponse.class);
            
            log.info("Payment created successfully: paymentId={}", response.getBody().getPaymentId());
            return response.getBody();
            
        } catch (Exception e) {
            log.error("Failed to create payment for maintenance request {}: {}", request.getOrderId(), e.getMessage());
            throw new RuntimeException("Payment service unavailable: " + e.getMessage(), e);
        }
    }
}
