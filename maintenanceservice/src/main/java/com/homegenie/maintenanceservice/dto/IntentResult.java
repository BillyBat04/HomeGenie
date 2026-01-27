package com.homegenie.maintenanceservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntentResult {
    private VoiceIntent intent;
    private Double confidence;
    private MaintenanceRequestDTO extractedData;  // Extracted maintenance data
    private Long ticketId;  // For status queries
    private boolean isEmergency;
    private String additionalInfo;
}
