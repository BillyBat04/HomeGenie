package com.homegenie.maintenanceservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Maintenance request creation")
public class MaintenanceRequestDTO {

    @Schema(description = "Request title", example = "Leaking faucet in kitchen")
    @NotBlank(message = "Title is required")
    private String title;

    @Schema(description = "Detailed description", example = "Water leaking from kitchen sink pipe")
    @NotBlank(message = "Description is required")
    private String description;

    @Schema(description = "Base64 encoded image (optional)", example = "data:image/jpeg;base64,/9j/4AAQ...")
    private String imageBase64;

    @Schema(description = "Optional: Link to specific household item", example = "1")
    private Long itemId;
}