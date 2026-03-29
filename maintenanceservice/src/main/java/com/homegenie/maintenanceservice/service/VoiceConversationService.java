package com.homegenie.maintenanceservice.service;

import com.homegenie.maintenanceservice.dto.ConversationContext;

public interface VoiceConversationService {

    void saveContext(String conversationId, ConversationContext context);

    ConversationContext getContext(String conversationId);

    String generateConversationId(Long userId);

    void cleanupOldConversations();

    String buildContextString(ConversationContext context);
}
