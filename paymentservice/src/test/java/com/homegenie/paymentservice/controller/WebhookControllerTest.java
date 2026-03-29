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
        
        String payload = "{\"type\":\"payment_intent.succeeded\",\"data\":{\"object\":{\"id\":\"pi_test123\"}}}";
        String signature = VALID_SIGNATURE;

        
        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            Event mockEvent = mock(Event.class);
            when(mockEvent.getType()).thenReturn("payment_intent.succeeded");

            mockedWebhook.when(() -> Webhook.constructEvent(payload, signature, WEBHOOK_SECRET))
                    .thenReturn(mockEvent);

            doNothing().when(stripePaymentService).handleWebhookEvent(mockEvent);

            
            ResponseEntity<String> response = webhookController.handleStripeWebhook(payload, signature);

            
            assertEquals("Webhook processed successfully", response.getBody());
            verify(stripePaymentService, times(1)).handleWebhookEvent(mockEvent);
        }
    }

    @Test
    void testHandleStripeWebhook_InvalidSignature_ThrowsException() {
        
        String payload = "{\"type\":\"payment_intent.succeeded\"}";
        String invalidSignature = INVALID_SIGNATURE;

        
        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent(payload, invalidSignature, WEBHOOK_SECRET))
                    .thenThrow(new com.stripe.exception.SignatureVerificationException(
                            "Invalid signature", invalidSignature));

            
            ResponseEntity<String> response = webhookController.handleStripeWebhook(payload, invalidSignature);
            assertEquals(400, response.getStatusCode().value());
            assertEquals("Invalid signature", response.getBody());

            verify(stripePaymentService, never()).handleWebhookEvent(any());
        }
    }

    @Test
    void testHandleStripeWebhook_MissingSignature_ReturnsBAD_REQUEST() {
        
        String payload = "{\"type\":\"payment_intent.succeeded\"}";
        String nullSignature = null;

        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent(payload, nullSignature, WEBHOOK_SECRET))
                    .thenThrow(new com.stripe.exception.SignatureVerificationException(
                            "Missing signature", nullSignature));

            ResponseEntity<String> response = webhookController.handleStripeWebhook(payload, nullSignature);
            assertEquals(400, response.getStatusCode().value());

            verify(stripePaymentService, never()).handleWebhookEvent(any());
        }
    }

    @Test
    void testHandleStripeWebhook_EmptySignature_ReturnsBAD_REQUEST() {
        
        String payload = "{\"type\":\"payment_intent.succeeded\"}";
        String emptySignature = "";

        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent(payload, emptySignature, WEBHOOK_SECRET))
                    .thenThrow(new com.stripe.exception.SignatureVerificationException(
                            "Empty signature", emptySignature));

            ResponseEntity<String> response = webhookController.handleStripeWebhook(payload, emptySignature);
            assertEquals(400, response.getStatusCode().value());

            verify(stripePaymentService, never()).handleWebhookEvent(any());
        }
    }

    @Test
    void testHandleStripeWebhook_MalformedPayload_ThrowsException() {
        
        String malformedPayload = "not-valid-json";
        String signature = VALID_SIGNATURE;

        
        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent(malformedPayload, signature, WEBHOOK_SECRET))
                    .thenThrow(new IllegalArgumentException("Invalid JSON"));

            
            ResponseEntity<String> response = webhookController.handleStripeWebhook(malformedPayload, signature);
            assertEquals(500, response.getStatusCode().value());

            verify(stripePaymentService, never()).handleWebhookEvent(any());
        }
    }

    @Test
    void testHandleStripeWebhook_PaymentIntentSucceeded_ProcessedCorrectly() {
        
        String payload = "{\"type\":\"payment_intent.succeeded\",\"data\":{\"object\":{\"id\":\"pi_test123\"}}}";
        String signature = VALID_SIGNATURE;

        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            Event mockEvent = mock(Event.class);
            when(mockEvent.getType()).thenReturn("payment_intent.succeeded");

            mockedWebhook.when(() -> Webhook.constructEvent(payload, signature, WEBHOOK_SECRET))
                    .thenReturn(mockEvent);

            doNothing().when(stripePaymentService).handleWebhookEvent(mockEvent);

            
            ResponseEntity<String> response = webhookController.handleStripeWebhook(payload, signature);

            
            assertEquals("Webhook processed successfully", response.getBody());
            verify(stripePaymentService, times(1)).handleWebhookEvent(argThat(event ->
                event.getType().equals("payment_intent.succeeded")
            ));
        }
    }

    @Test
    void testHandleStripeWebhook_PaymentIntentFailed_ProcessedCorrectly() {
        
        String payload = "{\"type\":\"payment_intent.payment_failed\",\"data\":{\"object\":{\"id\":\"pi_test123\"}}}";
        String signature = VALID_SIGNATURE;

        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            Event mockEvent = mock(Event.class);
            when(mockEvent.getType()).thenReturn("payment_intent.payment_failed");

            mockedWebhook.when(() -> Webhook.constructEvent(payload, signature, WEBHOOK_SECRET))
                    .thenReturn(mockEvent);

            doNothing().when(stripePaymentService).handleWebhookEvent(mockEvent);

            
            ResponseEntity<String> response = webhookController.handleStripeWebhook(payload, signature);

            
            assertEquals("Webhook processed successfully", response.getBody());
            verify(stripePaymentService, times(1)).handleWebhookEvent(any());
        }
    }

    @Test
    void testHandleStripeWebhook_ChargeRefunded_ProcessedCorrectly() {
        
        String payload = "{\"type\":\"charge.refunded\",\"data\":{\"object\":{\"id\":\"ch_test123\"}}}";
        String signature = VALID_SIGNATURE;

        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            Event mockEvent = mock(Event.class);
            when(mockEvent.getType()).thenReturn("charge.refunded");

            mockedWebhook.when(() -> Webhook.constructEvent(payload, signature, WEBHOOK_SECRET))
                    .thenReturn(mockEvent);

            doNothing().when(stripePaymentService).handleWebhookEvent(mockEvent);

            
            ResponseEntity<String> response = webhookController.handleStripeWebhook(payload, signature);

            
            assertEquals("Webhook processed successfully", response.getBody());
            verify(stripePaymentService, times(1)).handleWebhookEvent(any());
        }
    }

    @Test
    void testHandleStripeWebhook_UnknownEventType_StillProcessed() {
        
        String payload = "{\"type\":\"unknown.event.type\",\"data\":{\"object\":{\"id\":\"obj_test123\"}}}";
        String signature = VALID_SIGNATURE;

        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            Event mockEvent = mock(Event.class);
            when(mockEvent.getType()).thenReturn("unknown.event.type");

            mockedWebhook.when(() -> Webhook.constructEvent(payload, signature, WEBHOOK_SECRET))
                    .thenReturn(mockEvent);

            doNothing().when(stripePaymentService).handleWebhookEvent(mockEvent);

            
            ResponseEntity<String> response = webhookController.handleStripeWebhook(payload, signature);

            
            assertEquals("Webhook processed successfully", response.getBody());
            
            verify(stripePaymentService, times(1)).handleWebhookEvent(any());
        }
    }

    @Test
    void testHandleStripeWebhook_OldTimestamp_ThrowsException() {
        
        String payload = "{\"type\":\"payment_intent.succeeded\"}";
        String oldSignature = "t=1000000000,v1=old_signature"; 

        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent(payload, oldSignature, WEBHOOK_SECRET))
                    .thenThrow(new com.stripe.exception.SignatureVerificationException(
                            "Timestamp outside tolerance", oldSignature));

            
            ResponseEntity<String> response = webhookController.handleStripeWebhook(payload, oldSignature);
            assertEquals(400, response.getStatusCode().value());
            assertEquals("Invalid signature", response.getBody());

            verify(stripePaymentService, never()).handleWebhookEvent(any());
        }
    }

    @Test
    void testHandleStripeWebhook_ServiceThrowsException_ExceptionPropagated() {
        
        String payload = "{\"type\":\"payment_intent.succeeded\"}";
        String signature = VALID_SIGNATURE;

        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            Event mockEvent = mock(Event.class);
            when(mockEvent.getType()).thenReturn("payment_intent.succeeded");

            mockedWebhook.when(() -> Webhook.constructEvent(payload, signature, WEBHOOK_SECRET))
                    .thenReturn(mockEvent);

            
            doThrow(new RuntimeException("Database error"))
                    .when(stripePaymentService).handleWebhookEvent(mockEvent);

            
            ResponseEntity<String> response = webhookController.handleStripeWebhook(payload, signature);
            assertEquals(500, response.getStatusCode().value());

            verify(stripePaymentService, times(1)).handleWebhookEvent(any());
        }
    }

    @Test
    void testHandleStripeWebhook_EmptyPayload_ThrowsException() {
        
        String emptyPayload = "";
        String signature = VALID_SIGNATURE;

        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent(emptyPayload, signature, WEBHOOK_SECRET))
                    .thenThrow(new IllegalArgumentException("Empty payload"));

            
            ResponseEntity<String> response = webhookController.handleStripeWebhook(emptyPayload, signature);
            assertEquals(500, response.getStatusCode().value());

            verify(stripePaymentService, never()).handleWebhookEvent(any());
        }
    }

    @Test
    void testHandleStripeWebhook_NullPayload_ReturnsError() {
        
        String nullPayload = null;
        String signature = VALID_SIGNATURE;

        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent(nullPayload, signature, WEBHOOK_SECRET))
                    .thenThrow(new IllegalArgumentException("Null payload"));

            ResponseEntity<String> response = webhookController.handleStripeWebhook(nullPayload, signature);
            assertEquals(500, response.getStatusCode().value());

            verify(stripePaymentService, never()).handleWebhookEvent(any());
        }
    }
}

