package com.homegenie.maintenanceservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpeechToTextResponse {
    private String text;
    private Boolean success;
    private Double confidence;
    private String error;
}

