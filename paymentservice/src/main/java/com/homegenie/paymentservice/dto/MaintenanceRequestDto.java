package com.homegenie.paymentservice.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * DTO for MaintenanceRequest details from Maintenance Service
 */
@Data
public class MaintenanceRequestDto {
    private Long id;
    private Long userId;
    private String title;
    private String description;
    private String category;
    private String priority;
    private String status; // PENDING, IN_PROGRESS, COMPLETED, REJECTED
    private String imageUrl;
    private Long assignedTo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Check if request is in PENDING status (no technician assigned)
     */
    public boolean isPending() {
        return "PENDING".equalsIgnoreCase(status);
    }

    /**
     * Check if request has assigned technician
     */
    public boolean hasAssignedTechnician() {
        return assignedTo != null && assignedTo > 0;
    }

    /**
     * Check if payment is allowed for this request
     */
    public boolean canAcceptPayment() {
        // Payment only allowed if:
        // 1. Status is not PENDING (has been assigned)
        // 2. Has assigned technician
        // 3. Status is IN_PROGRESS or COMPLETED
        return !isPending() && hasAssignedTechnician() 
            && ("IN_PROGRESS".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status));
    }
}
