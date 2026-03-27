package com.homegenie.paymentservice.service;

import com.homegenie.paymentservice.dto.PaymentRequest;
import com.homegenie.paymentservice.dto.PaymentResponse;
import com.homegenie.paymentservice.model.Payment;
import com.homegenie.paymentservice.repository.PaymentRepository;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("null")
public class PaymentService {

    private final StripePaymentService stripePaymentService;
    private final PaymentRepository paymentRepository;
    private final MaintenanceServiceClient maintenanceServiceClient;
    private final PaymentEventPublisher paymentEventPublisher;

    /**
     * Create a new payment
     * 
     * BUSINESS RULE: Payment can only be created if maintenance request:
     * 1. Is not in PENDING status (must be assigned to technician)
     * 2. Has an assigned technician (assignedTo != null)
     * 3. Status is IN_PROGRESS or COMPLETED
     * 
     * ISSUE-006 FIX: Implements saga compensation pattern
     * If Stripe succeeds but DB save fails, automatically cancel Stripe payment
     */
    @Transactional
    public PaymentResponse createPayment(PaymentRequest request) throws StripeException {
        log.info("Creating payment for user {} and request {}", request.getUserId(), request.getRequestId());

        // Validate if payment already exists for this request
        if (paymentRepository.existsByOrderId(request.getRequestId())) {
            throw new IllegalStateException("Payment already exists for request: " + request.getRequestId());
        }

        // CRITICAL FIX: Validate maintenance request status before payment
        // This prevents payment for requests that haven't been assigned to technician yet
        maintenanceServiceClient.validatePaymentAllowed(request.getRequestId());

        Payment payment = null;
        String stripePaymentIntentId = null;
        
        try {
            // Step 1: Create payment in Stripe (external system)
            payment = stripePaymentService.createPayment(request);
            stripePaymentIntentId = payment.getStripePaymentIntentId();
            
            // Step 2: Save to database (local system)
            // If this fails, we need to cancel Stripe payment (compensation)
            payment = paymentRepository.save(payment);
            
            // Publish Kafka event so notification-service sends confirmation email to user
            paymentEventPublisher.publishPaymentConfirmed(payment);
            
            log.info("✅ Saga completed successfully: payment {} created", payment.getId());
            return PaymentResponse.fromEntity(payment);
            
        } catch (Exception e) {
            log.error("❌ Saga failed during payment creation: {}", e.getMessage(), e);
            
            // COMPENSATION: Cancel Stripe payment if it was created
            if (stripePaymentIntentId != null) {
                try {
                    log.warn("⚠️ Compensating saga: canceling Stripe payment intent {}", stripePaymentIntentId);
                    stripePaymentService.cancelPaymentByIntentId(stripePaymentIntentId);
                    log.info("✅ Compensation successful: Stripe payment canceled");
                } catch (Exception compensationError) {
                    log.error("❌❌ CRITICAL: Compensation failed! Manual intervention required. " +
                            "Orphaned Stripe payment intent: {}. Error: {}", 
                            stripePaymentIntentId, compensationError.getMessage());
                    // In production: Send alert to ops team, create incident ticket
                }
            }
            
            // Re-throw original exception
            if (e instanceof StripeException) {
                throw (StripeException) e;
            }
            throw new RuntimeException("Payment creation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get payment by ID
     */
    public PaymentResponse getPaymentById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
        return PaymentResponse.fromEntity(payment);
    }

    /**
     * Get payments by user ID
     */
    public List<PaymentResponse> getPaymentsByUserId(Long userId) {
        List<Payment> payments = paymentRepository.findByUserId(userId);
        return payments.stream()
                .map(PaymentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get payment by request ID
     */
    public PaymentResponse getPaymentByRequestId(Long requestId) {
        List<Payment> payments = paymentRepository.findByOrderId(requestId);
        if (payments.isEmpty()) {
            throw new RuntimeException("Payment not found for request: " + requestId);
        }
        return PaymentResponse.fromEntity(payments.get(0));
    }

    /**
     * Get payments by status
     */
    public List<PaymentResponse> getPaymentsByStatus(Payment.PaymentStatus status) {
        List<Payment> payments = paymentRepository.findByStatus(status);
        return payments.stream()
                .map(PaymentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Refund a payment (full or partial)
     */
    @Transactional
    public PaymentResponse refundPayment(Long paymentId, BigDecimal amount) throws StripeException {
        log.info("Refunding payment {} with amount {}", paymentId, amount);
        Payment payment = stripePaymentService.refundPayment(paymentId, amount);
        return PaymentResponse.fromEntity(payment);
    }

    /**
     * Cancel a payment
     */
    @Transactional
    public PaymentResponse cancelPayment(Long paymentId) throws StripeException {
        log.info("Canceling payment: {}", paymentId);
        Payment payment = stripePaymentService.cancelPayment(paymentId);
        return PaymentResponse.fromEntity(payment);
    }

    /**
     * Confirm payment (usually called from webhook)
     */
    @Transactional
    public PaymentResponse confirmPayment(String paymentIntentId) throws StripeException {
        log.info("Confirming payment intent: {}", paymentIntentId);
        Payment payment = stripePaymentService.confirmPayment(paymentIntentId);
        return PaymentResponse.fromEntity(payment);
    }
}
