package com.homegenie.userservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homegenie.userservice.dto.event.UserRegisteredEvent;
import com.homegenie.userservice.dto.event.UserUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class UserEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.user-events:user-events}")
    private String userEventsTopic;

    public void publishUserRegisteredEvent(UserRegisteredEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            log.info("Publishing UserRegisteredEvent: userId={}, email={}", 
                    event.getUserId(), event.getEmail());
            
            kafkaTemplate.send(userEventsTopic, event.getUserId().toString(), eventJson);
            
            log.info("UserRegisteredEvent published successfully");
        } catch (Exception e) {
            log.error("Failed to publish UserRegisteredEvent: {}", e.getMessage(), e);
        }
    }

    public void publishUserUpdatedEvent(UserUpdatedEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            log.info("Publishing UserUpdatedEvent: userId={}, updateType={}", 
                    event.getUserId(), event.getUpdateType());
            
            kafkaTemplate.send(userEventsTopic, event.getUserId().toString(), eventJson);
            
            log.info("UserUpdatedEvent published successfully");
        } catch (Exception e) {
            log.error("Failed to publish UserUpdatedEvent: {}", e.getMessage(), e);
        }
    }
}
