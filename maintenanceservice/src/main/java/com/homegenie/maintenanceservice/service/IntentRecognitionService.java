package com.homegenie.maintenanceservice.service;

import com.homegenie.maintenanceservice.dto.AIProcessingRequest;
import com.homegenie.maintenanceservice.dto.AIProcessingResponse;
import com.homegenie.maintenanceservice.dto.IntentResult;
import com.homegenie.maintenanceservice.dto.VoiceIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class IntentRecognitionService {

    private final WebClient webClient;

    @Value("${voice.service.url}")
    private String voiceServiceUrl;

    @Value("${voice.assistant.timeout:30000}")
    private int timeout;

    /**
     * Recognize user intent from transcribed text using AI
     */
    public Mono<IntentResult> recognizeIntent(String text, Long userId, String context) {
        log.info("Recognizing intent for user {}: {}", userId, text);

        AIProcessingRequest request = new AIProcessingRequest();
        request.setQuery(text);
        request.setUserId(userId);
        request.setContext(context);

        return webClient.post()
                .uri(voiceServiceUrl + "/api/recognize-intent")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AIProcessingResponse.class)
                .timeout(Duration.ofMillis(timeout))
                .map(AIProcessingResponse::getIntent)
                .doOnSuccess(intent -> log.info("Intent recognized: {}", intent.getIntent()))
                .doOnError(error -> log.error("Intent recognition failed", error))
                .onErrorReturn(createUnknownIntent());
    }

    private IntentResult createUnknownIntent() {
        IntentResult result = new IntentResult();
        result.setIntent(VoiceIntent.UNKNOWN);
        result.setConfidence(0.0);
        result.setEmergency(false);
        return result;
    }
}
