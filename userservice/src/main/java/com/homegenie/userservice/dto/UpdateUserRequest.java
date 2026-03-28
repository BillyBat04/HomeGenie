package com.homegenie.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "Request to update user profile information")
public class UpdateUserRequest {

    @NotBlank(message = "Full name is required")
    @Schema(description = "Full name of the user", example = "John Doe")
    private String fullName;

    @Pattern(regexp = "^[0-9+\\-\\s()]{7,20}$", message = "Invalid phone number format")
    @Schema(description = "Phone number", example = "0901234567")
    private String phoneNumber;

    @Schema(description = "Flat/unit number", example = "A-101")
    private String flatNumber;

    @Schema(description = "Technician specialty (TECHNICIAN role only)", example = "Plumber")
    private String specialty;

    @Schema(description = "Whether user wants email notifications", example = "true")
    private Boolean emailNotificationsEnabled;
}
