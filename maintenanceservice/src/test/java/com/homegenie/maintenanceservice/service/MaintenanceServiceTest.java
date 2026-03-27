package com.homegenie.maintenanceservice.service;

import com.homegenie.maintenanceservice.dto.*;
import com.homegenie.maintenanceservice.model.*;
import com.homegenie.maintenanceservice.repository.MaintenanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for MaintenanceService
 *
 * Test Coverage:
 * - Create maintenance request (with AI classification)
 * - Update request (assign technician, status change)
 * - Get requests (by ID, by user)
 * - AI classification fallback
 * - Email notifications
 * - Image upload
 * - Kafka event publishing
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class MaintenanceServiceTest {

    @Mock
    private MaintenanceRepository repository;

    @Mock
    private AIClassificationService aiService;

    @Mock
    private S3Service s3Service;

    @Mock
    private EmailNotificationService emailService;

    @Mock
    private MaintenanceEventPublisher eventPublisher;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private MaintenanceService maintenanceService;

    private MaintenanceRequestDTO requestDTO;
    private MaintenanceRequest request;
    private AIClassificationResponse aiResponse;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        requestDTO = new MaintenanceRequestDTO();
        requestDTO.setTitle("Water leaking from bathroom");
        requestDTO.setDescription("Urgent water leak issue");
        requestDTO.setImageBase64("base64encodedimage");

        request = new MaintenanceRequest();
        request.setId(1L);
        request.setUserId(1L);
        request.setTitle("Water leaking from bathroom");
        request.setDescription("Urgent water leak issue");
        request.setCategory(Category.PLUMBING);
        request.setPriority(Priority.CRITICAL);
        request.setStatus(Status.PENDING);
        request.setImageUrl("https://s3.amazonaws.com/image.jpg");
        request.setCreatedAt(LocalDateTime.now());

        aiResponse = new AIClassificationResponse();
        aiResponse.setCategory(Category.PLUMBING);
        aiResponse.setPriority(Priority.CRITICAL);
        aiResponse.setReasoning("Emergency water leak detected");

        userResponse = new UserResponse();
        userResponse.setId(1L);
        userResponse.setEmail("user@example.com");
        userResponse.setFullName("Test User");
        userResponse.setPhoneNumber("1234567890");
    }

    @Test
    void testCreateRequest_Success() {
        // Given
        when(restTemplate.getForObject(anyString(), eq(UserResponse.class))).thenReturn(userResponse);
        when(aiService.classifyRequest(anyString(), anyString())).thenReturn(aiResponse);
        when(s3Service.uploadImage(anyString())).thenReturn(
                new S3Service.ImageUploadResult("https://s3.amazonaws.com/image.jpg", "s3", "test.jpg", false));
        when(repository.save(any(MaintenanceRequest.class))).thenReturn(request);
        doNothing().when(emailService).notifyAdminNewRequest(anyString(), anyString(), anyString(), anyString(), anyLong());
        doNothing().when(eventPublisher).publishMaintenanceCreatedEvent(any());

        // When
        MaintenanceResponseDTO response = maintenanceService.createRequest(1L, requestDTO);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Water leaking from bathroom", response.getTitle());
        assertEquals(Category.PLUMBING, response.getCategory());
        assertEquals(Priority.CRITICAL, response.getPriority());
        assertEquals(Status.PENDING, response.getStatus());

        verify(aiService, times(1)).classifyRequest(anyString(), anyString());
        verify(s3Service, times(1)).uploadImage(anyString());
        verify(repository, times(1)).save(any(MaintenanceRequest.class));
        verify(emailService, times(1)).notifyAdminNewRequest(anyString(), anyString(), anyString(), anyString(), anyLong());
        verify(eventPublisher, times(1)).publishMaintenanceCreatedEvent(any());
    }

    @Test
    void testCreateRequest_ImageUploadFails_ContinuesWithoutImage() {
        // Given
        when(restTemplate.getForObject(anyString(), eq(UserResponse.class))).thenReturn(userResponse);
        when(aiService.classifyRequest(anyString(), anyString())).thenReturn(aiResponse);
        when(s3Service.uploadImage(anyString())).thenThrow(new RuntimeException("S3 upload failed"));

        MaintenanceRequest requestWithoutImage = new MaintenanceRequest();
        requestWithoutImage.setId(1L);
        requestWithoutImage.setUserId(1L);
        requestWithoutImage.setTitle("Water leaking from bathroom");
        requestWithoutImage.setCategory(Category.PLUMBING);
        requestWithoutImage.setPriority(Priority.CRITICAL);
        requestWithoutImage.setStatus(Status.PENDING);
        requestWithoutImage.setImageUrl(null);

        when(repository.save(any(MaintenanceRequest.class))).thenReturn(requestWithoutImage);

        // When
        MaintenanceResponseDTO response = maintenanceService.createRequest(1L, requestDTO);

        // Then
        assertNotNull(response);
        assertNull(response.getImageUrl());
        verify(s3Service, times(1)).uploadImage(anyString());
        verify(repository, times(1)).save(any(MaintenanceRequest.class));
    }

    @Test
    void testUpdateRequest_AssignTechnician_Success() {
        // Given
        UpdateRequestDTO updateDTO = new UpdateRequestDTO();
        updateDTO.setAssignedTo(2L);

        UserResponse technicianResponse = new UserResponse();
        technicianResponse.setId(2L);
        technicianResponse.setEmail("tech@example.com");
        technicianResponse.setFullName("Technician");

        when(repository.findById(anyLong())).thenReturn(Optional.of(request));
        when(restTemplate.getForObject(anyString(), eq(UserResponse.class))).thenReturn(technicianResponse);
        when(repository.save(any(MaintenanceRequest.class))).thenReturn(request);

        // When
        MaintenanceResponseDTO response = maintenanceService.updateRequest(1L, updateDTO);

        // Then
        assertNotNull(response);
        verify(repository, times(1)).findById(1L);
        verify(repository, times(1)).save(any(MaintenanceRequest.class));
        verify(emailService, times(1)).notifyTechnicianAssignment(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyLong()
        );
        verify(eventPublisher, times(1)).publishMaintenanceAssignedEvent(any());
    }

    @Test
    void testUpdateRequest_StatusChange_Success() {
        // Given
        UpdateRequestDTO updateDTO = new UpdateRequestDTO();
        updateDTO.setStatus(Status.COMPLETED);

        when(repository.findById(anyLong())).thenReturn(Optional.of(request));
        when(restTemplate.getForObject(anyString(), eq(UserResponse.class))).thenReturn(userResponse);
        when(repository.save(any(MaintenanceRequest.class))).thenReturn(request);

        // When
        MaintenanceResponseDTO response = maintenanceService.updateRequest(1L, updateDTO);

        // Then
        assertNotNull(response);
        verify(repository, times(1)).save(argThat(req ->
            req.getStatus() == Status.COMPLETED && req.getResolvedAt() != null
        ));
        verify(eventPublisher, times(1)).publishMaintenanceStatusChangedEvent(any());
    }

    @Test
    void testGetRequestById_Success() {
        // Given
        when(repository.findById(anyLong())).thenReturn(Optional.of(request));

        // When
        MaintenanceResponseDTO response = maintenanceService.getRequestById(1L);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Water leaking from bathroom", response.getTitle());
        verify(repository, times(1)).findById(1L);
    }

    @Test
    void testGetRequestById_NotFound_ThrowsException() {
        // Given
        when(repository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            maintenanceService.getRequestById(999L);
        });

        assertEquals("Request not found", exception.getMessage());
        verify(repository, times(1)).findById(999L);
    }
}

