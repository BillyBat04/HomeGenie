package com.homegenie.maintenanceservice.service;

import com.homegenie.maintenanceservice.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoiceProcessingService {

    private final WebClient webClient;

    @Value("${voice.service.url}")
    private String voiceServiceUrl;

    @Value("${voice.assistant.timeout:30000}")
    private int timeout;

    /**
     * Convert speech audio file to text using Python service
     */
    public Mono<SpeechToTextResponse> speechToText(MultipartFile audioFile) {
        log.info("Converting speech to text for file: {}", audioFile.getOriginalFilename());

        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("audio", new ByteArrayResource(audioFile.getBytes()) {
                @Override
                public String getFilename() {
                    return audioFile.getOriginalFilename();
                }
            }, MediaType.APPLICATION_OCTET_STREAM);

            return webClient.post()
                    .uri(voiceServiceUrl + "/api/speech-to-text")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(SpeechToTextResponse.class)
                    .timeout(Duration.ofMillis(timeout))
                    .doOnSuccess(response -> log.info("STT successful: {}", response.getText()))
                    .doOnError(error -> log.error("STT failed", error))
                    .onErrorReturn(createErrorSTTResponse());

        } catch (Exception e) {
            log.error("Error preparing STT request", e);
            return Mono.just(createErrorSTTResponse());
        }
    }

    /**
     * Convert text to speech audio using Python service
     */
    public Mono<TextToSpeechResponse> textToSpeech(String text) {
        log.info("Converting text to speech: {}", text.substring(0, Math.min(50, text.length())));

        Map<String, String> request = new HashMap<>();
        request.put("text", text);

        return webClient.post()
                .uri(voiceServiceUrl + "/api/text-to-speech")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(TextToSpeechResponse.class)
                .timeout(Duration.ofMillis(timeout))
                .doOnSuccess(response -> log.info("TTS successful"))
                .doOnError(error -> log.error("TTS failed", error))
                .onErrorReturn(createErrorTTSResponse());
    }

    private SpeechToTextResponse createErrorSTTResponse() {
        SpeechToTextResponse response = new SpeechToTextResponse();
        response.setSuccess(false);
        response.setError("Speech recognition service unavailable");
        return response;
    }

    private TextToSpeechResponse createErrorTTSResponse() {
        TextToSpeechResponse response = new TextToSpeechResponse();
        response.setSuccess(false);
        response.setError("Text-to-speech service unavailable");
        return response;
    }
}
