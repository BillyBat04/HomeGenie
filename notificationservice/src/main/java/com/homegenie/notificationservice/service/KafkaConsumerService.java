package com.homegenie.notificationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homegenie.notificationservice.dto.InvoiceEvent;
import com.homegenie.notificationservice.dto.MaintenanceReminderEvent;
import com.homegenie.notificationservice.dto.PaymentEvent;
import com.homegenie.notificationservice.dto.UserEvent;
import com.homegenie.notificationservice.dto.WarrantyExpiringEvent;
import com.homegenie.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
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
     * Maintenance reminder events from the dedicated 'maintenance-reminder' topic.
     * Published by ItemMaintenanceScheduler when items are due/overdue for service.
     * This topic is also watched by KEDA → pod count scales with reminder volume.
     */
    @KafkaListener(topics = "${kafka.topics.maintenance-reminder}", groupId = "notification-service")
    public void handleMaintenanceReminderEvent(String message) {
        try {
            log.info("📨 Received maintenance reminder event");
            MaintenanceReminderEvent event = objectMapper.readValue(message, MaintenanceReminderEvent.class);

            // Idempotency check: Kafka delivers messages at-least-once.
            // If the consumer pod crashes after processing but before committing the offset,
            // Kafka will re-deliver the same message. Without this guard, the email is sent twice.
            if (event.getEventId() != null && notificationRepository.existsByEventId(event.getEventId())) {
                log.warn("⚠️ Duplicate maintenance-reminder event ignored: eventId={}", event.getEventId());
                return;
            }

            notificationService.sendMaintenanceReminder(event);
        } catch (Exception e) {
            log.error("❌ Error processing maintenance reminder event: {}", e.getMessage(), e);
        }
    }

    /**
     * Warranty expiry events from the dedicated 'warranty-reminder' topic.
     * Published by ItemMaintenanceScheduler when item warranties are about to expire.
     * This topic is also watched by KEDA → pod count scales with reminder volume.
     */
    @KafkaListener(topics = "${kafka.topics.warranty-reminder}", groupId = "notification-service")
    public void handleWarrantyExpiringEvent(String message) {
        try {
            log.info("📨 Received warranty expiry event");
            WarrantyExpiringEvent event = objectMapper.readValue(message, WarrantyExpiringEvent.class);

            // Same idempotency guard as maintenance-reminder above.
            if (event.getEventId() != null && notificationRepository.existsByEventId(event.getEventId())) {
                log.warn("⚠️ Duplicate warranty-reminder event ignored: eventId={}", event.getEventId());
                return;
            }

            notificationService.sendWarrantyExpiryAlert(event);
        } catch (Exception e) {
            log.error("❌ Error processing warranty expiry event: {}", e.getMessage(), e);
        }
    }

    /**
     * Listen to maintenance request events (operational events, not reminders).
     * maintenance-events topic carries: Created, Assigned, StatusChanged, Completed.
     */
    @KafkaListener(topics = "${kafka.topics.maintenance-events}", groupId = "notification-service")
    public void handleMaintenanceEvent(String message) {
        try {
            log.info("📨 Received maintenance operational event");
            // maintenance-events is a fan-out topic with multiple event types.
            // Currently only status changes visible to users need a notification;
            // other event types (Created, Assigned) handled by maintenance-service directly.
            // TODO: deserialize by eventType field and route to appropriate notificationService methods.
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
