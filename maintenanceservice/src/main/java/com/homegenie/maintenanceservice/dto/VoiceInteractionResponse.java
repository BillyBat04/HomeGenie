package com.homegenie.maintenanceservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoiceInteractionResponse {
    private String textResponse;
    private String audioResponseBase64;
    private IntentResult intent;
    private MaintenanceResponseDTO createdTicket;
    private String conversationId;
    private boolean requiresFollowup;
    private String followupQuestion;
}