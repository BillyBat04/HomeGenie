package com.homegenie.platform.identity.service;

import com.homegenie.platform.identity.dto.TokenRefreshResponse;
import com.homegenie.platform.identity.dto.TokenValidationResponse;
import com.homegenie.platform.identity.exception.InvalidRefreshTokenException;
import com.homegenie.platform.identity.model.RefreshToken;
import com.homegenie.platform.identity.model.User;
import com.homegenie.platform.identity.repository.RefreshTokenRepository;
import com.homegenie.platform.identity.repository.UserRepository;
import com.homegenie.platform.identity.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Token Service - JWT and Refresh Token management
 * ✅ IDENTICAL logic to User Service RefreshTokenService
 * 
 * Responsibilities:
 * - Create refresh tokens
 * - Refresh JWT tokens
 * - Revoke tokens
 * - Validate tokens
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

    @Value("${jwt.refresh-token-expiration}") // 7 days
    private Long refreshTokenExpiration;

    /**
     * Create refresh token for user
     * ✅ Compatible with User Service createRefreshToken logic
     */
    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        log.info("Creating refresh token for userId={}", userId);
        
        // Generate unique token
        String token = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .userId(userId)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
                .createdAt(LocalDateTime.now())
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Refresh JWT token using refresh token
     * ✅ Compatible with User Service refresh logic
     * 
     * Token Rotation: Old refresh token is revoked, new one is generated
     */
    @Transactional
    public TokenRefreshResponse refreshToken(String refreshTokenString) {
        log.info("Refreshing token");
        
        // Find refresh token
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenString)
                .orElseThrow(() -> new InvalidRefreshTokenException());

        // Validate refresh token
        if (!refreshToken.isValid()) {
            log.warn("Invalid or expired refresh token attempt: token={}", refreshTokenString);
            throw new InvalidRefreshTokenException("Refresh token is expired or revoked");
        }

        // Get user
        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new InvalidRefreshTokenException("User not found"));

        // Check if user is active
        if (!user.isActive()) {
            log.warn("Refresh token attempt for inactive user: userId={}", user.getId());
            throw new InvalidRefreshTokenException("Account is deactivated");
        }

        // Revoke old refresh token (Token Rotation)
        refreshToken.setRevoked(true);
        refreshToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);

        log.info("Old refresh token revoked (rotation): userId={}", user.getId());

        // Generate new JWT token
        String newAccessToken = jwtUtil.generateToken(
                user.getEmail(),
                user.getId(),
                user.getRole().name()
        );

        // Generate new refresh token
        RefreshToken newRefreshToken = createRefreshToken(user.getId());

        log.info("Token refreshed successfully: userId={}", user.getId());

        return TokenRefreshResponse.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpiration())
                .build();
    }

    /**
     * Revoke refresh token (logout)
     * ✅ Compatible with User Service revoke logic
     */
    @Transactional
    public void revokeRefreshToken(String refreshTokenString) {
        log.info("Revoking refresh token");
        
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenString)
                .orElseThrow(() -> new InvalidRefreshTokenException());

        refreshToken.setRevoked(true);
        refreshToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);

        log.info("Refresh token revoked: userId={}", refreshToken.getUserId());
    }

    /**
     * Validate JWT token — verifies RS256 signature + claims via Spring Security JwtDecoder.
     * Allows other services to delegate token validation to identity-service.
     */
    public TokenValidationResponse validateToken(String token) {
        try {
            org.springframework.security.oauth2.jwt.Jwt decoded = jwtDecoder.decode(token);

            String email = decoded.getSubject();
            Long userId = ((Number) decoded.getClaim("userId")).longValue();
            String role = decoded.getClaimAsString("role");

            // Verify user still exists and is active
            User user = userRepository.findById(userId).orElse(null);
            if (user == null || !user.isActive()) {
                return TokenValidationResponse.builder()
                        .valid(false)
                        .error("User not found or inactive")
                        .build();
            }

            return TokenValidationResponse.builder()
                    .valid(true)
                    .userId(userId)
                    .email(email)
                    .role(role)
                    .build();

        } catch (Exception e) {
            log.error("Token validation error: {}", e.getMessage());
            return TokenValidationResponse.builder()
                    .valid(false)
                    .error("Invalid token")
                    .build();
        }
    }

    /**
     * Revoke all refresh tokens for a user
     * Useful for security scenarios (e.g., password change)
     */
    @Transactional
    public void revokeAllUserTokens(Long userId) {
        log.info("Revoking all tokens for userId={}", userId);
        
        refreshTokenRepository.findByUserIdAndRevokedFalse(userId)
                .forEach(token -> {
                    token.setRevoked(true);
                    token.setRevokedAt(LocalDateTime.now());
                    refreshTokenRepository.save(token);
                });

        log.info("All tokens revoked for userId={}", userId);
    }
}
