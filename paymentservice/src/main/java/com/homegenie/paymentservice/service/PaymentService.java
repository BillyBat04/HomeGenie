package com.homegenie.paymentservice.service;

import com.homegenie.paymentservice.dto.PaymentRequest;
import com.homegenie.paymentservice.dto.PaymentResponse;
import com.homegenie.paymentservice.model.Payment;
import com.stripe.exception.StripeException;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentService {

    PaymentResponse createPayment(PaymentRequest request) throws StripeException;

    PaymentResponse getPaymentById(Long paymentId);

    List<PaymentResponse> getPaymentsByUserId(Long userId);

    PaymentResponse getPaymentByRequestId(Long requestId);

    List<PaymentResponse> getPaymentsByStatus(Payment.PaymentStatus status);

    PaymentResponse refundPayment(Long paymentId, BigDecimal amount) throws StripeException;

    PaymentResponse cancelPayment(Long paymentId) throws StripeException;

    PaymentResponse confirmPayment(String paymentIntentId) throws StripeException;
}
