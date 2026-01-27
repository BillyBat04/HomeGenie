package com.homegenie.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class AlertService {

    private final RestTemplate restTemplate;
    
    @Value("${platform.alerts.slack.webhook-url:}")
    private String slackWebhookUrl;
    
    @Value("${platform.alerts.slack.enabled:false}")
    private boolean slackEnabled;
    
    @Value("${platform.alerts.email.enabled:false}")
    private boolean emailEnabled;
    
    @Value("${platform.alerts.email.recipients:}")
    private String emailRecipients;

    public AlertService() {
        this.restTemplate = new RestTemplate();
    }

        public void info(String title, String message) {
        sendAlert(AlertLevel.INFO, title, message);
    }

        public void warning(String title, String message) {
        sendAlert(AlertLevel.WARNING, title, message);
    }

        public void critical(String title, String message) {
        sendAlert(AlertLevel.CRITICAL, title, message);
    }

        private void sendAlert(AlertLevel level, String title, String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String emoji = getEmojiForLevel(level);
        
        
        String logMessage = String.format("[%s] %s %s - %s", timestamp, emoji, title, message);
        switch (level) {
            case INFO -> log.info(logMessage);
            case WARNING -> log.warn(logMessage);
            case CRITICAL -> log.error(logMessage);
        }
        
        
        if (slackEnabled && !slackWebhookUrl.isEmpty()) {
            sendSlackAlert(level, title, message, timestamp);
        }
        
        
        if (emailEnabled && level == AlertLevel.CRITICAL && !emailRecipients.isEmpty()) {
            sendEmailAlert(title, message, timestamp);
        }
    }

        private void sendSlackAlert(AlertLevel level, String title, String message, String timestamp) {
        try {
            String color = getColorForLevel(level);
            String emoji = getEmojiForLevel(level);
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("username", "Shadow Mode Monitor");
            payload.put("icon_emoji", ":ghost:");
            
            Map<String, Object> attachment = new HashMap<>();
            attachment.put("color", color);
            attachment.put("title", emoji + " " + title);
            attachment.put("text", message);
            attachment.put("footer", "Gateway Service");
            attachment.put("ts", System.currentTimeMillis() / 1000);
            
            Map<String, Object>[] attachments = new Map[]{attachment};
            payload.put("attachments", attachments);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            restTemplate.postForEntity(slackWebhookUrl, request, String.class);
            
            log.debug("Alert sent to Slack: {}", title);
        } catch (Exception e) {
            log.error("Failed to send Slack alert: {}", e.getMessage());
        }
    }

        private void sendEmailAlert(String title, String message, String timestamp) ách 
        log.info("Email alert would be sent to: {} - {}", emailRecipients, title);
    }

        private String getEmojiForLevel(AlertLevel level) {
        return switch (level) {
            case INFO -> "i";
            case WARNING -> "!";
            case CRITICAL -> "X";
        };
    }

        private String getColorForLevel(AlertLevel level) {
        return switch (level) {
            case INFO -> "#36a64f";      
            case WARNING -> "#ff9900";   
            case CRITICAL -> "#d00000";  
        };
    }

    public enum AlertLevel {
        INFO,
        WARNING,
        CRITICAL
    }
}
