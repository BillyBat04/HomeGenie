package com.homegenie.paymentservice.service;

import com.homegenie.paymentservice.dto.PaymentRequest;
import com.homegenie.paymentservice.dto.PaymentResponse;
import com.homegenie.paymentservice.model.Payment;
import com.homegenie.paymentservice.repository.PaymentRepository;
import com.stripe.exception.StripeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class PaymentServiceTest {

    @Mock
    private StripePaymentService stripePaymentService;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    private PaymentRequest paymentRequest;
    private Payment payment;

    @BeforeEach
    void setUp() {
        paymentRequest = new PaymentRequest();
        paymentRequest.setUserId(1L);
        paymentRequest.setRequestId(1L);
        paymentRequest.setAmount(new BigDecimal("100.00"));
        paymentRequest.setCurrency("USD");
        paymentRequest.setPaymentMethod("CREDIT_CARD");
        paymentRequest.setDescription("Payment for maintenance request");

        payment = Payment.builder()
                .id(1L)
                .userId(1L)
                .orderId(1L)
                .miniAppId("maintenance")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .status(Payment.PaymentStatus.PENDING)
                .paymentMethod(Payment.PaymentMethod.CREDIT_CARD)
                .stripePaymentIntentId("pi_test123")
                .build();
    }

    @Test
    void testCreatePayment_Success() throws StripeException {
        
        when(paymentRepository.existsByOrderId(anyLong())).thenReturn(false);
        when(stripePaymentService.createPayment(any(PaymentRequest.class))).thenReturn(payment);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        
        PaymentResponse result = paymentService.createPayment(paymentRequest);

        
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(new BigDecimal("100.00"), result.getAmount());
        assertEquals("pi_test123", result.getStripePaymentIntentId());

        verify(paymentRepository, times(1)).existsByOrderId(1L);
        verify(stripePaymentService, times(1)).createPayment(paymentRequest);
    }

    @Test
    void testCreatePayment_DuplicateRequest_ThrowsException() throws StripeException {
        
        when(paymentRepository.existsByOrderId(anyLong())).thenReturn(true);

        
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            paymentService.createPayment(paymentRequest);
        });

        assertTrue(exception.getMessage().contains("Payment already exists"));
        verify(paymentRepository, times(1)).existsByOrderId(1L);
        verify(stripePaymentService, never()).createPayment(any());
    }

    @Test
    void testConfirmPayment_Success() throws StripeException {
        
        payment.setStatus(Payment.PaymentStatus.SUCCEEDED);
        when(stripePaymentService.confirmPayment(anyString())).thenReturn(payment);

        
        PaymentResponse result = paymentService.confirmPayment("pi_test123");

        
        assertNotNull(result);
        assertEquals("SUCCEEDED", result.getStatus());
        verify(stripePaymentService, times(1)).confirmPayment("pi_test123");
    }

    @Test
    void testGetPaymentById_Success() {
        
        when(paymentRepository.findById(anyLong())).thenReturn(Optional.of(payment));

        
        PaymentResponse result = paymentService.getPaymentById(1L);

        
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(paymentRepository, times(1)).findById(1L);
    }

    @Test
    void testGetPaymentById_NotFound_ThrowsException() {
        
        when(paymentRepository.findById(anyLong())).thenReturn(Optional.empty());

        
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            paymentService.getPaymentById(999L);
        });

        assertTrue(exception.getMessage().contains("not found"));
        verify(paymentRepository, times(1)).findById(999L);
    }
}

