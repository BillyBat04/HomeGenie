package com.homegenie.marketplaceservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Notification Platform API Request DTO
 * 
 * For sending emails via Notification Service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    private Long userId;
    private String miniAppId;
    private String type; // EMAIL, SMS, PUSH
    private String templateId;
    private String recipientEmail;
    private String subject;
    private String body;
    private Map<String, Object> templateData;
    private String priority; // HIGH, NORMAL, LOW
}
