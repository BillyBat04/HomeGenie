package com.homegenie.platform.identity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token Validation Request (NEW - Platform API only)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenValidationRequest {
    
    @NotBlank(message = "Token is required")
    private String token;
}
