package com.homegenie.maintenanceservice.service;

import com.homegenie.maintenanceservice.dto.ItemRequest;
import com.homegenie.maintenanceservice.dto.ItemResponse;
import com.homegenie.maintenanceservice.dto.ItemSummaryDTO;
import com.homegenie.maintenanceservice.dto.MaintenanceHistoryDTO;
import com.homegenie.maintenanceservice.model.ItemCategory;
import com.homegenie.maintenanceservice.model.ItemStatus;

import java.util.List;
import java.util.Map;

public interface ItemService {

    ItemResponse createItem(Long userId, ItemRequest request);

    ItemResponse updateItem(Long userId, Long itemId, ItemRequest request);

    ItemResponse getItem(Long userId, Long itemId);

    List<ItemSummaryDTO> getUserItems(Long userId, ItemStatus status, ItemCategory category);

    List<ItemSummaryDTO> getItemsDueForMaintenance(Long userId);

    List<MaintenanceHistoryDTO> getItemMaintenanceHistory(Long userId, Long itemId);

    void deleteItem(Long userId, Long itemId);

    void recordMaintenanceCompleted(Long itemId, Long maintenanceRequestId);

    Map<String, Object> getUserItemStatistics(Long userId);

    void handleMaintenanceCompletedEvent(String message);
}
