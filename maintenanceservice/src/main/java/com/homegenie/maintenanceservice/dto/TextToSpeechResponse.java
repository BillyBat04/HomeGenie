package com.homegenie.maintenanceservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TextToSpeechResponse {
    private String audioBase64;
    private Boolean success;
    private String error;
    private String format;
}
