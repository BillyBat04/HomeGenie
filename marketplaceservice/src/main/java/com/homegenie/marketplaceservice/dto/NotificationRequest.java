package com.homegenie.marketplaceservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    private Long userId;
    private String miniAppId;
    private String type; 
    private String templateId;
    private String recipientEmail;
    private String subject;
    private String body;
    private Map<String, Object> templateData;
    private String priority; 
}
