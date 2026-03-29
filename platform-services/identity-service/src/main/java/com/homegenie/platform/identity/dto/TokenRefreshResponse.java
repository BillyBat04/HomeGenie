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
    
    private String token;        
    private String refreshToken; 
    private String tokenType;    
    private Long expiresIn;      
}
