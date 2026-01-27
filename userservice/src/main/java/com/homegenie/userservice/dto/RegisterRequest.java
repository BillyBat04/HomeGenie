package com.homegenie.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "User registration request")
public class RegisterRequest {

    @Schema(description = "User email address", example = "john@example.com")
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Schema(description = "User password (min 6 characters)", example = "password123")
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @Schema(description = "User full name", example = "John Doe")
    @NotBlank(message = "Full name is required")
    private String fullName;

    @Schema(description = "User phone number", example = "1234567890")
    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @Schema(description = "Flat/apartment number", example = "101A")
    private String flatNumber;

    // Optional: For technician registration
    @Schema(description = "User role", example = "RESIDENT", allowableValues = {"RESIDENT", "TECHNICIAN", "ADMIN"})
    private String role; // RESIDENT, TECHNICIAN, ADMIN
    @Schema(description = "Technician specialty", example = "Plumber")
    private String specialty; // Plumber, Electrician, etc.
}