package com.homegenie.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
@Schema(description = "User profile creation request (internal, from identity-service)")
public class CreateUserRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "User email address", example = "john@example.com")
    private String email;

    @NotBlank(message = "Full name is required")
    @Schema(description = "Full name", example = "John Doe")
    private String fullName;

    @Schema(description = "Phone number", example = "0901234567")
    private String phoneNumber;

    @Schema(description = "Flat/unit number", example = "A-101")
    private String flatNumber;

    @Schema(description = "User role: RESIDENT, TECHNICIAN, or ADMIN", example = "RESIDENT")
    private String role;

    @Schema(description = "Technician specialty (TECHNICIAN role only)", example = "Plumber")
    private String specialty;
}
