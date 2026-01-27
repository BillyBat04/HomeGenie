package com.homegenie.notificationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homegenie.notificationservice.dto.InvoiceEvent;
import com.homegenie.notificationservice.dto.PaymentEvent;
import com.homegenie.notificationservice.dto.UserEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    /**
     * Listen to payment events from Payment Service
     */
    @KafkaListener(topics = "${kafka.topics.payment-events}", groupId = "notification-service")
    public void handlePaymentEvent(String message) {
        try {
            log.info("📨 Received payment event: {}", message);
            PaymentEvent event = objectMapper.readValue(message, PaymentEvent.class);
            
            switch (event.getEventType()) {
                case "PAYMENT_CONFIRMED":
                    notificationService.sendPaymentConfirmation(event);
                    break;
                case "PAYMENT_FAILED":
                    notificationService.sendPaymentFailed(event);
                    break;
                case "PAYMENT_REFUNDED":
                    notificationService.sendRefundProcessed(event);
                    break;
                default:
                    log.warn("Unknown payment event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("❌ Error processing payment event: {}", e.getMessage(), e);
        }
    }

    /**
     * Listen to invoice events from Payment Service
     */
    @KafkaListener(topics = "${kafka.topics.invoice-events}", groupId = "notification-service")
    public void handleInvoiceEvent(String message) {
        try {
            log.info("📨 Received invoice event: {}", message);
            InvoiceEvent event = objectMapper.readValue(message, InvoiceEvent.class);
            
            switch (event.getEventType()) {
                case "INVOICE_SENT":
                    notificationService.sendInvoiceSent(event);
                    break;
                case "INVOICE_OVERDUE":
                    notificationService.sendInvoiceOverdue(event);
                    break;
                case "INVOICE_PAID":
                    notificationService.sendInvoicePaid(event);
                    break;
                default:
                    log.warn("Unknown invoice event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("❌ Error processing invoice event: {}", e.getMessage(), e);
        }
    }

    /**
     * Listen to maintenance request events
     */
    @KafkaListener(topics = "${kafka.topics.maintenance-events}", groupId = "notification-service")
    public void handleMaintenanceEvent(String message) {
        try {
            log.info("📨 Received maintenance event: {}", message);
            // Handle maintenance events if needed
            // For now, maintenance service sends emails directly
        } catch (Exception e) {
            log.error("❌ Error processing maintenance event: {}", e.getMessage(), e);
        }
    }

    /**
     * Listen to user events from User Service
     */
    @KafkaListener(topics = "${kafka.topics.user-events}", groupId = "notification-service")
    public void handleUserEvent(String message) {
        try {
            log.info("📨 Received user event: {}", message);
            UserEvent event = objectMapper.readValue(message, UserEvent.class);
            
            switch (event.getEventType()) {
                case "USER_REGISTERED":
                    notificationService.sendWelcomeEmail(event);
                    break;
                case "USER_UPDATED":
                    log.info("User updated: userId={}, updateType={}", event.getUserId(), event.getUpdateType());
                    // Handle user updates if needed (e.g., password reset confirmation)
                    break;
                default:
                    log.warn("Unknown user event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("❌ Error processing user event: {}", e.getMessage(), e);
        }
    }
}
