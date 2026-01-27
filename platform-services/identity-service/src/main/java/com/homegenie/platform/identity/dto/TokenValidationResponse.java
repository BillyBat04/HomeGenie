package com.homegenie.platform.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token Validation Response (NEW - Platform API only)
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TokenValidationResponse {
    
    private Boolean valid;
    private Long userId;
    private String email;
    private String role;
    private String error;  // Error message if validation fails
}
