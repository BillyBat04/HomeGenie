package com.homegenie.paymentservice.service;

import com.homegenie.paymentservice.dto.PaymentRequest;
import com.homegenie.paymentservice.model.Payment;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;

import java.math.BigDecimal;

public interface StripePaymentService {

    Payment createPayment(PaymentRequest request) throws StripeException;

    Payment confirmPayment(String paymentIntentId) throws StripeException;

    Payment refundPayment(Long paymentId, BigDecimal amount) throws StripeException;

    Payment cancelPayment(Long paymentId) throws StripeException;

    void handleWebhookEvent(Event event);

    void cancelPaymentByIntentId(String paymentIntentId) throws StripeException;
}
