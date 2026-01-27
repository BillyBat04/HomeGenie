package com.homegenie.maintenanceservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homegenie.maintenanceservice.dto.*;
import com.homegenie.maintenanceservice.dto.event.MaintenanceCompletedEvent;
import com.homegenie.maintenanceservice.model.*;
import com.homegenie.maintenanceservice.repository.ItemRepository;
import com.homegenie.maintenanceservice.repository.MaintenanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service layer for Item management
 * Handles business logic, validation, and orchestration
 * 
 * Architecture: Item is the Aggregate Root
 * - Only ItemService can modify Item state
 * - MaintenanceService publishes events → ItemService consumes
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ItemService {

    private final ItemRepository itemRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final S3Service s3Service;
    private final ObjectMapper objectMapper;

    // Constants for validation
    private static final int MAX_DOCUMENT_SIZE_BYTES = 5 * 1024 * 1024; // 5MB
    private static final int DEFAULT_MAINTENANCE_FREQUENCY_DAYS = 180; // 6 months

    /**
     * Create a new item for a user
     * Validates input, uploads warranty document to S3 if provided
     */
    @Transactional
    public ItemResponse createItem(Long userId, ItemRequest request) {
        log.info("Creating item '{}' for user {}", request.getName(), userId);

        // Validate business rules
        validateItemRequest(request);

        // Build entity
        Item item = Item.builder()
                .userId(userId)
                .name(request.getName())
                .category(request.getCategory())
                .brand(request.getBrand())
                .model(request.getModel())
                .purchaseDate(request.getPurchaseDate())
                .warrantyExpiryDate(request.getWarrantyExpiryDate())
                .maintenanceFrequencyDays(
                    request.getMaintenanceFrequencyDays() != null 
                        ? request.getMaintenanceFrequencyDays() 
                        : DEFAULT_MAINTENANCE_FREQUENCY_DAYS
                )
                .status(ItemStatus.ACTIVE)
                .location(request.getLocation())
                .notes(request.getNotes())
                .build();

        // Upload warranty document if provided
        if (request.getWarrantyDocumentBase64() != null && !request.getWarrantyDocumentBase64().isEmpty()) {
            String documentUrl = uploadWarrantyDocument(userId, item.getName(), request.getWarrantyDocumentBase64());
            item.setWarrantyDocumentUrl(documentUrl);
        }

        Item saved = itemRepository.save(item);
        log.info("Created item with ID: {}", saved.getId());

        // Get maintenance count (should be 0 for new item)
        long maintenanceCount = maintenanceRepository.countByItemId(saved.getId());

        return ItemResponse.fromEntity(saved, maintenanceCount);
    }

    /**
     * Update an existing item
     * Validates ownership and business rules
     */
    @Transactional
    public ItemResponse updateItem(Long userId, Long itemId, ItemRequest request) {
        log.info("Updating item {} for user {}", itemId, userId);

        // Validate ownership
        Item item = getItemByIdAndUserId(itemId, userId);

        // Validate business rules
        validateItemRequest(request);

        // Update fields
        item.setName(request.getName());
        item.setCategory(request.getCategory());
        item.setBrand(request.getBrand());
        item.setModel(request.getModel());
        item.setPurchaseDate(request.getPurchaseDate());
        item.setWarrantyExpiryDate(request.getWarrantyExpiryDate());
        item.setLocation(request.getLocation());
        item.setNotes(request.getNotes());

        if (request.getMaintenanceFrequencyDays() != null) {
            item.setMaintenanceFrequencyDays(request.getMaintenanceFrequencyDays());
            // Recalculate next maintenance date with new frequency
            item.calculateNextMaintenanceDate();
        }

        // Update status if provided (optional status transitions)
        if (request.getStatus() != null && !request.getStatus().equals(item.getStatus())) {
            updateItemStatus(item, request.getStatus());
        }

        // Upload new warranty document if provided
        if (request.getWarrantyDocumentBase64() != null && !request.getWarrantyDocumentBase64().isEmpty()) {
            String documentUrl = uploadWarrantyDocument(userId, item.getName(), request.getWarrantyDocumentBase64());
            item.setWarrantyDocumentUrl(documentUrl);
        }

        Item updated = itemRepository.save(item);
        log.info("Updated item {}", itemId);

        long maintenanceCount = maintenanceRepository.countByItemId(itemId);
        return ItemResponse.fromEntity(updated, maintenanceCount);
    }

    /**
     * Get item by ID with ownership validation
     */
    @Transactional(readOnly = true)
    public ItemResponse getItem(Long userId, Long itemId) {
        Item item = getItemByIdAndUserId(itemId, userId);
        long maintenanceCount = maintenanceRepository.countByItemId(itemId);
        return ItemResponse.fromEntity(item, maintenanceCount);
    }

    /**
     * Get all items for a user with optional filters
     */
    @Transactional(readOnly = true)
    public List<ItemSummaryDTO> getUserItems(Long userId, ItemStatus status, ItemCategory category) {
        log.info("Getting items for user {} with filters - status: {}, category: {}", userId, status, category);

        List<Item> items;

        if (status != null && category != null) {
            items = itemRepository.findByUserIdAndStatusAndCategory(userId, status, category);
        } else if (status != null) {
            items = itemRepository.findByUserIdAndStatus(userId, status);
        } else if (category != null) {
            items = itemRepository.findByUserIdAndCategory(userId, category);
        } else {
            items = itemRepository.findByUserId(userId);
        }

        return items.stream()
                .map(ItemSummaryDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get items that need maintenance (due now or overdue)
     */
    @Transactional(readOnly = true)
    public List<ItemSummaryDTO> getItemsDueForMaintenance(Long userId) {
        LocalDate today = LocalDate.now();
        List<Item> items = itemRepository.findItemsDueForMaintenance(today);
        
        // Filter by userId
        return items.stream()
                .filter(item -> item.getUserId().equals(userId))
                .map(ItemSummaryDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get maintenance history for an item
     */
    @Transactional(readOnly = true)
    public List<MaintenanceHistoryDTO> getItemMaintenanceHistory(Long userId, Long itemId) {
        // Validate ownership
        getItemByIdAndUserId(itemId, userId);

        List<MaintenanceRequest> history = maintenanceRepository.findByItemIdOrderByCreatedAtDesc(itemId);
        
        return history.stream()
                .map(MaintenanceHistoryDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Delete an item (soft delete by marking as RETIRED)
     */
    @Transactional
    public void deleteItem(Long userId, Long itemId) {
        log.info("Deleting item {} for user {}", itemId, userId);
        
        Item item = getItemByIdAndUserId(itemId, userId);
        item.retire();
        itemRepository.save(item);
        
        log.info("Item {} marked as RETIRED", itemId);
    }

    /**
     * Record maintenance completion for an item
     * Called by event consumer when MaintenanceCompletedEvent is received
     * 
     * CRITICAL: This is the ONLY way maintenance completion should update Item
     */
    @Transactional
    public void recordMaintenanceCompleted(Long itemId, Long maintenanceRequestId) {
        log.info("Recording maintenance completion for item {}, request {}", itemId, maintenanceRequestId);

        // CRITICAL FIX: Use pessimistic lock to prevent race conditions
        // Multiple scheduler threads or concurrent events could try to update same item
        Item item = itemRepository.findByIdWithLock(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));

        // Verify the maintenance request exists and is completed
        MaintenanceRequest request = maintenanceRepository.findById(maintenanceRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Maintenance request not found: " + maintenanceRequestId));

        if (request.getStatus() != Status.COMPLETED) {
            log.warn("Attempted to record incomplete maintenance request {} for item {}", maintenanceRequestId, itemId);
            return;
        }

        // Update item state (only Item can modify itself - Aggregate Root pattern)
        item.recordMaintenanceCompleted();
        
        // If item was under repair, mark as active
        if (item.getStatus() == ItemStatus.UNDER_REPAIR) {
            item.markActive();
        }

        itemRepository.save(item);
        log.info("Recorded maintenance completion for item {}", itemId);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Get item by ID and validate user ownership
     * Throws exception if not found or user doesn't own the item
     */
    private Item getItemByIdAndUserId(Long itemId, Long userId) {
        return itemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Item not found or access denied: " + itemId
                ));
    }

    /**
     * Validate business rules for ItemRequest
     */
    private void validateItemRequest(ItemRequest request) {
        // Warranty expiry must be after purchase date
        if (request.getWarrantyExpiryDate() != null && request.getPurchaseDate() != null) {
            if (request.getWarrantyExpiryDate().isBefore(request.getPurchaseDate())) {
                throw new IllegalArgumentException(
                    "Warranty expiry date cannot be before purchase date"
                );
            }
        }

        // Validate warranty document size if provided
        if (request.getWarrantyDocumentBase64() != null && !request.getWarrantyDocumentBase64().isEmpty()) {
            validateWarrantyDocumentSize(request.getWarrantyDocumentBase64());
        }
    }

    /**
     * Validate warranty document size (decode base64 and check bytes)
     */
    private void validateWarrantyDocumentSize(String base64Data) {
        try {
            // Remove data URI prefix if present (e.g., "data:image/png;base64,")
            String base64 = base64Data;
            if (base64Data.contains(",")) {
                base64 = base64Data.split(",")[1];
            }

            byte[] decoded = Base64.getDecoder().decode(base64);
            
            if (decoded.length > MAX_DOCUMENT_SIZE_BYTES) {
                throw new IllegalArgumentException(
                    String.format("Warranty document too large: %d bytes (max %d bytes)", 
                        decoded.length, MAX_DOCUMENT_SIZE_BYTES)
                );
            }
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("Warranty document too large")) {
                throw e;
            }
            throw new IllegalArgumentException("Invalid base64 warranty document");
        }
    }

    /**
     * Upload warranty document to S3
     */
    private String uploadWarrantyDocument(Long userId, String itemName, String base64Data) {
        try {
            log.info("Uploading warranty document for item '{}' (user {})", itemName, userId);
            S3Service.ImageUploadResult result = s3Service.uploadImage(base64Data);
            String url = result.getUrl();
            log.info("Warranty document uploaded successfully to {}", result.getStorageType());
            return url;
        } catch (Exception e) {
            log.error("Failed to upload warranty document for item '{}'", itemName, e);
            throw new RuntimeException("Failed to upload warranty document: " + e.getMessage(), e);
        }
    }

    /**
     * Update item status with validation
     * Uses domain methods to enforce state transitions
     */
    private void updateItemStatus(Item item, ItemStatus newStatus) {
        ItemStatus currentStatus = item.getStatus();
        
        // Use domain methods for state transitions
        switch (newStatus) {
            case ACTIVE:
                item.markActive();
                log.info("Item {} status changed: {} -> ACTIVE", item.getId(), currentStatus);
                break;
            case UNDER_REPAIR:
                item.markUnderRepair();
                log.info("Item {} status changed: {} -> UNDER_REPAIR", item.getId(), currentStatus);
                break;
            case RETIRED:
                item.retire();
                log.info("Item {} status changed: {} -> RETIRED", item.getId(), currentStatus);
                break;
            case INACTIVE:
                // Direct status change (no specific domain method)
                item.setStatus(ItemStatus.INACTIVE);
                log.info("Item {} status changed: {} -> INACTIVE", item.getId(), currentStatus);
                break;
        }
    }

    /**
     * Get item statistics for a user
     * Returns counts by status and category
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserItemStatistics(Long userId) {
        Map<String, Object> stats = new java.util.HashMap<>();
        
        // Count by status
        Map<String, Long> statusCounts = new java.util.HashMap<>();
        for (ItemStatus status : ItemStatus.values()) {
            long count = itemRepository.countByUserIdAndStatus(userId, status);
            statusCounts.put(status.name(), count);
        }
        stats.put("byStatus", statusCounts);
        
        // Count by category
        Map<String, Long> categoryCounts = new java.util.HashMap<>();
        for (ItemCategory category : ItemCategory.values()) {
            long count = itemRepository.countByUserIdAndCategory(userId, category);
            categoryCounts.put(category.name(), count);
        }
        stats.put("byCategory", categoryCounts);
        
        // Items needing attention
        LocalDate today = LocalDate.now();
        List<Item> allItems = itemRepository.findByUserId(userId);
        
        long needsMaintenance = allItems.stream().filter(Item::needsMaintenance).count();
        long maintenanceDueSoon = allItems.stream().filter(Item::maintenanceDueSoon).count();
        long warrantyExpiring = allItems.stream().filter(Item::isWarrantyExpiring).count();
        
        Map<String, Long> alerts = new java.util.HashMap<>();
        alerts.put("needsMaintenance", needsMaintenance);
        alerts.put("maintenanceDueSoon", maintenanceDueSoon);
        alerts.put("warrantyExpiring", warrantyExpiring);
        stats.put("alerts", alerts);
        
        return stats;
    }

    // ==================== KAFKA EVENT CONSUMER ====================

    /**
     * Kafka listener for MaintenanceCompletedEvent
     * Automatically updates item maintenance dates when maintenance is completed
     * 
     * This is the event-driven integration between MaintenanceService and ItemService
     * Enforces Aggregate Root pattern: Only ItemService can update Item state
     */
    @KafkaListener(
        topics = "${kafka.topics.maintenance-events:maintenance-events}",
        groupId = "item-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleMaintenanceCompletedEvent(String message) {
        try {
            log.info("📥 Received Kafka message: {}", message);
            
            // Parse the event
            MaintenanceCompletedEvent event = objectMapper.readValue(message, MaintenanceCompletedEvent.class);
            
            // Check if event is MAINTENANCE_COMPLETED type
            if (!"MAINTENANCE_COMPLETED".equals(event.getEventType())) {
                log.debug("⏭️ Skipping non-MAINTENANCE_COMPLETED event: {}", event.getEventType());
                return;
            }
            
            log.info("🔔 Processing MaintenanceCompletedEvent: requestId={}, itemId={}", 
                    event.getRequestId(), event.getItemId());
            
            // If no item linked, skip
            if (event.getItemId() == null) {
                log.info("⏭️ Skipping - no item linked to maintenance request {}", event.getRequestId());
                return;
            }
            
            // Update the item
            recordMaintenanceCompleted(event.getItemId(), event.getRequestId());
            
            log.info("✅ Successfully processed MaintenanceCompletedEvent for item {}", event.getItemId());
            
        } catch (Exception e) {
            log.error("❌ Failed to process MaintenanceCompletedEvent: {}", e.getMessage(), e);
            // Don't rethrow - we don't want Kafka to retry indefinitely
            // Consider adding dead letter queue in production
        }
    }
}
