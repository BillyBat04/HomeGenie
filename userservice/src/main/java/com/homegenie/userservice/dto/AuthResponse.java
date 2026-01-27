package com.homegenie.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Authentication response with JWT token")
public class AuthResponse {
    @Schema(description = "JWT authentication token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;
    @Schema(description = "Refresh token for obtaining new access tokens", example = "550e8400-e29b-41d4-a716-446655440000")
    private String refreshToken;
    @Schema(description = "User email", example = "john@example.com")
    private String email;
    @Schema(description = "User full name", example = "John Doe")
    private String fullName;
    @Schema(description = "User role", example = "RESIDENT")
    private String role;
    @Schema(description = "User ID", example = "1")
    private Long userId;
    @Schema(description = "Technician specialty", example = "Plumber")
    private String specialty;
}
