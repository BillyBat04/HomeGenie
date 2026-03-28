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


@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class ItemService {

    private final ItemRepository itemRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final S3Service s3Service;
    private final ObjectMapper objectMapper;

    
    private static final int MAX_DOCUMENT_SIZE_BYTES = 5 * 1024 * 1024; 
    private static final int DEFAULT_MAINTENANCE_FREQUENCY_DAYS = 180; 

    
    @Transactional
    public ItemResponse createItem(Long userId, ItemRequest request) {
        log.info("Creating item '{}' for user {}", request.getName(), userId);

        
        validateItemRequest(request);

        
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

        
        if (request.getWarrantyDocumentBase64() != null && !request.getWarrantyDocumentBase64().isEmpty()) {
            String documentUrl = uploadWarrantyDocument(userId, item.getName(), request.getWarrantyDocumentBase64());
            item.setWarrantyDocumentUrl(documentUrl);
        }

        Item saved = itemRepository.save(item);
        log.info("Created item with ID: {}", saved.getId());

        
        long maintenanceCount = maintenanceRepository.countByItemId(saved.getId());

        return ItemResponse.fromEntity(saved, maintenanceCount);
    }

    
    @Transactional
    public ItemResponse updateItem(Long userId, Long itemId, ItemRequest request) {
        log.info("Updating item {} for user {}", itemId, userId);

        
        Item item = getItemByIdAndUserId(itemId, userId);

        
        validateItemRequest(request);

        
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
            
            item.calculateNextMaintenanceDate();
        }

        
        if (request.getStatus() != null && !request.getStatus().equals(item.getStatus())) {
            updateItemStatus(item, request.getStatus());
        }

        
        if (request.getWarrantyDocumentBase64() != null && !request.getWarrantyDocumentBase64().isEmpty()) {
            String documentUrl = uploadWarrantyDocument(userId, item.getName(), request.getWarrantyDocumentBase64());
            item.setWarrantyDocumentUrl(documentUrl);
        }

        Item updated = itemRepository.save(item);
        log.info("Updated item {}", itemId);

        long maintenanceCount = maintenanceRepository.countByItemId(itemId);
        return ItemResponse.fromEntity(updated, maintenanceCount);
    }

    
    @Transactional(readOnly = true)
    public ItemResponse getItem(Long userId, Long itemId) {
        Item item = getItemByIdAndUserId(itemId, userId);
        long maintenanceCount = maintenanceRepository.countByItemId(itemId);
        return ItemResponse.fromEntity(item, maintenanceCount);
    }

    
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

    
    @Transactional(readOnly = true)
    public List<ItemSummaryDTO> getItemsDueForMaintenance(Long userId) {
        LocalDate today = LocalDate.now();
        List<Item> items = itemRepository.findItemsDueForMaintenance(today);
        
        
        return items.stream()
                .filter(item -> item.getUserId().equals(userId))
                .map(ItemSummaryDTO::fromEntity)
                .collect(Collectors.toList());
    }

    
    @Transactional(readOnly = true)
    public List<MaintenanceHistoryDTO> getItemMaintenanceHistory(Long userId, Long itemId) {
        
        getItemByIdAndUserId(itemId, userId);

        List<MaintenanceRequest> history = maintenanceRepository.findByItemIdOrderByCreatedAtDesc(itemId);
        
        return history.stream()
                .map(MaintenanceHistoryDTO::fromEntity)
                .collect(Collectors.toList());
    }

    
    @Transactional
    public void deleteItem(Long userId, Long itemId) {
        log.info("Deleting item {} for user {}", itemId, userId);
        
        Item item = getItemByIdAndUserId(itemId, userId);
        item.retire();
        itemRepository.save(item);
        
        log.info("Item {} marked as RETIRED", itemId);
    }

    
    @Transactional
    public void recordMaintenanceCompleted(Long itemId, Long maintenanceRequestId) {
        log.info("Recording maintenance completion for item {}, request {}", itemId, maintenanceRequestId);

        
        
        Item item = itemRepository.findByIdWithLock(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));

        
        MaintenanceRequest request = maintenanceRepository.findById(maintenanceRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Maintenance request not found: " + maintenanceRequestId));

        if (request.getStatus() != Status.COMPLETED) {
            log.warn("Attempted to record incomplete maintenance request {} for item {}", maintenanceRequestId, itemId);
            return;
        }

        
        item.recordMaintenanceCompleted();
        
        
        if (item.getStatus() == ItemStatus.UNDER_REPAIR) {
            item.markActive();
        }

        itemRepository.save(item);
        log.info("Recorded maintenance completion for item {}", itemId);
    }

    

    
    private Item getItemByIdAndUserId(Long itemId, Long userId) {
        return itemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Item not found or access denied: " + itemId
                ));
    }

    
    private void validateItemRequest(ItemRequest request) {
        
        if (request.getWarrantyExpiryDate() != null && request.getPurchaseDate() != null) {
            if (request.getWarrantyExpiryDate().isBefore(request.getPurchaseDate())) {
                throw new IllegalArgumentException(
                    "Warranty expiry date cannot be before purchase date"
                );
            }
        }

        
        if (request.getWarrantyDocumentBase64() != null && !request.getWarrantyDocumentBase64().isEmpty()) {
            validateWarrantyDocumentSize(request.getWarrantyDocumentBase64());
        }
    }

    
    private void validateWarrantyDocumentSize(String base64Data) {
        try {
            
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

    
    private void updateItemStatus(Item item, ItemStatus newStatus) {
        ItemStatus currentStatus = item.getStatus();
        
        
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
                
                item.setStatus(ItemStatus.INACTIVE);
                log.info("Item {} status changed: {} -> INACTIVE", item.getId(), currentStatus);
                break;
        }
    }

    
    @Transactional(readOnly = true)
    public Map<String, Object> getUserItemStatistics(Long userId) {
        Map<String, Object> stats = new java.util.HashMap<>();
        
        
        Map<String, Long> statusCounts = new java.util.HashMap<>();
        for (ItemStatus status : ItemStatus.values()) {
            long count = itemRepository.countByUserIdAndStatus(userId, status);
            statusCounts.put(status.name(), count);
        }
        stats.put("byStatus", statusCounts);
        
        
        Map<String, Long> categoryCounts = new java.util.HashMap<>();
        for (ItemCategory category : ItemCategory.values()) {
            long count = itemRepository.countByUserIdAndCategory(userId, category);
            categoryCounts.put(category.name(), count);
        }
        stats.put("byCategory", categoryCounts);
        
        
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

    

    
    @KafkaListener(
        topics = "${kafka.topics.maintenance-events:maintenance-events}",
        groupId = "item-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleMaintenanceCompletedEvent(String message) {
        try {
            log.info("📥 Received Kafka message: {}", message);
            
            
            MaintenanceCompletedEvent event = objectMapper.readValue(message, MaintenanceCompletedEvent.class);
            
            
            if (!"MAINTENANCE_COMPLETED".equals(event.getEventType())) {
                log.debug("⏭️ Skipping non-MAINTENANCE_COMPLETED event: {}", event.getEventType());
                return;
            }
            
            log.info("🔔 Processing MaintenanceCompletedEvent: requestId={}, itemId={}", 
                    event.getRequestId(), event.getItemId());
            
            
            if (event.getItemId() == null) {
                log.info("⏭️ Skipping - no item linked to maintenance request {}", event.getRequestId());
                return;
            }
            
            
            recordMaintenanceCompleted(event.getItemId(), event.getRequestId());
            
            log.info("✅ Successfully processed MaintenanceCompletedEvent for item {}", event.getItemId());
            
        } catch (Exception e) {
            log.error("❌ Failed to process MaintenanceCompletedEvent: {}", e.getMessage(), e);
            
            
        }
    }
}
