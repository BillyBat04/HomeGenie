package com.homegenie.paymentservice.service.impl;


import com.homegenie.paymentservice.service.PaymentService;
import com.homegenie.paymentservice.service.*;
import com.homegenie.paymentservice.dto.PaymentRequest;
import com.homegenie.paymentservice.dto.PaymentResponse;
import com.homegenie.paymentservice.exception.PaymentAlreadyExistsException;
import com.homegenie.paymentservice.model.Payment;
import com.homegenie.paymentservice.repository.PaymentRepository;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("null")
public class PaymentServiceImpl implements PaymentService {

    private final StripePaymentService stripePaymentService;
    private final PaymentRepository paymentRepository;
    private final MaintenanceServiceClient maintenanceServiceClient;
    private final PaymentEventPublisher paymentEventPublisher;

    
    @Transactional
    public PaymentResponse createPayment(PaymentRequest request) throws StripeException {
        log.info("Creating payment for user {} and request {}", request.getUserId(), request.getRequestId());

        // Optimistic duplicate check (fast path); DB unique constraint is the safety net
        if (paymentRepository.existsByOrderId(request.getRequestId())) {
            throw new PaymentAlreadyExistsException(request.getRequestId());
        }

        
        
        maintenanceServiceClient.validatePaymentAllowed(request.getRequestId());

        Payment payment = null;
        String stripePaymentIntentId = null;
        
        try {
            
            payment = stripePaymentService.createPayment(request);
            stripePaymentIntentId = payment.getStripePaymentIntentId();
            
            
            
            payment = paymentRepository.save(payment);
            paymentEventPublisher.publishPaymentConfirmed(payment);
            log.info("✅ Saga completed successfully: payment {} created", payment.getId());
            return PaymentResponse.fromEntity(payment);

        } catch (DataIntegrityViolationException e) {
            // Race condition: another instance inserted the same order_id concurrently
            log.warn("Duplicate order detected via DB constraint for request {}", request.getRequestId());
            if (stripePaymentIntentId != null) {
                try { stripePaymentService.cancelPaymentByIntentId(stripePaymentIntentId); }
                catch (Exception ce) { log.error("Compensation failed for intent {}: {}", stripePaymentIntentId, ce.getMessage()); }
            }
            throw new PaymentAlreadyExistsException(request.getRequestId());
        } catch (Exception e) {
            log.error("❌ Saga failed during payment creation: {}", e.getMessage(), e);
            
            
            if (stripePaymentIntentId != null) {
                try {
                    log.warn("⚠️ Compensating saga: canceling Stripe payment intent {}", stripePaymentIntentId);
                    stripePaymentService.cancelPaymentByIntentId(stripePaymentIntentId);
                    log.info("✅ Compensation successful: Stripe payment canceled");
                } catch (Exception compensationError) {
                    log.error("❌❌ CRITICAL: Compensation failed! Manual intervention required. " +
                            "Orphaned Stripe payment intent: {}. Error: {}", 
                            stripePaymentIntentId, compensationError.getMessage());
                    
                }
            }
            
            
            if (e instanceof StripeException) {
                throw (StripeException) e;
            }
            throw new RuntimeException("Payment creation failed: " + e.getMessage(), e);
        }
    }

    
    public PaymentResponse getPaymentById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
        return PaymentResponse.fromEntity(payment);
    }

    
    public List<PaymentResponse> getPaymentsByUserId(Long userId) {
        List<Payment> payments = paymentRepository.findByUserId(userId);
        return payments.stream()
                .map(PaymentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    
    public PaymentResponse getPaymentByRequestId(Long requestId) {
        List<Payment> payments = paymentRepository.findByOrderId(requestId);
        if (payments.isEmpty()) {
            throw new RuntimeException("Payment not found for request: " + requestId);
        }
        return PaymentResponse.fromEntity(payments.get(0));
    }

    
    public List<PaymentResponse> getPaymentsByStatus(Payment.PaymentStatus status) {
        List<Payment> payments = paymentRepository.findByStatus(status);
        return payments.stream()
                .map(PaymentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    
    @Transactional
    public PaymentResponse refundPayment(Long paymentId, BigDecimal amount) throws StripeException {
        log.info("Refunding payment {} with amount {}", paymentId, amount);
        Payment payment = stripePaymentService.refundPayment(paymentId, amount);
        return PaymentResponse.fromEntity(payment);
    }

    
    @Transactional
    public PaymentResponse cancelPayment(Long paymentId) throws StripeException {
        log.info("Canceling payment: {}", paymentId);
        Payment payment = stripePaymentService.cancelPayment(paymentId);
        return PaymentResponse.fromEntity(payment);
    }

    
    @Transactional
    public PaymentResponse confirmPayment(String paymentIntentId) throws StripeException {
        log.info("Confirming payment intent: {}", paymentIntentId);
        Payment payment = stripePaymentService.confirmPayment(paymentIntentId);
        return PaymentResponse.fromEntity(payment);
    }
}
