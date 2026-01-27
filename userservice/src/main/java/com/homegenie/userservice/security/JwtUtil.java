package com.homegenie.userservice.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}") // 24 hours
    private Long expiration;
    
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
        
        // Warn about common weak patterns (but don't block - allow override)
        if (secret.contains("CHANGE_THIS") || secret.contains("SECRET_KEY")) {
            throw new IllegalStateException(
                "JWT_SECRET contains default/example value. " +
                "This is a critical security vulnerability. " +
                "Generate a unique secure key for production."
            );
        }
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String email, Long userId, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setSubject(email)
                .claim("userId", userId)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}