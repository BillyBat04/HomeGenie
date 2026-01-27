package com.homegenie.maintenanceservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
class VoiceAssistantStats {
    private Long totalVoiceInteractions;
    private Long successfulCreations;
    private Long emergencyDetections;
    private Double averageConfidence;
    private Long activeConversations;
}
