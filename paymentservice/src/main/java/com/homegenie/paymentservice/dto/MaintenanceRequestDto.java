package com.homegenie.paymentservice.dto;

import lombok.Data;
import java.time.LocalDateTime;


@Data
public class MaintenanceRequestDto {
    private Long id;
    private Long userId;
    private String title;
    private String description;
    private String category;
    private String priority;
    private String status; 
    private String imageUrl;
    private Long assignedTo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    
    public boolean isPending() {
        return "PENDING".equalsIgnoreCase(status);
    }

    
    public boolean hasAssignedTechnician() {
        return assignedTo != null && assignedTo > 0;
    }

    
    public boolean canAcceptPayment() {
        
        
        
        
        return !isPending() && hasAssignedTechnician() 
            && ("IN_PROGRESS".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status));
    }
}
