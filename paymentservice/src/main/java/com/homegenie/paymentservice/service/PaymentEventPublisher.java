package com.homegenie.paymentservice.service;

import com.homegenie.paymentservice.model.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Publishes payment lifecycle events to Kafka so notification-service
 * can send confirmation/failure emails to the user.
 *
 * Topic: payment-events (consumed by notification-service group)
 * Also watched by KEDA to auto-scale notification-service pods.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.payment-events:payment-events}")
    private String paymentEventsTopic;

    /**
     * Called after a payment is successfully created and saved.
     * partitionKey = userId → events for the same user land on the same partition,
     * preserving order (e.g. PAYMENT_CONFIRMED always after PAYMENT_FAILED for a user).
     */
    public void publishPaymentConfirmed(Payment payment) {
        publish("PAYMENT_CONFIRMED", payment, null);
    }

    public void publishPaymentFailed(Payment payment, String failureReason) {
        publish("PAYMENT_FAILED", payment, failureReason);
    }

    public void publishPaymentRefunded(Payment payment) {
        publish("PAYMENT_REFUNDED", payment, null);
    }

    private void publish(String eventType, Payment payment, String failureReason) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("paymentId", payment.getId());
            event.put("userId", payment.getUserId());
            event.put("requestId", payment.getOrderId());
            event.put("amount", payment.getAmount());
            event.put("currency", payment.getCurrency());
            event.put("status", payment.getStatus() != null ? payment.getStatus().name() : null);
            event.put("paymentMethod", payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : null);
            event.put("failureReason", failureReason);
            event.put("eventType", eventType);
            event.put("eventTime", LocalDateTime.now().toString());

            String partitionKey = payment.getUserId().toString();
            String topic = Objects.requireNonNull(paymentEventsTopic, "paymentEventsTopic");
            String key = Objects.requireNonNull(partitionKey, "partitionKey");
            log.info("📤 Publishing {}: paymentId={}, userId={}", eventType, payment.getId(), payment.getUserId());
            kafkaTemplate.send(topic, key, event);
            log.info(" {} published successfully", eventType);
        } catch (Exception e) {
            // Log but do not throw — a Kafka failure must NOT roll back an already-committed Stripe payment.
            // The notification is best-effort; the payment itself was successful.
            log.error(" Failed to publish {}: {}. Payment {} was already committed — no rollback.", 
                    eventType, e.getMessage(), payment.getId());
        }
    }
}
