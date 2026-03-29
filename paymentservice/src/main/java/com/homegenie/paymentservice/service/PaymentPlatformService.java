package com.homegenie.paymentservice.service;

import com.homegenie.paymentservice.config.PaymentPlatformConfig;
import com.homegenie.paymentservice.dto.MiniAppPaymentRequest;
import com.homegenie.paymentservice.dto.MiniAppPaymentResponse;
import com.homegenie.paymentservice.model.Payment;
import com.homegenie.paymentservice.model.Payment.PaymentMethod;
import com.homegenie.paymentservice.model.Payment.PaymentStatus;
import com.homegenie.paymentservice.repository.PaymentRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;


@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("null")
public class PaymentPlatformService {

    private final PaymentRepository paymentRepository;
    private final PaymentPlatformConfig platformConfig;

    
    @Transactional
    public MiniAppPaymentResponse createMiniAppPayment(MiniAppPaymentRequest request) throws StripeException {
        log.info("Creating payment for mini-app: {} order: {}", request.getMiniAppId(), request.getOrderId());

        platformConfig.validateMiniApp(request.getMiniAppId());

        if (paymentRepository.existsByOrderId(request.getOrderId())) {
            throw new IllegalArgumentException("Payment already exists for order: " + request.getOrderId());
        }

        BigDecimal commissionRate = platformConfig.getCommissionRate(request.getMiniAppId());
        BigDecimal commission = request.getAmount()
                .multiply(commissionRate)
                .setScale(2, RoundingMode.HALF_UP);

        log.debug("Commission calculation - Amount: {}, Rate: {}, Commission: {}", 
                request.getAmount(), commissionRate, commission);

        PaymentIntent paymentIntent = createStripePaymentIntent(request, commission);

        Payment payment = Payment.builder()
                .userId(request.getUserId())
                .miniAppId(request.getMiniAppId())
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentMethod(PaymentMethod.valueOf(request.getPaymentMethod()))
                .status(PaymentStatus.PENDING)
                .stripePaymentIntentId(paymentIntent.getId())
                .stripeCustomerId(request.getStripeCustomerId())
                .commission(commission)
                .commissionRate(commissionRate)
                .metadataJson(convertMetadataToJson(request.getMetadata()))
                .createdAt(LocalDateTime.now())
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment created successfully - ID: {}, Stripe Intent: {}", 
                savedPayment.getId(), paymentIntent.getId());

        return MiniAppPaymentResponse.fromPayment(savedPayment);
    }

    public MiniAppPaymentResponse getPaymentByOrderId(Long orderId) {
        java.util.List<Payment> payments = paymentRepository.findByOrderId(orderId);
        if (payments.isEmpty()) {
            throw new IllegalArgumentException("Payment not found for order: " + orderId);
        }
        return MiniAppPaymentResponse.fromPayment(payments.get(0));
    }

    
    public java.util.List<MiniAppPaymentResponse> getPaymentsByMiniApp(String miniAppId) {
        platformConfig.validateMiniApp(miniAppId);
        return paymentRepository.findByMiniAppId(miniAppId).stream()
                .map(MiniAppPaymentResponse::fromPayment)
                .toList();
    }

    
    public java.util.List<MiniAppPaymentResponse> getUserPaymentsInMiniApp(Long userId, String miniAppId) {
        platformConfig.validateMiniApp(miniAppId);
        return paymentRepository.findByUserIdAndMiniAppId(userId, miniAppId).stream()
                .map(MiniAppPaymentResponse::fromPayment)
                .toList();
    }

    
    public BigDecimal getTotalCommission(String miniAppId) {
        platformConfig.validateMiniApp(miniAppId);
        return paymentRepository.findByMiniAppId(miniAppId).stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCEEDED)
                .map(Payment::getCommission)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    
    private PaymentIntent createStripePaymentIntent(MiniAppPaymentRequest request, BigDecimal commission) 
            throws StripeException {
        
        long amountInCents = request.getAmount().multiply(BigDecimal.valueOf(100)).longValue();
        
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency(request.getCurrency())
                .setCustomer(request.getStripeCustomerId())
                .setPaymentMethod(request.getPaymentMethodId())
                .setConfirm(false)
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .putMetadata("miniAppId", request.getMiniAppId())
                .putMetadata("orderId", request.getOrderId().toString())
                .putMetadata("commission", commission.toString())
                .putMetadata("userId", request.getUserId().toString())
                .build();

        PaymentIntent paymentIntent = PaymentIntent.create(params);
        log.debug("Stripe Payment Intent created - ID: {}, Status: {}", 
                paymentIntent.getId(), paymentIntent.getStatus());
        
        return paymentIntent;
    }

    
    private String convertMetadataToJson(java.util.Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(metadata);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.warn("Failed to convert metadata to JSON: {}", e.getMessage());
            return null;
        }
    }
}
