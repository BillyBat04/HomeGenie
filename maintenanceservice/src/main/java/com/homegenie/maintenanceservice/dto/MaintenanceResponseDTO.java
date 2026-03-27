package com.homegenie.maintenanceservice.dto;

import com.homegenie.maintenanceservice.model.Category;
import com.homegenie.maintenanceservice.model.PaymentStatus;
import com.homegenie.maintenanceservice.model.Priority;
import com.homegenie.maintenanceservice.model.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Maintenance request response")
public class MaintenanceResponseDTO {
    @Schema(description = "Request ID", example = "1")
    private Long id;
    @Schema(description = "User ID", example = "1")
    private Long userId;
    @Schema(description = "User name", example = "John Doe")
    private String userName;
    @Schema(description = "Request title", example = "Leaking faucet")
    private String title;
    @Schema(description = "Description", example = "Water leaking from sink")
    private String description;
    @Schema(description = "Category", example = "PLUMBING")
    private Category category;
    @Schema(description = "Priority", example = "HIGH")
    private Priority priority;
    @Schema(description = "Status", example = "PENDING")
    private Status status;
    @Schema(description = "Image URL", example = "https://s3.amazonaws.com/...")
    private String imageUrl;
    @Schema(description = "Assigned technician ID", example = "2")
    private Long assignedTo;
    @Schema(description = "Assigned technician name", example = "Jane Smith")
    private String assignedToName;
    @Schema(description = "Created timestamp", example = "2026-01-15T10:30:00")
    private LocalDateTime createdAt;
    @Schema(description = "Updated timestamp", example = "2026-01-15T11:00:00")
    private LocalDateTime updatedAt;
    @Schema(description = "Resolved timestamp", example = "2026-01-15T15:00:00")
    private LocalDateTime resolvedAt;
    @Schema(description = "Admin notes", example = "Assigned to plumber")
    private String adminNotes;

    @Schema(description = "Payment status for COMPLETED requests: PENDING / SUCCESS / FAILED", example = "PENDING")
    private PaymentStatus paymentStatus;
}
