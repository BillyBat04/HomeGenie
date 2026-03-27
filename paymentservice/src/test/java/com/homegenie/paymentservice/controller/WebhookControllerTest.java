package com.homegenie.paymentservice.controller;

import com.homegenie.paymentservice.service.StripePaymentService;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for WebhookController
 *
 * CRITICAL Security Tests - Stripe Webhook Validation
 *
 * Test Coverage:
 * - Valid webhook signature verification
 * - Invalid signature rejection
 * - Missing signature header
 * - Malformed payload handling
 * - Event type routing
 * - Payment success events
 * - Payment failure events
 * - Refund events
 * - Unknown event types
 * - Signature replay attack prevention
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class WebhookControllerTest {

    @Mock
    private StripePaymentService stripePaymentService;

    @InjectMocks
    private WebhookController webhookController;

    private static final String WEBHOOK_SECRET = "whsec_test_secret";
    private static final String VALID_SIGNATURE = "t=1614297600,v1=valid_signature_hash";
    private static final String INVALID_SIGNATURE = "t=1614297600,v1=invalid_signature";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(webhookController, "webhookSecret", WEBHOOK_SECRET);
    }

    @Test
    void testHandleStripeWebhook_ValidSignature_Success() {
        // Given
        String payload = "{\"type\":\"payment_intent.succeeded\",\"data\":{\"object\":{\"id\":\"pi_test123\"}}}";
        String signature = VALID_SIGNATURE;

        // Mock Webhook.constructEvent
        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            Event mockEvent = mock(Event.class);
            when(mockEvent.getType()).thenReturn("payment_intent.succeeded");

            mockedWebhook.when(() -> Webhook.constructEvent(payload, signature, WEBHOOK_SECRET))
                    .thenReturn(mockEvent);

            doNothing().when(stripePaymentService).handleWebhookEvent(mockEvent);

            // When
            ResponseEntity<String> response = webhookController.handleStripeWebhook(payload, signature);

            // Then
            assertEquals("Webhook processed successfully", response.getBody());
            verify(stripePaymentService, times(1)).handleWebhookEvent(mockEvent);
        }
    }

    @Test
    void testHandleStripeWebhook_InvalidSignature_ThrowsException() {
        // Given
        String payload = "{\"type\":\"payment_intent.succeeded\"}";
        String invalidSignature = INVALID_SIGNATURE;

        // Mock Webhook.constructEvent to throw exception
        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent(payload, invalidSignature, WEBHOOK_SECRET))
                    .thenThrow(new com.stripe.exception.SignatureVerificationException(
                            "Invalid signature", invalidSignature));

            // When & Then
            assertThrows(Exception.class, () -> {
                webhookController.handleStripeWebhook(payload, invalidSignature);
            });

            verify(stripePaymentService, never()).handleWebhookEvent(any());
        }
    }

    @Test
    void testHandleStripeWebhook_MissingSignature_ThrowsException() {
        // Given
        String payload = "{\"type\":\"payment_intent.succeeded\"}";
        String nullSignature = null;

        // When & Then
        assertThrows(Exception.class, () -> {
            webhookController.handleStripeWebhook(payload, nullSignature);
        });

        verify(stripePaymentService, never()).handleWebhookEvent(any());
    }

    @Test
    void testHandleStripeWebhook_EmptySignature_ThrowsException() {
        // Given
        String payload = "{\"type\":\"payment_intent.succeeded\"}";
        String emptySignature = "";

        // When & Then
        assertThrows(Exception.class, () -> {
            webhookController.handleStripeWebhook(payload, emptySignature);
        });

        verify(stripePaymentService, never()).handleWebhookEvent(any());
    }

    @Test
    void testHandleStripeWebhook_MalformedPayload_ThrowsException() {
        // Given
        String malformedPayload = "not-valid-json";
        String signature = VALID_SIGNATURE;

        // Mock Webhook.constructEvent to throw exception
        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent(malformedPayload, signature, WEBHOOK_SECRET))
                    .thenThrow(new IllegalArgumentException("Invalid JSON"));

            // When & Then
            assertThrows(Exception.class, () -> {
                webhookController.handleStripeWebhook(malformedPayload, signature);
            });

            verify(stripePaymentService, never()).handleWebhookEvent(any());
        }
    }

    @Test
    void testHandleStripeWebhook_PaymentIntentSucceeded_ProcessedCorrectly() {
        // Given
        String payload = "{\"type\":\"payment_intent.succeeded\",\"data\":{\"object\":{\"id\":\"pi_test123\"}}}";
        String signature = VALID_SIGNATURE;

        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            Event mockEvent = mock(Event.class);
            when(mockEvent.getType()).thenReturn("payment_intent.succeeded");

            mockedWebhook.when(() -> Webhook.constructEvent(payload, signature, WEBHOOK_SECRET))
                    .thenReturn(mockEvent);

            doNothing().when(stripePaymentService).handleWebhookEvent(mockEvent);

            // When
            ResponseEntity<String> response = webhookController.handleStripeWebhook(payload, signature);

            // Then
            assertEquals("Webhook processed successfully", response.getBody());
            verify(stripePaymentService, times(1)).handleWebhookEvent(argThat(event ->
                event.getType().equals("payment_intent.succeeded")
            ));
        }
    }

    @Test
    void testHandleStripeWebhook_PaymentIntentFailed_ProcessedCorrectly() {
        // Given
        String payload = "{\"type\":\"payment_intent.payment_failed\",\"data\":{\"object\":{\"id\":\"pi_test123\"}}}";
        String signature = VALID_SIGNATURE;

        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            Event mockEvent = mock(Event.class);
            when(mockEvent.getType()).thenReturn("payment_intent.payment_failed");

            mockedWebhook.when(() -> Webhook.constructEvent(payload, signature, WEBHOOK_SECRET))
                    .thenReturn(mockEvent);

            doNothing().when(stripePaymentService).handleWebhookEvent(mockEvent);

            // When
            ResponseEntity<String> response = webhookController.handleStripeWebhook(payload, signature);

            // Then
            assertEquals("Webhook processed successfully", response.getBody());
            verify(stripePaymentService, times(1)).handleWebhookEvent(any());
        }
    }

    @Test
    void testHandleStripeWebhook_ChargeRefunded_ProcessedCorrectly() {
        // Given
        String payload = "{\"type\":\"charge.refunded\",\"data\":{\"object\":{\"id\":\"ch_test123\"}}}";
        String signature = VALID_SIGNATURE;

        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            Event mockEvent = mock(Event.class);
            when(mockEvent.getType()).thenReturn("charge.refunded");

            mockedWebhook.when(() -> Webhook.constructEvent(payload, signature, WEBHOOK_SECRET))
                    .thenReturn(mockEvent);

            doNothing().when(stripePaymentService).handleWebhookEvent(mockEvent);

            // When
            ResponseEntity<String> response = webhookController.handleStripeWebhook(payload, signature);

            // Then
            assertEquals("Webhook processed successfully", response.getBody());
            verify(stripePaymentService, times(1)).handleWebhookEvent(any());
        }
    }

    @Test
    void testHandleStripeWebhook_UnknownEventType_StillProcessed() {
        // Given
        String payload = "{\"type\":\"unknown.event.type\",\"data\":{\"object\":{\"id\":\"obj_test123\"}}}";
        String signature = VALID_SIGNATURE;

        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            Event mockEvent = mock(Event.class);
            when(mockEvent.getType()).thenReturn("unknown.event.type");

            mockedWebhook.when(() -> Webhook.constructEvent(payload, signature, WEBHOOK_SECRET))
                    .thenReturn(mockEvent);

            doNothing().when(stripePaymentService).handleWebhookEvent(mockEvent);

            // When
            ResponseEntity<String> response = webhookController.handleStripeWebhook(payload, signature);

            // Then
            assertEquals("Webhook processed successfully", response.getBody());
            // Should still call service (service will handle unknown types)
            verify(stripePaymentService, times(1)).handleWebhookEvent(any());
        }
    }

    @Test
    void testHandleStripeWebhook_OldTimestamp_ThrowsException() {
        // Given - Signature with old timestamp (replay attack)
        String payload = "{\"type\":\"payment_intent.succeeded\"}";
        String oldSignature = "t=1000000000,v1=old_signature"; // Very old timestamp

        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent(payload, oldSignature, WEBHOOK_SECRET))
                    .thenThrow(new com.stripe.exception.SignatureVerificationException(
                            "Timestamp outside tolerance", oldSignature));

            // When & Then
            assertThrows(Exception.class, () -> {
                webhookController.handleStripeWebhook(payload, oldSignature);
            });

            verify(stripePaymentService, never()).handleWebhookEvent(any());
        }
    }

    @Test
    void testHandleStripeWebhook_ServiceThrowsException_ExceptionPropagated() {
        // Given
        String payload = "{\"type\":\"payment_intent.succeeded\"}";
        String signature = VALID_SIGNATURE;

        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            Event mockEvent = mock(Event.class);
            when(mockEvent.getType()).thenReturn("payment_intent.succeeded");

            mockedWebhook.when(() -> Webhook.constructEvent(payload, signature, WEBHOOK_SECRET))
                    .thenReturn(mockEvent);

            // Service throws exception during processing
            doThrow(new RuntimeException("Database error"))
                    .when(stripePaymentService).handleWebhookEvent(mockEvent);

            // When & Then
            assertThrows(RuntimeException.class, () -> {
                webhookController.handleStripeWebhook(payload, signature);
            });

            verify(stripePaymentService, times(1)).handleWebhookEvent(any());
        }
    }

    @Test
    void testHandleStripeWebhook_EmptyPayload_ThrowsException() {
        // Given
        String emptyPayload = "";
        String signature = VALID_SIGNATURE;

        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent(emptyPayload, signature, WEBHOOK_SECRET))
                    .thenThrow(new IllegalArgumentException("Empty payload"));

            // When & Then
            assertThrows(Exception.class, () -> {
                webhookController.handleStripeWebhook(emptyPayload, signature);
            });

            verify(stripePaymentService, never()).handleWebhookEvent(any());
        }
    }

    @Test
    void testHandleStripeWebhook_NullPayload_ThrowsException() {
        // Given
        String nullPayload = null;
        String signature = VALID_SIGNATURE;

        // When & Then
        assertThrows(Exception.class, () -> {
            webhookController.handleStripeWebhook(nullPayload, signature);
        });

        verify(stripePaymentService, never()).handleWebhookEvent(any());
    }
}

