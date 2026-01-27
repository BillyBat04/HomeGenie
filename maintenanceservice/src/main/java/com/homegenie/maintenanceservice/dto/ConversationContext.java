package com.homegenie.maintenanceservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationContext {
    private String conversationId;
    private Long userId;
    private String lastIntent;
    private MaintenanceRequestDTO partialRequest;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime lastUpdated;
}
