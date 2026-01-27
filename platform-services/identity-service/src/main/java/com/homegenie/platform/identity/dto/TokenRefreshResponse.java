package com.homegenie.platform.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TokenRefreshResponse {
    
    private String token;        // New JWT access token
    private String refreshToken; // New refresh token (rotated)
    private String tokenType;    // Bearer
    private Long expiresIn;      // Token expiration in milliseconds
}
