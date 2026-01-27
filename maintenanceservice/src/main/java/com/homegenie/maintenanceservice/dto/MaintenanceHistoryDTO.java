package com.homegenie.maintenanceservice.dto;

import com.homegenie.maintenanceservice.model.MaintenanceRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Simplified maintenance history DTO
 * Used in ItemResponse to show maintenance history for an item
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceHistoryDTO {

    private Long requestId;
    private String title;
    private String status;
    private String requestType;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private Long technicianId;
    private String technicianName;

    /**
     * Convert MaintenanceRequest to MaintenanceHistoryDTO
     */
    public static MaintenanceHistoryDTO fromEntity(MaintenanceRequest request) {
        return MaintenanceHistoryDTO.builder()
                .requestId(request.getId())
                .title(request.getTitle())
                .status(request.getStatus().name())
                .requestType(request.getRequestType() != null ? request.getRequestType().name() : null)
                .createdAt(request.getCreatedAt())
                .completedAt(request.getResolvedAt())
                .technicianId(request.getAssignedTo())
                .build();
    }
}
