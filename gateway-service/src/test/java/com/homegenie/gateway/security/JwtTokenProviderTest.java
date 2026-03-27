package com.homegenie.gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final String TEST_SECRET =
            "test-secret-key-for-unit-testing-must-be-at-least-32-characters-long";

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(tokenProvider, "jwtSecret", TEST_SECRET);
    }

    private String buildToken(String subject, String userId, String role, long expiryMs) {
        Map<String, Object> claims = new HashMap<>();
        if (userId != null) claims.put("userId", userId);
        if (role != null) claims.put("role", role);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiryMs))
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    void validateToken_validToken_returnsTrue() {
        String token = buildToken("user@example.com", "user-123", "USER", 60_000);
        assertThat(tokenProvider.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_expiredToken_returnsFalse() {
        String token = buildToken("user@example.com", "user-123", "USER", -1_000);
        assertThat(tokenProvider.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_malformedToken_returnsFalse() {
        assertThat(tokenProvider.validateToken("not.a.valid.jwt")).isFalse();
    }

    @Test
    void validateToken_nullToken_returnsFalse() {
        assertThat(tokenProvider.validateToken(null)).isFalse();
    }

    @Test
    void validateToken_wrongSignature_returnsFalse() {
        String token = buildToken("user@example.com", "user-123", "USER", 60_000);
        // tamper the signature section
        String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "invalidsig";
        assertThat(tokenProvider.validateToken(tampered)).isFalse();
    }

    @Test
    void getUserIdFromToken_extractsUserId() {
        String token = buildToken("user@example.com", "user-123", "USER", 60_000);
        assertThat(tokenProvider.getUserIdFromToken(token)).isEqualTo("user-123");
    }

    @Test
    void getRoleFromToken_extractsRole() {
        String token = buildToken("user@example.com", "user-123", "ADMIN", 60_000);
        assertThat(tokenProvider.getRoleFromToken(token)).isEqualTo("ADMIN");
    }

    @Test
    void getEmailFromToken_extractsSubject() {
        String token = buildToken("user@example.com", "user-123", "USER", 60_000);
        assertThat(tokenProvider.getEmailFromToken(token)).isEqualTo("user@example.com");
    }

    @Test
    void getUserIdFromToken_absentClaim_returnsNull() {
        String token = buildToken("user@example.com", null, "USER", 60_000);
        assertThat(tokenProvider.getUserIdFromToken(token)).isNull();
    }
}
