package com.homegenie.maintenanceservice.service;

import com.homegenie.maintenanceservice.client.PaymentServiceClient;
import com.homegenie.maintenanceservice.dto.*;
import com.homegenie.maintenanceservice.dto.event.*;
import com.homegenie.maintenanceservice.dto.payment.MiniAppPaymentRequest;
import com.homegenie.maintenanceservice.dto.payment.PaymentResponse;
import com.homegenie.maintenanceservice.model.*;
import com.homegenie.maintenanceservice.repository.MaintenanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaintenanceService {

    private final MaintenanceRepository repository;
    private final AIClassificationService aiService;
    private final S3Service s3Service;
    private final EmailNotificationService emailService;
    private final MaintenanceEventPublisher eventPublisher;
    private final RestTemplate restTemplate;
    private final PaymentServiceClient paymentServiceClient;

    @Value("${user.service.url:http://localhost:8081}")
    private String userServiceUrl;

    @Transactional
    public MaintenanceResponseDTO createRequest(Long userId, MaintenanceRequestDTO dto) {
        log.info("Creating maintenance request for user: {}", userId);

        // Get user details
        UserResponse user = getUserDetails(userId);
        
        // CRITICAL FIX: Validate itemId if provided (ISSUE-002)
        if (dto.getItemId() != null) {
            log.info("Validating item {} for user {}", dto.getItemId(), userId);
            validateItemOwnership(dto.getItemId(), userId);
        }

        // AI Classification
        AIClassificationResponse aiResult = aiService.classifyRequest(dto.getTitle(), dto.getDescription());
        log.info("AI Classification - Category: {}, Priority: {}", aiResult.getCategory(), aiResult.getPriority());

        MaintenanceRequest request = new MaintenanceRequest();
        request.setUserId(userId);
        request.setItemId(dto.getItemId()); // Set itemId (can be null for ad-hoc requests)
        request.setTitle(dto.getTitle());
        request.setDescription(dto.getDescription());
        request.setCategory(aiResult.getCategory());
        request.setPriority(aiResult.getPriority());
        request.setStatus(Status.PENDING);

        // Upload image if provided
        if (dto.getImageBase64() != null && !dto.getImageBase64().isEmpty()) {
            try {
                String imageUrl = String.valueOf(s3Service.uploadImage(dto.getImageBase64()));
                request.setImageUrl(imageUrl);
            } catch (Exception e) {
                log.error("Failed to upload image, continuing without it", e);
            }
        }

        MaintenanceRequest saved = repository.save(request);

        // 🔔 Publish MaintenanceCreatedEvent to Kafka
        publishMaintenanceCreatedEvent(saved, user);

        // Keep old email notification for backward compatibility (can be removed later)
        // Send notification to admin
        try {
            emailService.notifyAdminNewRequest(
                    user.getFullName(),
                    saved.getTitle(),
                    saved.getCategory().toString(),
                    saved.getPriority().toString(),
                    saved.getId()
            );
            log.info("Admin notification sent for request ID: {}", saved.getId());
        } catch (Exception e) {
            log.error("Failed to send admin notification", e);
        }

        if (saved.getAssignedTo() != null) {
            try {
                UserResponse technician = getUserDetails(saved.getAssignedTo());
                emailService.notifyTechnicianAssignment(
                        technician.getEmail(),
                        technician.getFullName(),
                        saved.getTitle(),
                        saved.getCategory().toString(),
                        saved.getPriority().toString(),
                        saved.getId()
                );
                log.info("Technician notification sent to: {} (ID: {})",
                        technician.getEmail(), technician.getId());
            } catch (Exception e) {
                log.error("Failed to send technician notification", e);
            }
        }

        return mapToResponseDTO(saved);
    }

    public List<MaintenanceResponseDTO> getAllRequests() {
        return repository.findAll().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<MaintenanceResponseDTO> getRequestsByUser(Long userId) {
        return repository.findByUserId(userId).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public MaintenanceResponseDTO getRequestById(Long id) {
        MaintenanceRequest request = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        return mapToResponseDTO(request);
    }

    @Transactional
    public MaintenanceResponseDTO updateRequest(Long id, UpdateRequestDTO dto) {
        log.info("Updating maintenance request ID: {} with data: {}", id, dto);

        MaintenanceRequest request = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        Status oldStatus = request.getStatus();
        Long oldAssignedTo = request.getAssignedTo();

        log.info("Current state - Status: {}, AssignedTo: {}", oldStatus, oldAssignedTo);
        log.info("Update DTO - Status: {}, AssignedTo: {}, AdminNotes: {}",
                dto.getStatus(), dto.getAssignedTo(), dto.getAdminNotes());

        if (dto.getStatus() != null) {
            log.info("Updating status from {} to {}", oldStatus, dto.getStatus());
            request.setStatus(dto.getStatus());
            if (dto.getStatus() == Status.COMPLETED) {
                request.setResolvedAt(LocalDateTime.now());
                log.info("Request marked as completed at: {}", request.getResolvedAt());
                
                // 💰 Create payment for completed maintenance request
                try {
                    BigDecimal amount = calculateMaintenanceAmount(request);
                    MiniAppPaymentRequest paymentRequest = MiniAppPaymentRequest.builder()
                            .userId(request.getUserId())
                            .miniAppId("maintenance")
                            .orderId(request.getId())
                            .amount(amount)
                            .currency("USD")
                            .paymentMethod("card")
                            .description("Maintenance service payment - " + request.getTitle())
                            .build();
                    
                    log.info("Creating payment for maintenance request {}: amount={}", id, amount);
                    PaymentResponse paymentResponse = paymentServiceClient.createPayment(paymentRequest);
                    log.info("Payment created successfully: paymentId={}", paymentResponse.getPaymentId());
                    
                } catch (Exception e) {
                    log.error("Failed to create payment for maintenance request {}: {}", id, e.getMessage(), e);
                    // Don't fail the whole update if payment fails
                }
            }
        }

        if (dto.getAssignedTo() != null) {
            log.info("Assignment requested - Old: {}, New: {}", oldAssignedTo, dto.getAssignedTo());

            if (!dto.getAssignedTo().equals(oldAssignedTo)) {
                log.info("Assigning technician ID: {} to request ID: {}", dto.getAssignedTo(), id);
                request.setAssignedTo(dto.getAssignedTo());

                // Change status to IN_PROGRESS if it was PENDING
                if (request.getStatus() == Status.PENDING) {
                    log.info("Changing status from PENDING to IN_PROGRESS");
                    request.setStatus(Status.IN_PROGRESS);
                }

                // 🔔 Publish MaintenanceAssignedEvent to Kafka
                MaintenanceRequest savedRequest = repository.save(request);
                publishMaintenanceAssignedEvent(savedRequest, dto.getAssignedTo());

                // Keep old email notification for backward compatibility (can be removed later)
                // Send notification to new technician
                try {
                    log.info("Fetching technician details for ID: {}", dto.getAssignedTo());
                    UserResponse technician = getUserDetails(dto.getAssignedTo());

                    if (technician == null) {
                        log.error("Technician not found for ID: {}", dto.getAssignedTo());
                    } else {
                        log.info("Sending notification to technician: {} ({})",
                                technician.getFullName(), technician.getEmail());

                        emailService.notifyTechnicianAssignment(
                                technician.getEmail(),
                                technician.getFullName(),
                                request.getTitle(),
                                request.getCategory().toString(),
                                request.getPriority().toString(),
                                request.getId()
                        );
                        log.info("Technician notification sent successfully to: {} (ID: {})",
                                technician.getEmail(), technician.getId());
                    }
                } catch (Exception e) {
                    log.error("Failed to send technician notification: {}", e.getMessage(), e);
                }
            } else {
                log.info("Technician unchanged, skipping notification");
            }
        } else {
            log.info("No technician assignment in update request");
        }

        if (dto.getAdminNotes() != null) {
            request.setAdminNotes(dto.getAdminNotes());
        }

        request.setUpdatedAt(LocalDateTime.now());

        MaintenanceRequest updated = repository.save(request);

        // 🔔 Publish MaintenanceStatusChangedEvent if status changed
        if (oldStatus != updated.getStatus()) {
            publishMaintenanceStatusChangedEvent(updated, oldStatus, updated.getStatus());
            
            // 🔔 Publish MaintenanceCompletedEvent if status changed to COMPLETED and linked to item
            if (updated.getStatus() == Status.COMPLETED && updated.getItemId() != null) {
                publishMaintenanceCompletedEvent(updated);
            }
        }

        // Keep old email notification for backward compatibility (can be removed later)
        // Send status change notification to resident if status changed
        if (oldStatus != updated.getStatus()) {
            try {
                UserResponse resident = getUserDetails(updated.getUserId());
                emailService.notifyResidentStatusChange(
                        resident.getEmail(),
                        resident.getFullName(),
                        updated.getTitle(),
                        oldStatus.toString(),
                        updated.getStatus().toString(),
                        updated.getId()
                );
                log.info("Status change notification sent to resident: {}", resident.getEmail());
            } catch (Exception e) {
                log.error("Failed to send status change notification", e);
            }
        }

        return mapToResponseDTO(updated);
    }

    public UserResponse getUserDetails(Long userId) {
        try {
            String url = userServiceUrl + "/api/users/" + userId;

            log.info("Fetching user details from: {}", url);

            UserResponse user = restTemplate.getForObject(url, UserResponse.class);

            if (user != null) {
                log.info("User details retrieved successfully: ID={}, Name={}, Email={}",
                        user.getId(), user.getFullName(), user.getEmail());
            } else {
                log.warn("No user found for ID: {}", userId);
            }

            return user;

        } catch (Exception e) {
            log.error("Failed to get user details for userId: {}", userId, e);

            UserResponse dummy = new UserResponse();
            dummy.setId(userId);
            dummy.setEmail("unknown@example.com");
            dummy.setFullName("Unknown User");
            return dummy;
        }
    }

    @Transactional
    public void deleteRequest(Long id) {
        MaintenanceRequest request = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (request.getImageUrl() != null) {
            try {
                s3Service.deleteImage(request.getImageUrl());
            } catch (Exception e) {
                log.error("Failed to delete image", e);
            }
        }

        repository.deleteById(id);
    }

    public Map<String, Long> getStatistics() {
        List<MaintenanceRequest> all = repository.findAll();

        Map<String, Long> stats = new HashMap<>();
        stats.put("total", (long) all.size());
        stats.put("pending", all.stream().filter(r -> r.getStatus() == Status.PENDING).count());
        stats.put("inProgress", all.stream().filter(r -> r.getStatus() == Status.IN_PROGRESS).count());
        stats.put("completed", all.stream().filter(r -> r.getStatus() == Status.COMPLETED).count());
        stats.put("critical", all.stream().filter(r -> r.getPriority() == Priority.CRITICAL).count());

        return stats;
    }

    private MaintenanceResponseDTO mapToResponseDTO(MaintenanceRequest request) {
        MaintenanceResponseDTO dto = new MaintenanceResponseDTO();
        dto.setId(request.getId());
        dto.setUserId(request.getUserId());
        dto.setTitle(request.getTitle());
        dto.setDescription(request.getDescription());
        dto.setCategory(request.getCategory());
        dto.setPriority(request.getPriority());
        dto.setStatus(request.getStatus());
        dto.setImageUrl(request.getImageUrl());
        dto.setAssignedTo(request.getAssignedTo());
        dto.setCreatedAt(request.getCreatedAt());
        dto.setUpdatedAt(request.getUpdatedAt());
        dto.setResolvedAt(request.getResolvedAt());
        dto.setAdminNotes(request.getAdminNotes());
        return dto;
    }

    public List<UserResponse> getAllTechnicians() {
        try {
            String url = userServiceUrl + "/api/users/technicians";
            log.info("Fetching all technicians from User Service: {}", url);

            UserResponse[] response = restTemplate.getForObject(url, UserResponse[].class);

            if (response == null) {
                log.warn("No technicians received from User Service");
                return List.of();
            }

            log.info("Successfully fetched {} technicians", response.length);
            return List.of(response);

        } catch (Exception e) {
            log.error("Failed to fetch technicians from User Service", e);
            return List.of();
        }
    }

    // =====================================================
    // EVENT PUBLISHING METHODS
    // =====================================================

    private void publishMaintenanceCreatedEvent(MaintenanceRequest request, UserResponse user) {
        MaintenanceCreatedEvent event = MaintenanceCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .requestId(request.getId())
                .userId(user.getId())
                .userName(user.getFullName())
                .userEmail(user.getEmail())
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory().toString())
                .priority(request.getPriority().toString())
                .status(request.getStatus().toString())
                .imageUrl(request.getImageUrl())
                .eventType("MAINTENANCE_CREATED")
                .timestamp(Instant.now())
                .build();

        eventPublisher.publishMaintenanceCreatedEvent(event);
    }

    private void publishMaintenanceAssignedEvent(MaintenanceRequest request, Long technicianId) {
        try {
            UserResponse user = getUserDetails(request.getUserId());
            UserResponse technician = getUserDetails(technicianId);

            MaintenanceAssignedEvent event = MaintenanceAssignedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .requestId(request.getId())
                    .userId(user.getId())
                    .userName(user.getFullName())
                    .userEmail(user.getEmail())
                    .technicianId(technician.getId())
                    .technicianName(technician.getFullName())
                    .technicianEmail(technician.getEmail())
                    .title(request.getTitle())
                    .category(request.getCategory().toString())
                    .priority(request.getPriority().toString())
                    .status(request.getStatus().toString())
                    .eventType("MAINTENANCE_ASSIGNED")
                    .timestamp(Instant.now())
                    .build();

            eventPublisher.publishMaintenanceAssignedEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish MaintenanceAssignedEvent: {}", e.getMessage(), e);
        }
    }

    private void publishMaintenanceStatusChangedEvent(MaintenanceRequest request, Status oldStatus, Status newStatus) {
        try {
            UserResponse user = getUserDetails(request.getUserId());

            MaintenanceStatusChangedEvent event = MaintenanceStatusChangedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .requestId(request.getId())
                    .userId(user.getId())
                    .userName(user.getFullName())
                    .userEmail(user.getEmail())
                    .technicianId(request.getAssignedTo())
                    .title(request.getTitle())
                    .category(request.getCategory().toString())
                    .oldStatus(oldStatus.toString())
                    .newStatus(newStatus.toString())
                    .priority(request.getPriority().toString())
                    .eventType("MAINTENANCE_STATUS_CHANGED")
                    .timestamp(Instant.now())
                    .build();

            eventPublisher.publishMaintenanceStatusChangedEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish MaintenanceStatusChangedEvent: {}", e.getMessage(), e);
        }
    }

    private void publishMaintenanceCompletedEvent(MaintenanceRequest request) {
        try {
            UserResponse user = getUserDetails(request.getUserId());
            
            String technicianName = null;
            if (request.getAssignedTo() != null) {
                try {
                    UserResponse technician = getUserDetails(request.getAssignedTo());
                    technicianName = technician.getFullName();
                } catch (Exception e) {
                    log.warn("Failed to get technician name for ID {}", request.getAssignedTo());
                }
            }

            MaintenanceCompletedEvent event = MaintenanceCompletedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .requestId(request.getId())
                    .itemId(request.getItemId())
                    .userId(user.getId())
                    .userName(user.getFullName())
                    .userEmail(user.getEmail())
                    .technicianId(request.getAssignedTo())
                    .technicianName(technicianName)
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .category(request.getCategory().toString())
                    .priority(request.getPriority().toString())
                    .requestType(request.getRequestType() != null ? request.getRequestType().toString() : "AD_HOC")
                    .completedAt(request.getResolvedAt())
                    .eventType("MAINTENANCE_COMPLETED")
                    .timestamp(Instant.now())
                    .build();

            eventPublisher.publishMaintenanceCompletedEvent(event);
            log.info("📤 MaintenanceCompletedEvent published for requestId={}, itemId={}", 
                    request.getId(), request.getItemId());
        } catch (Exception e) {
            log.error("Failed to publish MaintenanceCompletedEvent: {}", e.getMessage(), e);
        }
    }
    
    /**
     * ISSUE-002 FIX: Validate that Item exists and belongs to the user
     * Prevents referential integrity violations and cross-user item linking
     */
    private void validateItemOwnership(Long itemId, Long userId) {
        try {
            String url = "http://localhost:8082/api/items/" + itemId;
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(createHeaders(userId)),
                Map.class
            );
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalArgumentException("Item not found or access denied: " + itemId);
            }
            
            // Verify item belongs to user
            Map<String, Object> itemData = response.getBody();
            if (itemData == null) {
                throw new IllegalArgumentException("Item not found: " + itemId);
            }
            
            Object itemUserId = itemData.get("userId");
            if (itemUserId == null || !itemUserId.toString().equals(userId.toString())) {
                log.warn("User {} attempted to link item {} owned by user {}", userId, itemId, itemUserId);
                throw new IllegalArgumentException("Cannot link maintenance request to item owned by another user");
            }
            
            log.info("✅ Validated item {} belongs to user {}", itemId, userId);
            
        } catch (Exception e) {
            log.error("Item validation failed for itemId={}, userId={}: {}", itemId, userId, e.getMessage());
            throw new IllegalArgumentException("Invalid item reference: " + e.getMessage());
        }
    }
    
    /**
     * Create HTTP headers with userId for authentication
     */
    private HttpHeaders createHeaders(Long userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", userId.toString());
        return headers;
    }
    
    /**
     * Calculate maintenance service amount based on category and priority
     */
    private BigDecimal calculateMaintenanceAmount(MaintenanceRequest request) {
        // Base rates by category
        BigDecimal baseAmount = switch (request.getCategory()) {
            case PLUMBING -> new BigDecimal("75.00");
            case ELECTRICAL -> new BigDecimal("85.00");
            case HVAC -> new BigDecimal("95.00");
            case CLEANING -> new BigDecimal("55.00");
            case SECURITY -> new BigDecimal("80.00");
            case CARPENTRY -> new BigDecimal("70.00");
            case PAINTING -> new BigDecimal("65.00");
            case OTHERS -> new BigDecimal("50.00");
        };
        
        // Priority multiplier
        BigDecimal multiplier = switch (request.getPriority()) {
            case CRITICAL -> new BigDecimal("2.0");
            case HIGH -> new BigDecimal("1.5");
            case MODERATE -> new BigDecimal("1.2");
            case LOW -> new BigDecimal("1.0");
        };
        
        return baseAmount.multiply(multiplier);
    }

}
