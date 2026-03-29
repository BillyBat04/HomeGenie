package com.homegenie.maintenanceservice.service;

import com.homegenie.maintenanceservice.dto.IntentResult;
import reactor.core.publisher.Mono;

public interface IntentRecognitionService {

    Mono<IntentResult> recognizeIntent(String text, Long userId, String context);
}
