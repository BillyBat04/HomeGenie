package com.homegenie.paymentservice.service;

import com.homegenie.paymentservice.dto.MiniAppPaymentRequest;
import com.homegenie.paymentservice.dto.MiniAppPaymentResponse;
import com.stripe.exception.StripeException;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentPlatformService {

    MiniAppPaymentResponse refundMiniAppPayment(Long paymentId, BigDecimal amount) throws StripeException;

    MiniAppPaymentResponse createMiniAppPayment(MiniAppPaymentRequest request) throws StripeException;

    MiniAppPaymentResponse getPaymentByOrderId(Long orderId);

    List<MiniAppPaymentResponse> getPaymentsByMiniApp(String miniAppId);

    List<MiniAppPaymentResponse> getUserPaymentsInMiniApp(Long userId, String miniAppId);

    BigDecimal getTotalCommission(String miniAppId);
}
