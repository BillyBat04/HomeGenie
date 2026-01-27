package com.homegenie.maintenanceservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIProcessingResponse {
    private String response;
    private IntentResult intent;
    private boolean success;
    private String error;
}
