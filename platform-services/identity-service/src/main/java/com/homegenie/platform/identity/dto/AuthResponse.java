package com.homegenie.platform.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    
    private String token;           
    private String refreshToken;    
    private String tokenType;       
    private Long userId;
    private String email;
    private String fullName;
    private String role;
    private Long expiresIn;         
}
