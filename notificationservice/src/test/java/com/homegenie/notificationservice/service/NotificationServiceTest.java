package com.homegenie.notificationservice.service;

import com.homegenie.notificationservice.dto.*;
import com.homegenie.notificationservice.model.Notification;
import com.homegenie.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for NotificationService
 *
 * Test Coverage:
 * - Payment confirmation notification
 * - Payment failed notification
 * - Refund processed notification
 * - Invoice sent notification
 * - Invoice overdue notification
 * - Invoice paid notification
 * - Email sending success/failure
 * - Notification retry logic
 * - User details fetching
 * - Notification status updates
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private NotificationService notificationService;

    private PaymentEvent paymentEvent;
    private InvoiceEvent invoiceEvent;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationService, "userServiceUrl", "http://localhost:8081");
        ReflectionTestUtils.setField(notificationService, "maxRetryAttempts", 3);

        paymentEvent = new PaymentEvent();
        paymentEvent.setPaymentId(1L);
        paymentEvent.setUserId(1L);
        paymentEvent.setRequestId(1L);
        paymentEvent.setAmount(new BigDecimal("100.00"));
        paymentEvent.setCurrency("USD");
        paymentEvent.setPaymentMethod("CREDIT_CARD");
        paymentEvent.setStatus("SUCCEEDED");
        paymentEvent.setReceiptUrl("https://stripe.com/receipt");

        invoiceEvent = new InvoiceEvent();
        invoiceEvent.setInvoiceId(1L);
        invoiceEvent.setUserId(1L);
        invoiceEvent.setRequestId(1L);
        invoiceEvent.setInvoiceNumber("INV-2024-001");
        invoiceEvent.setTotalAmount(new BigDecimal("165.00"));
        invoiceEvent.setDueDate(LocalDateTime.now().plusDays(7));

        userResponse = new UserResponse();
        userResponse.setId(1L);
        userResponse.setEmail("user@example.com");
        userResponse.setFullName("Test User");
    }

    @Test
    void testSendPaymentConfirmation_Success() {
        // Given
        when(restTemplate.getForObject(anyString(), eq(UserResponse.class)))
                .thenReturn(userResponse);
        when(emailService.buildPaymentConfirmationEmail(anyString(), any(), anyString(),
                anyString(), anyLong(), anyString()))
                .thenReturn("<html>Payment Confirmation Email</html>");
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(emailService).sendEmail(anyString(), anyString(), anyString());

        // When
        notificationService.sendPaymentConfirmation(paymentEvent);

        // Then
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, atLeastOnce()).save(notificationCaptor.capture());

        List<Notification> savedNotifications = notificationCaptor.getAllValues();
        assertTrue(savedNotifications.size() >= 1);

        Notification savedNotification = savedNotifications.get(0);
        assertEquals(Notification.NotificationType.PAYMENT_CONFIRMATION, savedNotification.getType());
        assertEquals(userResponse.getEmail(), savedNotification.getRecipient());

        verify(emailService, times(1)).sendEmail(
                eq(userResponse.getEmail()),
                contains("Payment Confirmation"),
                anyString()
        );
    }

    @Test
    void testSendPaymentFailed_Success() {
        // Given
        paymentEvent.setStatus("FAILED");
        paymentEvent.setFailureReason("Insufficient funds");

        when(restTemplate.getForObject(anyString(), eq(UserResponse.class)))
                .thenReturn(userResponse);
        when(emailService.buildPaymentFailedEmail(anyString(), any(), anyString(),
                anyString(), anyLong()))
                .thenReturn("<html>Payment Failed Email</html>");
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(emailService).sendEmail(anyString(), anyString(), anyString());

        // When
        notificationService.sendPaymentFailed(paymentEvent);

        // Then
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, atLeastOnce()).save(notificationCaptor.capture());

        Notification savedNotification = notificationCaptor.getAllValues().get(0);
        assertEquals(Notification.NotificationType.PAYMENT_FAILED, savedNotification.getType());
        assertEquals(userResponse.getEmail(), savedNotification.getRecipient());
    }

    @Test
    void testSendRefundProcessed_Success() {
        // Given
        when(restTemplate.getForObject(anyString(), eq(UserResponse.class)))
                .thenReturn(userResponse);
        when(emailService.buildRefundProcessedEmail(anyString(), any(), anyString(),
                anyLong(), anyString()))
                .thenReturn("<html>Refund Processed Email</html>");
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(emailService).sendEmail(anyString(), anyString(), anyString());

        // When
        notificationService.sendRefundProcessed(paymentEvent);

        // Then
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, atLeastOnce()).save(notificationCaptor.capture());

        Notification savedNotification = notificationCaptor.getAllValues().get(0);
        assertEquals(Notification.NotificationType.PAYMENT_REFUNDED, savedNotification.getType());
    }

    @Test
    void testSendInvoiceSent_Success() {
        // Given
        when(restTemplate.getForObject(anyString(), eq(UserResponse.class)))
                .thenReturn(userResponse);
        when(emailService.buildInvoiceSentEmail(anyString(), anyString(), any(),
                anyString(), any(), anyLong()))
                .thenReturn("<html>Invoice Sent Email</html>");
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(emailService).sendEmail(anyString(), anyString(), anyString());

        // When
        notificationService.sendInvoiceSent(invoiceEvent);

        // Then
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, atLeastOnce()).save(notificationCaptor.capture());

        Notification savedNotification = notificationCaptor.getAllValues().get(0);
        assertEquals(Notification.NotificationType.INVOICE_SENT, savedNotification.getType());
        assertTrue(savedNotification.getSubject().contains(invoiceEvent.getInvoiceNumber()));
    }

    @Test
    void testSendInvoiceOverdue_Success() {
        // Given
        invoiceEvent.setLateFee(new BigDecimal("15.00"));

        when(restTemplate.getForObject(anyString(), eq(UserResponse.class)))
                .thenReturn(userResponse);
        when(emailService.buildInvoiceOverdueEmail(anyString(), anyString(), any(),
                anyString(), any(), anyLong()))
                .thenReturn("<html>Invoice Overdue Email</html>");
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(emailService).sendEmail(anyString(), anyString(), anyString());

        // When
        notificationService.sendInvoiceOverdue(invoiceEvent);

        // Then
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, atLeastOnce()).save(notificationCaptor.capture());

        Notification savedNotification = notificationCaptor.getAllValues().get(0);
        assertEquals(Notification.NotificationType.INVOICE_OVERDUE, savedNotification.getType());
    }

    @Test
    void testSendInvoicePaid_Success() {
        // Given
        invoiceEvent.setPaymentId(1L);
        invoiceEvent.setPaidDate(LocalDateTime.now());

        when(restTemplate.getForObject(anyString(), eq(UserResponse.class)))
                .thenReturn(userResponse);
        when(emailService.buildInvoicePaidEmail(anyString(), anyString(), any(),
                anyString(), any(), anyLong()))
                .thenReturn("<html>Invoice Paid Email</html>");
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(emailService).sendEmail(anyString(), anyString(), anyString());

        // When
        notificationService.sendInvoicePaid(invoiceEvent);

        // Then
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, atLeastOnce()).save(notificationCaptor.capture());

        Notification savedNotification = notificationCaptor.getAllValues().get(0);
        assertEquals(Notification.NotificationType.INVOICE_PAID, savedNotification.getType());
    }

    @Test
    void testSendPaymentConfirmation_EmailServiceFails_NotificationMarkedFailed() {
        // Given
        when(restTemplate.getForObject(anyString(), eq(UserResponse.class)))
                .thenReturn(userResponse);
        when(emailService.buildPaymentConfirmationEmail(anyString(), any(), anyString(),
                anyString(), anyLong(), anyString()))
                .thenReturn("<html>Email</html>");
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Email service throws exception
        doThrow(new RuntimeException("SMTP connection failed"))
                .when(emailService).sendEmail(anyString(), anyString(), anyString());

        // When
        notificationService.sendPaymentConfirmation(paymentEvent);

        // Then
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, atLeastOnce()).save(notificationCaptor.capture());

        // Should have saved at least twice (PENDING, then FAILED)
        List<Notification> savedNotifications = notificationCaptor.getAllValues();
        assertTrue(savedNotifications.size() >= 2);

        // Last save should be FAILED status
        Notification finalNotification = savedNotifications.get(savedNotifications.size() - 1);
        assertEquals(Notification.NotificationStatus.FAILED, finalNotification.getStatus());
    }

    @Test
    void testSendPaymentConfirmation_UserNotFound_UsesDefaultUser() {
        // Given
        when(restTemplate.getForObject(anyString(), eq(UserResponse.class)))
                .thenThrow(new RuntimeException("User not found"));

        // Should not throw exception - continue with default values
        when(emailService.buildPaymentConfirmationEmail(anyString(), any(), anyString(),
                anyString(), anyLong(), anyString()))
                .thenReturn("<html>Email</html>");
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When - Should not throw exception
        assertDoesNotThrow(() -> {
            notificationService.sendPaymentConfirmation(paymentEvent);
        });
    }

    @Test
    void testSendPaymentConfirmation_NullPaymentEvent_HandledGracefully() {
        // Given
        PaymentEvent nullEvent = null;

        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> {
            notificationService.sendPaymentConfirmation(nullEvent);
        });

        verify(notificationRepository, never()).save(any());
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void testSendInvoiceSent_NullInvoiceEvent_HandledGracefully() {
        // Given
        InvoiceEvent nullEvent = null;

        // When & Then
        assertDoesNotThrow(() -> {
            notificationService.sendInvoiceSent(nullEvent);
        });

        verify(notificationRepository, never()).save(any());
    }
}

