package com.homegenie.maintenanceservice.dto;

import com.homegenie.maintenanceservice.model.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Update maintenance request")
public class UpdateRequestDTO {
    @Schema(description = "New status", example = "IN_PROGRESS", allowableValues = {"PENDING", "IN_PROGRESS", "COMPLETED", "CANCELLED"})
    private Status status;
    @Schema(description = "Assign to technician ID", example = "2")
    private Long assignedTo;
    @Schema(description = "Admin notes", example = "Assigned to plumber")
    private String adminNotes;
}
