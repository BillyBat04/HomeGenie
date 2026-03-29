package com.homegenie.maintenanceservice.service;

import com.homegenie.maintenanceservice.dto.SpeechToTextResponse;
import com.homegenie.maintenanceservice.dto.TextToSpeechResponse;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

public interface VoiceProcessingService {

    Mono<SpeechToTextResponse> speechToText(MultipartFile audioFile);

    Mono<TextToSpeechResponse> textToSpeech(String text);
}
