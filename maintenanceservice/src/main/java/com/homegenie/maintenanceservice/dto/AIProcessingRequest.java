package com.homegenie.maintenanceservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIProcessingRequest {
    private String query;
    private String conversationHistory;
    private Long userId;
    private String context;
}
