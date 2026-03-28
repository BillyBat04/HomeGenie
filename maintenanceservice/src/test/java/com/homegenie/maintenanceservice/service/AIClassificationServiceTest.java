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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressWarnings("null")
class AIClassificationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AIClassificationService aiService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(aiService, "geminiApiKey", "test-gemini-key");
        
        ReflectionTestUtils.setField(aiService, "huggingFaceToken", "");
        ReflectionTestUtils.setField(aiService, "geminiEnabled", false);
        ReflectionTestUtils.setField(aiService, "huggingFaceEnabled", false);
    }

    @Test
    void testClassifyRequest_EmergencyKeywords_CriticalPriority() {
        
        String title = "URGENT EMERGENCY water flooding apartment";
        String description = "Water leaking everywhere, need help now!";

        
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("API unavailable"));

        
        AIClassificationResponse response = aiService.classifyRequest(title, description);

        
        assertNotNull(response);
        assertEquals(Priority.CRITICAL, response.getPriority());
    }

    @Test
    void testClassifyRequest_PlumbingKeywords_PlumbingCategory() {
        
        String title = "Kitchen sink leaking";
        String description = "Water dripping from pipe under sink";

        
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("API unavailable"));

        
        AIClassificationResponse response = aiService.classifyRequest(title, description);

        
        assertNotNull(response);
        assertEquals(Category.PLUMBING, response.getCategory());
    }

    @Test
    void testClassifyRequest_ElectricalKeywords_ElectricalCategory() {
        
        String title = "Power outlet not working";
        String description = "Electrical socket has no power, lights flickering";

        
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("API unavailable"));

        
        AIClassificationResponse response = aiService.classifyRequest(title, description);

        
        assertNotNull(response);
        assertEquals(Category.ELECTRICAL, response.getCategory());
    }

    @Test
    void testClassifyRequest_ACKeywords_AirConditioningCategory() {
        
        String title = "Air conditioner not cooling";
        String description = "AC making noise but not cold";

        
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("API unavailable"));

        
        AIClassificationResponse response = aiService.classifyRequest(title, description);

        
        assertNotNull(response);
        assertEquals(Category.HVAC, response.getCategory());
    }

    @Test
    void testClassifyRequest_CleaningKeywords_CleaningCategory() {
        
        String title = "Need cleaning service";
        String description = "Apartment needs deep cleaning";

        
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("API unavailable"));

        
        AIClassificationResponse response = aiService.classifyRequest(title, description);

        
        assertNotNull(response);
        assertEquals(Category.CLEANING, response.getCategory());
    }

    @Test
    void testClassifyRequest_HighPriorityKeywords_HighPriority() {
        
        String title = "Broken window needs attention";
        String description = "Window broken, urgent repair needed";

        
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("API unavailable"));

        
        AIClassificationResponse response = aiService.classifyRequest(title, description);

        
        assertNotNull(response);
        assertTrue(response.getPriority() == Priority.HIGH ||
                   response.getPriority() == Priority.CRITICAL);
    }

    @Test
    void testClassifyRequest_LowPriorityKeywords_LowPriority() {
        
        String title = "Minor paint touch up";
        String description = "Small paint chip on wall";

        
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("API unavailable"));

        
        AIClassificationResponse response = aiService.classifyRequest(title, description);

        
        assertNotNull(response);
        
        assertTrue(response.getPriority() == Priority.LOW ||
                   response.getPriority() == Priority.MODERATE);
    }

    @Test
    void testClassifyRequest_NullTitle_ReturnsDefault() {
        
        String title = null;
        String description = "Some description";

        
        AIClassificationResponse response = aiService.classifyRequest(title, description);

        
        assertNotNull(response);
        assertNotNull(response.getCategory());
        assertNotNull(response.getPriority());
        assertEquals("OTHERS", response.getCategory().name());
    }

    @Test
    void testClassifyRequest_EmptyTitle_ReturnsDefault() {
        
        String title = "";
        String description = "Some description";

        
        AIClassificationResponse response = aiService.classifyRequest(title, description);

        
        assertNotNull(response);
        assertNotNull(response.getCategory());
        assertNotNull(response.getPriority());
    }

    @Test
    void testClassifyRequest_NullDescription_UsesTitle() {
        
        String title = "Plumbing emergency";
        String description = null;

        
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("API unavailable"));

        
        AIClassificationResponse response = aiService.classifyRequest(title, description);

        
        assertNotNull(response);
        assertEquals(Category.PLUMBING, response.getCategory());
        assertEquals(Priority.HIGH, response.getPriority());
    }

    @Test
    void testClassifyRequest_EmptyDescription_UsesTitle() {
        
        String title = "Electrical issue urgent";
        String description = "";

        
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("API unavailable"));

        
        AIClassificationResponse response = aiService.classifyRequest(title, description);

        
        assertNotNull(response);
        assertEquals(Category.ELECTRICAL, response.getCategory());
        assertTrue(response.getPriority() == Priority.HIGH ||
                   response.getPriority() == Priority.CRITICAL);
    }

    @Test
    void testClassifyRequest_BothNullOrEmpty_ReturnsDefault() {
        
        String title = "";
        String description = "";

        
        AIClassificationResponse response = aiService.classifyRequest(title, description);

        
        assertNotNull(response);
        assertEquals(Category.OTHERS, response.getCategory());
        assertEquals(Priority.LOW, response.getPriority());
    }

    @Test
    void testClassifyRequest_MultipleCategories_ReturnsFirstMatch() {
        
        String title = "Plumbing and electrical problems";
        String description = "Sink leaking and power outlet not working";

        
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("API unavailable"));

        
        AIClassificationResponse response = aiService.classifyRequest(title, description);

        
        assertNotNull(response);
        
        assertTrue(response.getCategory() == Category.PLUMBING ||
                   response.getCategory() == Category.ELECTRICAL);
    }

    @Test
    void testClassifyRequest_CaseInsensitive_Success() {
        
        String title = "PLUMBING EMERGENCY";
        String description = "URGENT WATER LEAK";

        
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("API unavailable"));

        
        AIClassificationResponse response = aiService.classifyRequest(title, description);

        
        assertNotNull(response);
        assertEquals(Category.PLUMBING, response.getCategory());
        assertEquals(Priority.CRITICAL, response.getPriority());
    }

    @Test
    void testClassifyRequest_SpecialCharacters_HandledCorrectly() {
        
        String title = "Plumbing!!! @#$ emergency";
        String description = "Water leak $$ urgent!!!";

        
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("API unavailable"));

        
        AIClassificationResponse response = aiService.classifyRequest(title, description);

        
        assertNotNull(response);
        assertEquals(Category.PLUMBING, response.getCategory());
        assertEquals(Priority.CRITICAL, response.getPriority());
    }

    @Test
    void testClassifyRequest_NoKeywords_ReturnsOther() {
        
        String title = "Random request";
        String description = "Some random text without keywords";

        
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("API unavailable"));

        
        AIClassificationResponse response = aiService.classifyRequest(title, description);

        
        assertNotNull(response);
        assertEquals(Category.OTHERS, response.getCategory());
        assertEquals(Priority.LOW, response.getPriority());
    }

    @Test
    void testClassifyRequest_VeryLongText_HandledCorrectly() {
        
        String title = "Plumbing issue";
        String description = "Water ".repeat(1000) + "leaking"; 

        
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("API unavailable"));

        
        AIClassificationResponse response = aiService.classifyRequest(title, description);

        
        assertNotNull(response);
        assertEquals(Category.PLUMBING, response.getCategory());
    }

    @Test
    void testClassifyRequest_APITimeout_FallsBackToRuleBased() {
        
        String title = "Electrical problem urgent";
        String description = "Power socket not working";

        
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("Timeout"));

        
        AIClassificationResponse response = aiService.classifyRequest(title, description);

        
        assertNotNull(response);
        assertEquals(Category.ELECTRICAL, response.getCategory());
        assertNotNull(response.getReasoning());
        assertTrue(response.getReasoning().contains("Rule-based") ||
                   response.getReasoning().contains("Keyword"));
    }
}

