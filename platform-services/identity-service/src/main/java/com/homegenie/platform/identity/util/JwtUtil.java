package com.homegenie.platform.identity.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 * JWT Utility Class
 * IDENTICAL to User Service JwtUtil
 * 
 * Generates and validates JWT tokens with same secret and algorithm
 * Ensures token compatibility between Identity Service and User Service
 */
@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}") // 24 hours
    private Long accessTokenExpiration;
    
    /**
     * SECURITY: Validate JWT secret on startup
     * Prevents application from starting with missing or weak secret
     */
    @PostConstruct
    public void validateConfiguration() {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException(
                "JWT_SECRET environment variable must be set. " +
                "Generate a secure 256-bit key and set it as environment variable."
            );
        }
        
        if (secret.length() < 32) {
            throw new IllegalStateException(
                "JWT_SECRET must be at least 256 bits (32 characters). " +
                "Current length: " + secret.length() + " characters. " +
                "Use a cryptographically secure random key."
            );
        }
        
        // Warn about common weak patterns
        if (secret.contains("CHANGE_THIS") || secret.contains("SECRET_KEY") || 
            secret.contains("your-secret-key")) {
            log.warn("JWT_SECRET contains default/example value. " +
                "Replace with a unique secure key in production!");
        }
        
        log.info("JWT configuration validated successfully");
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Generate JWT access token
     * IDENTICAL signature to User Service
     */
    public String generateToken(String email, Long userId, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .setSubject(email)
                .claim("userId", userId)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extract email from JWT token
     */
    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    /**
     * Extract userId from JWT token
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("userId", Long.class);
    }

    /**
     * Extract role from JWT token
     */
    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("role", String.class);
    }

    /**
     * Validate JWT token
     * ✅ IDENTICAL logic to User Service
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT token expired: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.error("JWT token unsupported: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.error("JWT token malformed: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            log.error("JWT signature invalid: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.error("JWT token invalid: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.error("JWT validation error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get token expiration time
     */
    public Long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }
}
