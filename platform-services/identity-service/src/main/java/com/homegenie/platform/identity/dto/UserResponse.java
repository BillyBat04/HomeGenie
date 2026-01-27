package com.homegenie.platform.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User Response DTO
 * ✅ IDENTICAL to User Service UserResponse
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    
    private Long id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String flatNumber;
    private String role;
    private String technicianSpecialty;
    private Boolean active;
}
