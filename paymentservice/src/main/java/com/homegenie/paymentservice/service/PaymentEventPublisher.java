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


@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.payment-events:payment-events}")
    private String paymentEventsTopic;

    
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
            
            
            log.error(" Failed to publish {}: {}. Payment {} was already committed — no rollback.", 
                    eventType, e.getMessage(), payment.getId());
        }
    }
}
