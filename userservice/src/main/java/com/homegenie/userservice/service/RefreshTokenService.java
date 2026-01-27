package com.homegenie.userservice.service;

import com.homegenie.userservice.model.RefreshToken;
import com.homegenie.userservice.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration:604800000}") // 7 days default
    private Long refreshTokenExpirationMs;

    /**
     * Generate new refresh token for user
     */
    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        // Generate unique token
        String token = UUID.randomUUID().toString();
        
        LocalDateTime expiresAt = LocalDateTime.now()
            .plusSeconds(refreshTokenExpirationMs / 1000);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .userId(userId)
                .expiresAt(expiresAt)
                .createdAt(LocalDateTime.now())
                .revoked(false)
                .build();

        log.info("Creating refresh token for user: {}", userId);
        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Find and validate refresh token
     */
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    /**
     * Verify refresh token validity
     */
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            log.warn("Refresh token expired: {}", token.getToken());
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token expired. Please login again");
        }
        
        if (token.isRevoked()) {
            log.warn("Refresh token revoked: {}", token.getToken());
            throw new RuntimeException("Refresh token revoked. Please login again");
        }

        return token;
    }

    /**
     * Revoke refresh token
     */
    @Transactional
    public void revokeToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshToken -> {
            refreshToken.setRevoked(true);
            refreshToken.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(refreshToken);
            log.info("Refresh token revoked: {}", token);
        });
    }

    /**
     * Revoke all refresh tokens for user
     */
    @Transactional
    public void revokeAllUserTokens(Long userId) {
        var tokens = refreshTokenRepository.findByUserIdAndRevokedFalse(userId);
        LocalDateTime now = LocalDateTime.now();
        
        tokens.forEach(token -> {
            token.setRevoked(true);
            token.setRevokedAt(now);
        });
        
        refreshTokenRepository.saveAll(tokens);
        log.info("Revoked {} refresh tokens for user: {}", tokens.size(), userId);
    }

    /**
     * Delete expired tokens (cleanup job)
     */
    @Transactional
    public void deleteExpiredTokens() {
        var allTokens = refreshTokenRepository.findAll();
        var expiredTokens = allTokens.stream()
                .filter(RefreshToken::isExpired)
                .toList();
        
        refreshTokenRepository.deleteAll(expiredTokens);
        log.info("Deleted {} expired refresh tokens", expiredTokens.size());
    }
}
