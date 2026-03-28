package com.homegenie.maintenanceservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homegenie.maintenanceservice.dto.event.MaintenanceAssignedEvent;
import com.homegenie.maintenanceservice.dto.event.MaintenanceCompletedEvent;
import com.homegenie.maintenanceservice.dto.event.MaintenanceCreatedEvent;
import com.homegenie.maintenanceservice.dto.event.MaintenanceReminderEvent;
import com.homegenie.maintenanceservice.dto.event.MaintenanceStatusChangedEvent;
import com.homegenie.maintenanceservice.dto.event.WarrantyExpiringEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class MaintenanceEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.maintenance-events:maintenance-events}")
    private String maintenanceEventsTopic;

    
    
    @Value("${kafka.topics.maintenance-reminder:maintenance-reminder}")
    private String maintenanceReminderTopic;

    @Value("${kafka.topics.warranty-reminder:warranty-reminder}")
    private String warrantyReminderTopic;

    
    public void publishMaintenanceCreatedEvent(MaintenanceCreatedEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            log.info("📤 Publishing MaintenanceCreatedEvent: requestId={}, title={}", 
                    event.getRequestId(), event.getTitle());
            
            
            kafkaTemplate.send(maintenanceEventsTopic, event.getRequestId().toString(), eventJson);
            
            log.info("✅ MaintenanceCreatedEvent published successfully");
        } catch (Exception e) {
            log.error("❌ Failed to publish MaintenanceCreatedEvent: {}", e.getMessage(), e);
        }
    }

    
    public void publishMaintenanceAssignedEvent(MaintenanceAssignedEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            log.info("📤 Publishing MaintenanceAssignedEvent: requestId={}, technicianId={}", 
                    event.getRequestId(), event.getTechnicianId());
            
            kafkaTemplate.send(maintenanceEventsTopic, event.getRequestId().toString(), eventJson);
            
            log.info("✅ MaintenanceAssignedEvent published successfully");
        } catch (Exception e) {
            log.error("❌ Failed to publish MaintenanceAssignedEvent: {}", e.getMessage(), e);
        }
    }

    
    public void publishMaintenanceStatusChangedEvent(MaintenanceStatusChangedEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            log.info("📤 Publishing MaintenanceStatusChangedEvent: requestId={}, {} → {}", 
                    event.getRequestId(), event.getOldStatus(), event.getNewStatus());
            
            kafkaTemplate.send(maintenanceEventsTopic, event.getRequestId().toString(), eventJson);
            
            log.info("✅ MaintenanceStatusChangedEvent published successfully");
        } catch (Exception e) {
            log.error("❌ Failed to publish MaintenanceStatusChangedEvent: {}", e.getMessage(), e);
        }
    }

    
    public void publishMaintenanceCompletedEvent(MaintenanceCompletedEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            log.info("📤 Publishing MaintenanceCompletedEvent: requestId={}, itemId={}", 
                    event.getRequestId(), event.getItemId());
            
            
            String partitionKey = event.getItemId() != null ? event.getItemId().toString() : event.getRequestId().toString();
            kafkaTemplate.send(maintenanceEventsTopic, partitionKey, eventJson);
            
            log.info("✅ MaintenanceCompletedEvent published successfully");
        } catch (Exception e) {
            log.error("❌ Failed to publish MaintenanceCompletedEvent: {}", e.getMessage(), e);
        }
    }

    
    public void publishMaintenanceReminderEvent(MaintenanceReminderEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            log.info("📤 Publishing MaintenanceReminderEvent: itemId={}, urgency={}", 
                    event.getItemId(), event.getUrgencyLevel());
            
            
            kafkaTemplate.send(maintenanceReminderTopic, event.getItemId().toString(), eventJson);
            
            log.info("✅ MaintenanceReminderEvent published successfully");
        } catch (Exception e) {
            log.error("❌ Failed to publish MaintenanceReminderEvent: {}", e.getMessage(), e);
        }
    }

    
    public void publishWarrantyExpiringEvent(WarrantyExpiringEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            log.info("📤 Publishing WarrantyExpiringEvent: itemId={}, urgency={}", 
                    event.getItemId(), event.getUrgencyLevel());
            
            
            kafkaTemplate.send(warrantyReminderTopic, event.getItemId().toString(), eventJson);
            
            log.info("✅ WarrantyExpiringEvent published successfully");
        } catch (Exception e) {
            log.error("❌ Failed to publish WarrantyExpiringEvent: {}", e.getMessage(), e);
        }
    }
}
