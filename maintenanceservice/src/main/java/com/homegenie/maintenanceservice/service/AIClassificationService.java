package com.homegenie.maintenanceservice.service;

import com.homegenie.maintenanceservice.dto.AIClassificationResponse;
import com.homegenie.maintenanceservice.model.Category;
import com.homegenie.maintenanceservice.model.Priority;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;

@Service
@Slf4j
public class AIClassificationService {

    @Value("${huggingface.api.token:}")
    private String apiToken;

    private final WebClient webClient;

    private static final String HUGGINGFACE_API_URL = "https://router.huggingface.co/hf-inference/models/facebook/bart-large-mnli";

    private static final Map<Category, List<String>> CATEGORY_KEYWORDS = Map.of(
            Category.PLUMBING, Arrays.asList("water", "leak", "pipe", "tap", "drain", "toilet", "sink", "bathroom", "kitchen", "faucet", "plumbing"),
            Category.ELECTRICAL, Arrays.asList("light", "electricity", "power", "socket", "wiring", "switch", "fan", "bulb", "fuse", "electrical", "outlet"),
            Category.CLEANING, Arrays.asList("garbage", "trash", "dirty", "clean", "sweeping", "waste", "dustbin", "mess", "sanitation"),
            Category.SECURITY, Arrays.asList("gate", "lock", "security", "cctv", "camera", "guard", "entry", "access", "alarm", "safety"),
            Category.CARPENTRY, Arrays.asList("door", "window", "furniture", "wood", "cabinet", "shelf", "wardrobe", "carpenter", "timber", "knob", "handle"),
            Category.PAINTING, Arrays.asList("paint", "wall", "ceiling", "color", "whitewash", "painter", "coating"),
            Category.HVAC, Arrays.asList("ac", "air conditioning", "heating", "ventilation", "temperature", "thermostat", "hvac", "cooling")
    );

    private static final List<String> CRITICAL_KEYWORDS = Arrays.asList(
            "urgent", "emergency", "immediately", "critical", "dangerous", "leak", "fire", "electrical",
            "gas", "no water", "no power", "flooding", "smoke", "broken", "hazard"
    );

    private static final List<String> HIGH_PRIORITY_KEYWORDS = Arrays.asList(
            "soon", "asap", "quickly", "important", "needed", "priority", "problem", "issue"
    );

    public AIClassificationService() {
        this.webClient = WebClient.builder()
                .baseUrl(HUGGINGFACE_API_URL)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public AIClassificationResponse classifyRequest(String title, String description) {
        String combinedText = (title + " " + description).toLowerCase();

        // Try AI classification first (if API token is available)
        if (apiToken != null && !apiToken.isEmpty() && !apiToken.isBlank()) {
            try {
                log.info("Attempting AI classification with Hugging Face...");
                return classifyWithHuggingFace(combinedText);
            } catch (Exception e) {
                log.warn("AI classification failed, falling back to rule-based: {}", e.getMessage());
            }
        } else {
            log.debug("Hugging Face API token not configured, using rule-based classification");
        }

        return classifyWithRules(combinedText);
    }

    private AIClassificationResponse classifyWithHuggingFace(String text) {
        try {
            List<String> candidateLabels = Arrays.asList(
                    "plumbing issue", "electrical problem", "cleaning request",
                    "security concern", "carpentry work", "painting job",
                    "hvac issue", "general maintenance"
            );

            Map<String, Object> requestBody = Map.of(
                    "inputs", text,
                    "parameters", Map.of(
                            "candidate_labels", candidateLabels,
                            "multi_label", false
                    )
            );

            log.debug("Sending request to Hugging Face API");

            Map<String, Object> response = webClient.post()
                    .header("Authorization", "Bearer " + apiToken)
                    .header("x-wait-for-model", "true")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            if (response != null && response.containsKey("labels")) {
                List<String> labels = (List<String>) response.get("labels");
                List<Double> scores = (List<Double>) response.get("scores");

                if (labels != null && !labels.isEmpty() && scores != null && !scores.isEmpty()) {
                    String topLabel = labels.get(0);
                    Double confidence = scores.get(0);

                    Category category = mapLabelToCategory(topLabel);
                    Priority priority = determinePriority(text);

                    AIClassificationResponse aiResponse = new AIClassificationResponse();
                    aiResponse.setCategory(category);
                    aiResponse.setPriority(priority);
                    aiResponse.setReasoning("AI detected: " + topLabel + " (confidence: " +
                            String.format("%.2f", confidence * 100) + "%)");

                    log.info("AI classification successful: {} with confidence {}", category, confidence);
                    return aiResponse;
                }
            }

            throw new RuntimeException("Invalid response from AI classification");

        } catch (Exception e) {
            log.error("Hugging Face API error: {}", e.getMessage());
            throw new RuntimeException("AI classification failed: " + e.getMessage(), e);
        }
    }

    private AIClassificationResponse classifyWithRules(String text) {
        log.debug("Using rule-based classification for text: {}", text.substring(0, Math.min(50, text.length())));

        Category category = determineCategory(text);
        Priority priority = determinePriority(text);

        AIClassificationResponse response = new AIClassificationResponse();
        response.setCategory(category);
        response.setPriority(priority);
        response.setReasoning("Rule-based classification based on keywords");

        log.info("Rule-based classification result: Category={}, Priority={}", category, priority);
        return response;
    }

    private Category determineCategory(String text) {
        Map<Category, Integer> scores = new HashMap<>();

        for (Map.Entry<Category, List<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            int score = 0;
            for (String keyword : entry.getValue()) {
                if (text.contains(keyword)) {
                    score++;
                }
            }
            if (score > 0) {
                scores.put(entry.getKey(), score);
            }
        }

        return scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(Category.OTHERS);
    }

    private Priority determinePriority(String text) {
        long criticalCount = CRITICAL_KEYWORDS.stream()
                .filter(text::contains)
                .count();

        if (criticalCount >= 2) {
            log.debug("Critical priority detected (multiple critical keywords)");
            return Priority.CRITICAL;
        }

        if (criticalCount == 1) {
            log.debug("High priority detected (one critical keyword)");
            return Priority.HIGH;
        }

        long highPriorityCount = HIGH_PRIORITY_KEYWORDS.stream()
                .filter(text::contains)
                .count();

        if (highPriorityCount >= 1) {
            log.debug("High priority detected (priority keywords)");
            return Priority.HIGH;
        }

        if (text.length() > 200) {
            log.debug("Moderate priority detected (long description)");
            return Priority.MODERATE;
        }

        log.debug("Low priority detected (default)");
        return Priority.LOW;
    }

    private Category mapLabelToCategory(String label) {
        String lowerLabel = label.toLowerCase();

        if (lowerLabel.contains("plumbing")) return Category.PLUMBING;
        if (lowerLabel.contains("electrical")) return Category.ELECTRICAL;
        if (lowerLabel.contains("cleaning")) return Category.CLEANING;
        if (lowerLabel.contains("security")) return Category.SECURITY;
        if (lowerLabel.contains("carpentry")) return Category.CARPENTRY;
        if (lowerLabel.contains("painting")) return Category.PAINTING;
        if (lowerLabel.contains("hvac") || lowerLabel.contains("air")) return Category.HVAC;

        return Category.OTHERS;
    }
}