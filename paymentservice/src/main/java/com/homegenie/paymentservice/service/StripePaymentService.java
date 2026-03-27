package com.homegenie.paymentservice.service;

import com.homegenie.paymentservice.dto.PaymentRequest;
import com.homegenie.paymentservice.model.Payment;
import com.homegenie.paymentservice.model.Transaction;
import com.homegenie.paymentservice.repository.PaymentRepository;
import com.homegenie.paymentservice.repository.TransactionRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.param.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("null")
public class StripePaymentService {

    private final PaymentRepository paymentRepository;
    private final TransactionRepository transactionRepository;

    @Value("${stripe.api-key}")
    private String stripeApiKey;

    @Value("${stripe.currency:usd}")
    private String defaultCurrency;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
        log.info("Stripe API initialized successfully");
    }

    /**
     * Create a payment intent with Stripe
     */
    @Transactional
    public Payment createPayment(PaymentRequest request) throws StripeException {
        log.info("Creating payment for user {} and request {}", request.getUserId(), request.getRequestId());

        // Create payment entity
        Payment payment = Payment.builder()
                .userId(request.getUserId())
                .orderId(request.getRequestId())
                .miniAppId("maintenance")
                .amount(request.getAmount())
                .currency(request.getCurrency().toUpperCase())
                .status(Payment.PaymentStatus.PENDING)
                .paymentMethod(Payment.PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase()))
                .description(request.getDescription())
                .stripeCustomerId(request.getCustomerId())
                .build();

        // Save payment first
        payment = paymentRepository.save(payment);

        try {
            // Convert amount to smallest currency unit (cents for USD)
            Long amountInCents = request.getAmount().multiply(BigDecimal.valueOf(100)).longValue();

            // Create Stripe PaymentIntent
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(request.getCurrency().toLowerCase())
                    .setDescription(request.getDescription())
                    .putMetadata("userId", request.getUserId().toString())
                    .putMetadata("orderId", request.getRequestId().toString())
                    .putMetadata("miniAppId", "maintenance")
                    .putMetadata("paymentId", payment.getId().toString())
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            // Update payment with Stripe details
            payment.setStripePaymentIntentId(paymentIntent.getId());
            payment.setStatus(Payment.PaymentStatus.PROCESSING);
            payment = paymentRepository.save(payment);

            // Create transaction record
            createTransaction(payment, Transaction.TransactionType.AUTHORIZATION, payment.getAmount(),
                    "Payment intent created: " + paymentIntent.getId());

            log.info("Payment created successfully: {}", payment.getId());
            return payment;

        } catch (StripeException e) {
            log.error("Stripe error creating payment: {}", e.getMessage(), e);
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setFailureReason(e.getMessage());
            paymentRepository.save(payment);
            throw e;
        }
    }

    /**
     * Confirm a payment (when payment succeeds)
     */
    @Transactional
    public Payment confirmPayment(String paymentIntentId) throws StripeException {
        log.info("Confirming payment intent: {}", paymentIntentId);

        Payment payment = paymentRepository.findByStripePaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new RuntimeException("Payment not found for intent: " + paymentIntentId));

        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);

            if ("succeeded".equals(paymentIntent.getStatus())) {
                payment.setStatus(Payment.PaymentStatus.SUCCEEDED);
                payment.setPaidAt(LocalDateTime.now());
                
                // Get charge ID and receipt URL
                if (paymentIntent.getLatestCharge() != null) {
                    Charge charge = Charge.retrieve(paymentIntent.getLatestCharge());
                    payment.setStripeChargeId(charge.getId());
                    payment.setReceiptUrl(charge.getReceiptUrl());
                }

                payment = paymentRepository.save(payment);

                // Create transaction
                createTransaction(payment, Transaction.TransactionType.CHARGE, payment.getAmount(),
                        "Payment succeeded");

                log.info("Payment confirmed successfully: {}", payment.getId());
            }

            return payment;

        } catch (StripeException e) {
            log.error("Error confirming payment: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Refund a payment
     */
    @Transactional
    public Payment refundPayment(Long paymentId, BigDecimal amount) throws StripeException {
        log.info("Refunding payment: {} with amount: {}", paymentId, amount);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        if (payment.getStatus() != Payment.PaymentStatus.SUCCEEDED) {
            throw new IllegalStateException("Can only refund succeeded payments");
        }

        try {
            Long refundAmount = amount != null 
                    ? amount.multiply(BigDecimal.valueOf(100)).longValue()
                    : null; // null means full refund

            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(payment.getStripePaymentIntentId())
                    .setAmount(refundAmount)
                    .build();

            Refund refund = Refund.create(params);

            // Update payment status
            if (refund.getAmount().equals(payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue())) {
                payment.setStatus(Payment.PaymentStatus.REFUNDED);
            } else {
                payment.setStatus(Payment.PaymentStatus.PARTIALLY_REFUNDED);
            }

            payment = paymentRepository.save(payment);

            // Create refund transaction
            BigDecimal refundAmountDecimal = BigDecimal.valueOf(refund.getAmount()).divide(BigDecimal.valueOf(100));
            createTransaction(payment, 
                    amount != null ? Transaction.TransactionType.PARTIAL_REFUND : Transaction.TransactionType.REFUND,
                    refundAmountDecimal, "Refund: " + refund.getId());

            log.info("Payment refunded successfully: {}", payment.getId());
            return payment;

        } catch (StripeException e) {
            log.error("Error refunding payment: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Cancel a payment intent
     */
    @Transactional
    public Payment cancelPayment(Long paymentId) throws StripeException {
        log.info("Canceling payment: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        if (payment.getStatus() == Payment.PaymentStatus.SUCCEEDED) {
            throw new IllegalStateException("Cannot cancel succeeded payment. Use refund instead.");
        }

        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(payment.getStripePaymentIntentId());
            paymentIntent.cancel();

            payment.setStatus(Payment.PaymentStatus.CANCELED);
            payment = paymentRepository.save(payment);

            createTransaction(payment, Transaction.TransactionType.CANCELLATION, BigDecimal.ZERO,
                    "Payment canceled");

            log.info("Payment canceled successfully: {}", payment.getId());
            return payment;

        } catch (StripeException e) {
            log.error("Error canceling payment: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Handle Stripe webhook events
     */
    @Transactional
    public void handleWebhookEvent(Event event) {
        log.info("Processing webhook event: {} - {}", event.getId(), event.getType());

        try {
            switch (event.getType()) {
                case "payment_intent.succeeded":
                    handlePaymentIntentSucceeded(event);
                    break;
                case "payment_intent.payment_failed":
                    handlePaymentIntentFailed(event);
                    break;
                case "charge.refunded":
                    handleChargeRefunded(event);
                    break;
                default:
                    log.info("Unhandled event type: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Error handling webhook event: {}", e.getMessage(), e);
        }
    }

    private void handlePaymentIntentSucceeded(Event event) throws StripeException {
        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                .getObject().orElseThrow();
        confirmPayment(paymentIntent.getId());
    }

    private void handlePaymentIntentFailed(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                .getObject().orElseThrow();

        paymentRepository.findByStripePaymentIntentId(paymentIntent.getId())
                .ifPresent(payment -> {
                    payment.setStatus(Payment.PaymentStatus.FAILED);
                    payment.setFailureReason(paymentIntent.getLastPaymentError() != null
                            ? paymentIntent.getLastPaymentError().getMessage()
                            : "Payment failed");
                    paymentRepository.save(payment);
                });
    }

    private void handleChargeRefunded(Event event) {
        Charge charge = (Charge) event.getDataObjectDeserializer()
                .getObject().orElseThrow();

        paymentRepository.findByStripeChargeId(charge.getId())
                .ifPresent(payment -> {
                    payment.setStatus(Payment.PaymentStatus.REFUNDED);
                    paymentRepository.save(payment);
                });
    }

    /**
     * Helper method to create transaction records
     */
    private void createTransaction(Payment payment, Transaction.TransactionType type,
                                    BigDecimal amount, String description) {
        Transaction transaction = Transaction.builder()
                .payment(payment)
                .transactionId(UUID.randomUUID().toString())
                .type(type)
                .amount(amount)
                .currency(payment.getCurrency())
                .status(Transaction.TransactionStatus.SUCCEEDED)
                .description(description)
                .build();

        transactionRepository.save(transaction);
        log.debug("Transaction created: {} for payment: {}", transaction.getTransactionId(), payment.getId());
    }
    
    /**
     * ISSUE-006 FIX: Cancel payment by intent ID (for saga compensation)
     * Used when database transaction fails after Stripe payment created
     */
    public void cancelPaymentByIntentId(String paymentIntentId) throws StripeException {
        log.warn("⚠️ Canceling Stripe payment intent for compensation: {}", paymentIntentId);
        
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            
            // Can only cancel if payment is in cancelable state
            if ("requires_payment_method".equals(paymentIntent.getStatus()) ||
                "requires_confirmation".equals(paymentIntent.getStatus()) ||
                "requires_action".equals(paymentIntent.getStatus()) ||
                "processing".equals(paymentIntent.getStatus())) {
                
                PaymentIntentCancelParams params = PaymentIntentCancelParams.builder().build();
                paymentIntent.cancel(params);
                log.info("✅ Stripe payment intent canceled: {}", paymentIntentId);
            } else {
                log.warn("⚠️ Cannot cancel payment intent {} in status: {}", 
                        paymentIntentId, paymentIntent.getStatus());
            }
        } catch (StripeException e) {
            log.error("❌ Failed to cancel Stripe payment intent {}: {}", paymentIntentId, e.getMessage());
            throw e;
        }
    }
}
