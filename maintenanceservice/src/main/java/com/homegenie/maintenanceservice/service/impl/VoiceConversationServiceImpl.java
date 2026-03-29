package com.homegenie.maintenanceservice.service.impl;


import com.homegenie.maintenanceservice.service.VoiceConversationService;
import com.homegenie.maintenanceservice.dto.ConversationContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class VoiceConversationServiceImpl implements VoiceConversationService {

    private final Map<String, ConversationContext> conversations = new ConcurrentHashMap<>();

    public void saveContext(String conversationId, ConversationContext context) {
        context.setLastUpdated(java.time.LocalDateTime.now());
        conversations.put(conversationId, context);
        log.info("Saved conversation context: {}", conversationId);
    }
    public ConversationContext getContext(String conversationId) {
        return conversations.get(conversationId);
    }

    public String generateConversationId(Long userId) {
        return userId + "_" + System.currentTimeMillis();
    }

    public void cleanupOldConversations() {
        java.time.LocalDateTime cutoff = java.time.LocalDateTime.now().minusHours(1);
        conversations.entrySet().removeIf(entry ->
                entry.getValue().getLastUpdated().isBefore(cutoff));
        log.info("Cleaned up old conversations. Remaining: {}", conversations.size());
    }

    public String buildContextString(ConversationContext context) {
        if (context == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Previous conversation context:\n");

        if (context.getLastIntent() != null) {
            sb.append("Last intent: ").append(context.getLastIntent()).append("\n");
        }

        if (context.getPartialRequest() != null) {
            sb.append("Partial request details:\n");
            if (context.getPartialRequest().getTitle() != null) {
                sb.append("- Title: ").append(context.getPartialRequest().getTitle()).append("\n");
            }
            if (context.getPartialRequest().getDescription() != null) {
                sb.append("- Description: ").append(context.getPartialRequest().getDescription()).append("\n");
            }
        }

        return sb.toString();
    }
}
