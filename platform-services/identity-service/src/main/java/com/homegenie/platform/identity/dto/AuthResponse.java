package com.homegenie.platform.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Auth Response DTO
 * ✅ IDENTICAL to User Service AuthResponse
 * Ensures backward compatibility
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    
    private String token;           // JWT access token
    private String refreshToken;    // Refresh token
    private String tokenType;       // Bearer
    private Long userId;
    private String email;
    private String fullName;
    private String role;
    private Long expiresIn;         // Token expiration in milliseconds
}
