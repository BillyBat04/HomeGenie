package com.homegenie.maintenanceservice.controller;

import com.homegenie.maintenanceservice.dto.*;
import com.homegenie.maintenanceservice.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/maintenance/voice")
@RequiredArgsConstructor
@Slf4j
public class VoiceAssistantController {

    private final VoiceProcessingService voiceProcessingService;
    private final IntentRecognitionService intentRecognitionService;
    private final VoiceConversationService conversationService;
    private final MaintenanceService maintenanceService;

    /**
     * Main voice interaction endpoint
     * User uploads audio file -> AI processes -> Returns text + audio response
     */
    @PostMapping(value = "/interact", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<VoiceInteractionResponse>> voiceInteraction(
            @RequestPart("audio") MultipartFile audioFile,
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(value = "conversationId", required = false) String conversationId) {

        log.info("Voice interaction received from user: {}", userId);

        // Generate conversation ID if not provided
        if (conversationId == null) {
            conversationId = conversationService.generateConversationId(userId);
        }

        final String finalConversationId = conversationId;

        return voiceProcessingService.speechToText(audioFile)
                .flatMap(sttResponse -> {
                    if (!sttResponse.getSuccess()) {
                        return Mono.just(createErrorResponse(
                                "I couldn't understand the audio. Please try again.",
                                finalConversationId));
                    }

                    log.info("Transcribed text: {}", sttResponse.getText());

                    return processTextQuery(sttResponse.getText(), userId, finalConversationId);
                })
                .map(ResponseEntity::ok);
    }

    /**
     * Text-based voice assistant interaction (no audio upload)
     * Useful for testing or text-only interfaces
     */
    @PostMapping("/interact-text")
    public Mono<ResponseEntity<VoiceInteractionResponse>> textInteraction(
            @RequestBody VoiceInteractionRequest request,
            @RequestHeader("X-User-Id") Long userId) {

        log.info("Text interaction received from user: {}", userId);

        String conversationId = request.getConversationId();
        if (conversationId == null) {
            conversationId = conversationService.generateConversationId(userId);
        }

        return processTextQuery(request.getTranscribedText(), userId, conversationId)
                .map(ResponseEntity::ok);
    }

    /**
     * Get conversation history/context
     */
    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<ConversationContext> getConversation(
            @PathVariable String conversationId) {
        ConversationContext context = conversationService.getContext(conversationId);
        if (context != null) {
            return ResponseEntity.ok(context);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Core processing logic for voice/text queries
     */
    private Mono<VoiceInteractionResponse> processTextQuery(
            String text, Long userId, String conversationId) {

        // Get conversation context
        ConversationContext context = conversationService.getContext(conversationId);
        String contextString = conversationService.buildContextString(context);

        // Step 1: Recognize intent
        return intentRecognitionService.recognizeIntent(text, userId, contextString)
                .flatMap(intent -> {
                    log.info("Recognized intent: {} with confidence: {}",
                            intent.getIntent(), intent.getConfidence());

                    // Step 2: Handle based on intent
                    return handleIntent(intent, userId, conversationId, text);
                })
                .flatMap(response -> {
                    // Step 3: Convert response to speech
                    return voiceProcessingService.textToSpeech(response.getTextResponse())
                            .map(tts -> {
                                if (tts.getSuccess()) {
                                    response.setAudioResponseBase64(tts.getAudioBase64());
                                }
                                return response;
                            });
                })
                .doOnError(error -> log.error("Error processing voice query", error))
                .onErrorReturn(createErrorResponse(
                        "I'm having trouble processing your request. Please try again.",
                        conversationId));
    }

    /**
     * Handle different intents
     */
    private Mono<VoiceInteractionResponse> handleIntent(
            IntentResult intent, Long userId, String conversationId, String originalText) {

        switch (intent.getIntent()) {
            case CREATE_MAINTENANCE_REQUEST:
                return handleCreateMaintenanceRequest(intent, userId, conversationId);

            case QUERY_STATUS:
                return handleStatusQuery(intent, userId, conversationId);

            case LIST_MY_REQUESTS:
                return handleListRequests(userId, conversationId);

            case EMERGENCY:
                return handleEmergency(intent, userId, conversationId);

            case GENERAL_INQUIRY:
                return handleGeneralInquiry(originalText, conversationId);

            default:
                return Mono.just(createResponse(
                        "I'm not sure I understood that. Could you please rephrase? " +
                                "You can report maintenance issues, check status, or list your requests.",
                        conversationId, intent));
        }
    }

    /**
     * Handle maintenance request creation
     */
    private Mono<VoiceInteractionResponse> handleCreateMaintenanceRequest(
            IntentResult intent, Long userId, String conversationId) {

        MaintenanceRequestDTO dto = intent.getExtractedData();

        if (dto == null || dto.getTitle() == null || dto.getDescription() == null) {
            ConversationContext context = new ConversationContext();
            context.setConversationId(conversationId);
            context.setUserId(userId);
            context.setLastIntent(VoiceIntent.CREATE_MAINTENANCE_REQUEST.name());
            context.setPartialRequest(dto != null ? dto : new MaintenanceRequestDTO());
            context.setCreatedAt(java.time.LocalDateTime.now());
            conversationService.saveContext(conversationId, context);

            VoiceInteractionResponse response = new VoiceInteractionResponse();
            response.setTextResponse(
                    "I'd be happy to help you create a maintenance request. " +
                            "Could you please describe the issue you're experiencing?");
            response.setConversationId(conversationId);
            response.setRequiresFollowup(true);
            response.setIntent(intent);

            return Mono.just(response);
        }

        // Create the maintenance request
        try {
            MaintenanceResponseDTO created = maintenanceService.createRequest(userId, dto);

            VoiceInteractionResponse response = new VoiceInteractionResponse();
            response.setCreatedTicket(created);
            response.setConversationId(conversationId);
            response.setIntent(intent);

            String message = String.format(
                    "I've created your maintenance request successfully. " +
                            "Ticket number %d for %s has been submitted with %s priority. " +
                            "You'll be notified when a technician is assigned. Is there anything else I can help you with?",
                    created.getId(),
                    created.getCategory().toString().toLowerCase().replace("_", " "),
                    created.getPriority().toString().toLowerCase());

            response.setTextResponse(message);

            return Mono.just(response);

        } catch (Exception e) {
            log.error("Failed to create maintenance request", e);
            return Mono.just(createErrorResponse(
                    "I encountered an error creating your request. Please try again or contact support.",
                    conversationId));
        }
    }

    /**
     * Handle status query
     */
    private Mono<VoiceInteractionResponse> handleStatusQuery(
            IntentResult intent, Long userId, String conversationId) {

        try {
            if (intent.getTicketId() != null) {
                // Query specific ticket
                MaintenanceResponseDTO ticket = maintenanceService.getRequestById(intent.getTicketId());

                String message = String.format(
                        "Your request #%d for %s is currently %s. " +
                                "It was created on %s. " +
                                "%s",
                        ticket.getId(),
                        ticket.getTitle(),
                        ticket.getStatus().toString().toLowerCase().replace("_", " "),
                        ticket.getCreatedAt().toLocalDate(),
                        ticket.getAssignedTo() != null
                                ? "A technician has been assigned and will contact you soon."
                                : "We're working on assigning a technician.");

                return Mono.just(createResponse(message, conversationId, intent));
            } else {
                // List all user's recent requests
                return handleListRequests(userId, conversationId);
            }
        } catch (Exception e) {
            log.error("Failed to query status", e);
            return Mono.just(createErrorResponse(
                    "I couldn't find that request. Could you provide the ticket number?",
                    conversationId));
        }
    }

    /**
     * Handle list all requests
     */
    private Mono<VoiceInteractionResponse> handleListRequests(Long userId, String conversationId) {
        try {
            var requests = maintenanceService.getRequestsByUser(userId);

            if (requests.isEmpty()) {
                return Mono.just(createResponse(
                        "You don't have any maintenance requests at the moment.",
                        conversationId, null));
            }

            StringBuilder message = new StringBuilder();
            message.append(String.format("You have %d maintenance request%s. ",
                    requests.size(), requests.size() > 1 ? "s" : ""));

            int count = Math.min(3, requests.size());
            for (int i = 0; i < count; i++) {
                MaintenanceResponseDTO req = requests.get(i);
                message.append(String.format(
                        "Request #%d for %s is %s. ",
                        req.getId(),
                        req.getTitle(),
                        req.getStatus().toString().toLowerCase().replace("_", " ")));
            }

            if (requests.size() > 3) {
                message.append(String.format("And %d more. ", requests.size() - 3));
            }

            message.append("Would you like details on any specific request?");

            return Mono.just(createResponse(message.toString(), conversationId, null));

        } catch (Exception e) {
            log.error("Failed to list requests", e);
            return Mono.just(createErrorResponse(
                    "I'm having trouble retrieving your requests. Please try again.",
                    conversationId));
        }
    }

    /**
     * Handle emergency situations
     */
    private Mono<VoiceInteractionResponse> handleEmergency(
            IntentResult intent, Long userId, String conversationId) {

        // Create emergency request with CRITICAL priority
        MaintenanceRequestDTO dto = intent.getExtractedData();
        if (dto != null) {
            try {
                MaintenanceResponseDTO created = maintenanceService.createRequest(userId, dto);

                String message = String.format(
                        "I've marked this as an emergency. Ticket #%d has been created with critical priority. " +
                                "Our emergency team has been notified and will respond immediately. " +
                                "If this is life-threatening, please call 911. Stay safe!",
                        created.getId());

                VoiceInteractionResponse response = new VoiceInteractionResponse();
                response.setCreatedTicket(created);
                response.setTextResponse(message);
                response.setConversationId(conversationId);
                response.setIntent(intent);

                return Mono.just(response);

            } catch (Exception e) {
                log.error("Failed to create emergency request", e);
            }
        }

        return Mono.just(createResponse(
                "I understand this is an emergency. Please describe the situation so I can help immediately.",
                conversationId, intent));
    }

    /**
     * Handle general inquiries
     */
    private Mono<VoiceInteractionResponse> handleGeneralInquiry(
            String query, String conversationId) {

        String response = "I'm your HomeGenie maintenance assistant. " +
                "I can help you report maintenance issues, check the status of your requests, " +
                "or provide information about our services. How can I assist you today?";

        return Mono.just(createResponse(response, conversationId, null));
    }

    /**
     * Helper methods
     */
    private VoiceInteractionResponse createResponse(
            String text, String conversationId, IntentResult intent) {
        VoiceInteractionResponse response = new VoiceInteractionResponse();
        response.setTextResponse(text);
        response.setConversationId(conversationId);
        response.setIntent(intent);
        response.setRequiresFollowup(false);
        return response;
    }

    private VoiceInteractionResponse createErrorResponse(String message, String conversationId) {
        VoiceInteractionResponse response = new VoiceInteractionResponse();
        response.setTextResponse(message);
        response.setConversationId(conversationId);
        response.setRequiresFollowup(false);
        return response;
    }
}