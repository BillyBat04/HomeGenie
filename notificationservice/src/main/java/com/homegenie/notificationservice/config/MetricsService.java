package com.homegenie.notificationservice.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MetricsService {

    private final MeterRegistry registry;

    
    private Counter emailSentCounter;
    private Counter emailFailedCounter;
    private Timer emailDeliveryTimer;

    
    private Counter kafkaEventReceivedCounter;
    private Counter kafkaEventProcessedCounter;
    private Counter kafkaEventFailedCounter;
    private Timer kafkaEventProcessingTimer;

    
    private Counter paymentNotificationCounter;
    private Counter invoiceNotificationCounter;
    private Counter maintenanceNotificationCounter;
    private Counter userNotificationCounter;

    @PostConstruct
    public void init() {
        log.info("Initializing Notification Service custom metrics...");

        
        emailSentCounter = Counter.builder("notification_email_sent_total")
                .description("Total number of emails sent successfully")
                .tag("service", "notification-service")
                .tag("channel", "email")
                .tag("status", "success")
                .register(registry);

        emailFailedCounter = Counter.builder("notification_email_sent_total")
                .description("Total number of emails failed to send")
                .tag("service", "notification-service")
                .tag("channel", "email")
                .tag("status", "failed")
                .register(registry);

        emailDeliveryTimer = Timer.builder("notification_email_delivery_duration_seconds")
                .description("Time taken to send an email")
                .tag("service", "notification-service")
                .tag("channel", "email")
                .register(registry);

        
        kafkaEventReceivedCounter = Counter.builder("notification_kafka_events_total")
                .description("Total number of Kafka events received")
                .tag("service", "notification-service")
                .tag("source", "kafka")
                .tag("status", "received")
                .register(registry);

        kafkaEventProcessedCounter = Counter.builder("notification_kafka_events_total")
                .description("Total number of Kafka events processed successfully")
                .tag("service", "notification-service")
                .tag("source", "kafka")
                .tag("status", "processed")
                .register(registry);

        kafkaEventFailedCounter = Counter.builder("notification_kafka_events_total")
                .description("Total number of Kafka events failed to process")
                .tag("service", "notification-service")
                .tag("source", "kafka")
                .tag("status", "failed")
                .register(registry);

        kafkaEventProcessingTimer = Timer.builder("notification_kafka_processing_duration_seconds")
                .description("Time taken to process a Kafka event")
                .tag("service", "notification-service")
                .tag("source", "kafka")
                .register(registry);

        
        paymentNotificationCounter = Counter.builder("notification_by_type_total")
                .description("Total notifications by type")
                .tag("service", "notification-service")
                .tag("type", "payment")
                .register(registry);

        invoiceNotificationCounter = Counter.builder("notification_by_type_total")
                .description("Total notifications by type")
                .tag("service", "notification-service")
                .tag("type", "invoice")
                .register(registry);

        maintenanceNotificationCounter = Counter.builder("notification_by_type_total")
                .description("Total notifications by type")
                .tag("service", "notification-service")
                .tag("type", "maintenance")
                .register(registry);

        userNotificationCounter = Counter.builder("notification_by_type_total")
                .description("Total notifications by type")
                .tag("service", "notification-service")
                .tag("type", "user")
                .register(registry);

        log.info("Notification Service custom metrics initialized successfully");
    }

    
    public void recordEmailSent() {
        emailSentCounter.increment();
    }

    public void recordEmailFailed() {
        emailFailedCounter.increment();
    }

    public void recordEmailDeliveryTime(Runnable operation) {
        emailDeliveryTimer.record(operation);
    }

    
    public void recordKafkaEventReceived() {
        kafkaEventReceivedCounter.increment();
    }

    public void recordKafkaEventProcessed() {
        kafkaEventProcessedCounter.increment();
    }

    public void recordKafkaEventFailed() {
        kafkaEventFailedCounter.increment();
    }

    public void recordKafkaEventProcessingTime(Runnable operation) {
        kafkaEventProcessingTimer.record(operation);
    }

    
    public void recordPaymentNotification() {
        paymentNotificationCounter.increment();
    }

    public void recordInvoiceNotification() {
        invoiceNotificationCounter.increment();
    }

    public void recordMaintenanceNotification() {
        maintenanceNotificationCounter.increment();
    }

    public void recordUserNotification() {
        userNotificationCounter.increment();
    }
}
