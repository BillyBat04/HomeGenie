package com.homegenie.platform.identity.service;

import com.homegenie.platform.identity.dto.TokenRefreshResponse;
import com.homegenie.platform.identity.dto.TokenValidationResponse;
import com.homegenie.platform.identity.model.RefreshToken;

public interface TokenService {

    RefreshToken createRefreshToken(Long userId);

    TokenRefreshResponse refreshToken(String refreshTokenString);

    void revokeRefreshToken(String refreshTokenString);

    TokenValidationResponse validateToken(String token);

    void revokeAllUserTokens(Long userId);
}
