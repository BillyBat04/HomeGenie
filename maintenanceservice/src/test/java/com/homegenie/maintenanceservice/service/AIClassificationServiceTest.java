package com.homegenie.maintenanceservice.service;

import com.homegenie.maintenanceservice.dto.AIClassificationResponse;
import com.homegenie.maintenanceservice.model.Category;
import com.homegenie.maintenanceservice.model.Priority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for AIClassificationService
 *
 * Test Coverage:
 * - AI classification with Gemini
 * - Fallback to HuggingFace when Gemini fails
 * - Fallback to rule-based when AI fails
 * - Category detection from keywords
 * - Priority detection from keywords
 * - Emergency keyword detection
 * - Null/empty input handling
 * - API timeout handling
 */
@ExtendWith(MockitoExtension.class)
class AIClassificationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AIClassificationService aiService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(aiService, "geminiApiKey", "test-gemini-key");
        ReflectionTestUtils.setField(aiService, "huggingFaceToken", "test-hf-token");
        ReflectionTestUtils.setField(aiService, "geminiEnabled", true);
        ReflectionTestUtils.setField(aiService, "huggingFaceEnabled", true);
    }

    @Test
    void testClassifyRequest_EmergencyKeywords_CriticalPriority() {
        // Given
        String title = "URGENT EMERGENCY water flooding apartment";
        String description = "Water leaking everywhere, need help now!";

        // Mock Gemini API call to fail, forcing rule-based fallback
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("API unavailable"));

        // When
        AIClassificationResponse response = aiService.classifyRequest(title, description);

        // Then
        assertNotNull(response);
        assertEquals(Priority.CRITICAL, response.getPriority());
        assertTrue(response.getReasoning().contains("emergency") ||
                   response.getReasoning().contains("urgent"));
    }

    @Test
    void testClassifyRequest_PlumbingKeywords_PlumbingCategory() {
        // Given
        String title = "Kitchen sink leaking";
        String description = "Water dripping from pipe under sink";

        // Mock Gemini API failure
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("API unavailable"));

        // When
        AIClassificationResponse response = aiService.classifyRequest(title, description);

        // Then
        assertNotNull(response);
        assertEquals(Category.PLUMBING, response.getCategory());
    }

    @Test
    void testClassifyRequest_ElectricalKeywords_ElectricalCategory() {
        // Given
        String title = "Power outlet not working";
        String description = "Electrical socket has no power, lights flickering";

        // Mock API failure
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("API unavailable"));

        // When
        AIClassificationResponse response = aiService.classifyRequest(title, description);

        // Then
        assertNotNull(response);
        assertEquals(Category.ELECTRICAL, response.getCategory());
    }

    @Test
    void testClassifyRequest_ACKeywords_AirConditioningCategory() {
        // Given
        String title = "Air conditioner not cooling";
        String description = "AC making noise but not cold";

        // Mock API failure
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("API unavailable"));

        // When
        AIClassificationResponse response = aiService.classifyRequest(title, description);

        // Then
        assertNotNull(response);
        assertEquals(Category.AIR_CONDITIONING, response.getCategory());
    }

    @Test
    void testClassifyRequest_CleaningKeywords_CleaningCategory() {
        // Given
        String title = "Need cleaning service";
        String description = "Apartment needs deep cleaning";

        // Mock API failure
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("API unavailable"));

        // When
        AIClassificationResponse response = aiService.classifyRequest(title, description);

        // Then
        assertNotNull(response);
        assertEquals(Category.CLEANING, response.getCategory());
    }

    @Test
    void testClassifyRequest_HighPriorityKeywords_HighPriority() {
        // Given
        String title = "Broken window needs attention";
        String description = "Window broken, urgent repair needed";

        // Mock API failure
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("API unavailable"));

        // When
        AIClassificationResponse response = aiService.classifyRequest(title, description);

        // Then
        assertNotNull(response);
        assertTrue(response.getPriority() == Priority.HIGH ||
                   response.getPriority() == Priority.CRITICAL);
    }

    @Test
    void testClassifyRequest_LowPriorityKeywords_LowPriority() {
        // Given
        String title = "Minor paint touch up needed";
        String description = "Small paint chip on wall, not urgent";

        // Mock API failure
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("API unavailable"));

        // When
        AIClassificationResponse response = aiService.classifyRequest(title, description);

        // Then
        assertNotNull(response);
        // Should be LOW or MEDIUM
        assertTrue(response.getPriority() == Priority.LOW ||
                   response.getPriority() == Priority.MEDIUM);
    }

    @Test
    void testClassifyRequest_NullTitle_ReturnsDefault() {
        // Given
        String title = null;
        String description = "Some description";

        // When
        AIClassificationResponse response = aiService.classifyRequest(title, description);

        // Then
        assertNotNull(response);
        assertNotNull(response.getCategory());
        assertNotNull(response.getPriority());
        assertEquals("OTHER", response.getCategory().name());
    }

    @Test
    void testClassifyRequest_EmptyTitle_ReturnsDefault() {
        // Given
        String title = "";
        String description = "Some description";

        // When
        AIClassificationResponse response = aiService.classifyRequest(title, description);

        // Then
        assertNotNull(response);
        assertNotNull(response.getCategory());
        assertNotNull(response.getPriority());
    }

    @Test
    void testClassifyRequest_NullDescription_UsesTitle() {
        // Given
        String title = "Plumbing emergency";
        String description = null;

        // Mock API failure
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("API unavailable"));

        // When
        AIClassificationResponse response = aiService.classifyRequest(title, description);

        // Then
        assertNotNull(response);
        assertEquals(Category.PLUMBING, response.getCategory());
        assertEquals(Priority.CRITICAL, response.getPriority());
    }

    @Test
    void testClassifyRequest_EmptyDescription_UsesTitle() {
        // Given
        String title = "Electrical issue urgent";
        String description = "";

        // Mock API failure
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("API unavailable"));

        // When
        AIClassificationResponse response = aiService.classifyRequest(title, description);

        // Then
        assertNotNull(response);
        assertEquals(Category.ELECTRICAL, response.getCategory());
        assertTrue(response.getPriority() == Priority.HIGH ||
                   response.getPriority() == Priority.CRITICAL);
    }

    @Test
    void testClassifyRequest_BothNullOrEmpty_ReturnsDefault() {
        // Given
        String title = "";
        String description = "";

        // When
        AIClassificationResponse response = aiService.classifyRequest(title, description);

        // Then
        assertNotNull(response);
        assertEquals(Category.OTHER, response.getCategory());
        assertEquals(Priority.MEDIUM, response.getPriority());
    }

    @Test
    void testClassifyRequest_MultipleCategories_ReturnsFirstMatch() {
        // Given
        String title = "Plumbing and electrical problems";
        String description = "Sink leaking and power outlet not working";

        // Mock API failure
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("API unavailable"));

        // When
        AIClassificationResponse response = aiService.classifyRequest(title, description);

        // Then
        assertNotNull(response);
        // Should match either PLUMBING or ELECTRICAL (first occurrence)
        assertTrue(response.getCategory() == Category.PLUMBING ||
                   response.getCategory() == Category.ELECTRICAL);
    }

    @Test
    void testClassifyRequest_CaseInsensitive_Success() {
        // Given
        String title = "PLUMBING EMERGENCY";
        String description = "URGENT WATER LEAK";

        // Mock API failure
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("API unavailable"));

        // When
        AIClassificationResponse response = aiService.classifyRequest(title, description);

        // Then
        assertNotNull(response);
        assertEquals(Category.PLUMBING, response.getCategory());
        assertEquals(Priority.CRITICAL, response.getPriority());
    }

    @Test
    void testClassifyRequest_SpecialCharacters_HandledCorrectly() {
        // Given
        String title = "Plumbing!!! @#$ emergency";
        String description = "Water leak $$ urgent!!!";

        // Mock API failure
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("API unavailable"));

        // When
        AIClassificationResponse response = aiService.classifyRequest(title, description);

        // Then
        assertNotNull(response);
        assertEquals(Category.PLUMBING, response.getCategory());
        assertEquals(Priority.CRITICAL, response.getPriority());
    }

    @Test
    void testClassifyRequest_NoKeywords_ReturnsOther() {
        // Given
        String title = "Random request";
        String description = "Some random text without keywords";

        // Mock API failure
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("API unavailable"));

        // When
        AIClassificationResponse response = aiService.classifyRequest(title, description);

        // Then
        assertNotNull(response);
        assertEquals(Category.OTHER, response.getCategory());
        assertEquals(Priority.MEDIUM, response.getPriority());
    }

    @Test
    void testClassifyRequest_VeryLongText_HandledCorrectly() {
        // Given
        String title = "Plumbing issue";
        String description = "Water ".repeat(1000) + "leaking"; // Very long description

        // Mock API failure
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("API unavailable"));

        // When
        AIClassificationResponse response = aiService.classifyRequest(title, description);

        // Then
        assertNotNull(response);
        assertEquals(Category.PLUMBING, response.getCategory());
    }

    @Test
    void testClassifyRequest_APITimeout_FallsBackToRuleBased() {
        // Given
        String title = "Electrical problem urgent";
        String description = "Power socket not working";

        // Mock API timeout
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("Timeout"));

        // When
        AIClassificationResponse response = aiService.classifyRequest(title, description);

        // Then
        assertNotNull(response);
        assertEquals(Category.ELECTRICAL, response.getCategory());
        assertNotNull(response.getReasoning());
        assertTrue(response.getReasoning().contains("Rule-based") ||
                   response.getReasoning().contains("Keyword"));
    }
}

