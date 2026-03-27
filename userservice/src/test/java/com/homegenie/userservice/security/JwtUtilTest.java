package com.homegenie.userservice.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests for JwtUtil
 *
 * Test Coverage:
 * - Token generation
 * - Token validation
 * - Token expiration
 * - Email extraction from token
 * - Invalid token handling
 * - Signature validation
 */
@SuppressWarnings("null")
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private static final String TEST_SECRET = "test-secret-key-must-be-at-least-256-bits-long-for-HS256-algorithm-security-requirements";
    private static final Long TEST_EXPIRATION = 3600000L; // 1 hour
    private static final String TEST_EMAIL = "test@example.com";
    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_ROLE = "RESIDENT";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", TEST_EXPIRATION);
    }

    @Test
    void testGenerateToken_Success() {
        // When
        String token = jwtUtil.generateToken(TEST_EMAIL, TEST_USER_ID, TEST_ROLE);

        // Then
        assertNotNull(token);
        assertTrue(token.length() > 0);
        assertTrue(token.startsWith("eyJ")); // JWT format starts with eyJ

        // Verify token parts (header.payload.signature)
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);
    }

    @Test
    void testValidateToken_ValidToken_ReturnsTrue() {
        // Given
        String token = jwtUtil.generateToken(TEST_EMAIL, TEST_USER_ID, TEST_ROLE);

        // When
        boolean isValid = jwtUtil.validateToken(token);

        // Then
        assertTrue(isValid);
    }

    @Test
    void testValidateToken_InvalidToken_ReturnsFalse() {
        // Given
        String invalidToken = "invalid.token.here";

        // When
        boolean isValid = jwtUtil.validateToken(invalidToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    void testValidateToken_MalformedToken_ReturnsFalse() {
        // Given
        String malformedToken = "not-a-jwt-token";

        // When
        boolean isValid = jwtUtil.validateToken(malformedToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    void testValidateToken_ExpiredToken_ReturnsFalse() {
        // Given - Create JwtUtil with short expiration
        JwtUtil shortExpirationJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(shortExpirationJwtUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(shortExpirationJwtUtil, "expiration", -1000L); // Already expired

        String expiredToken = shortExpirationJwtUtil.generateToken(TEST_EMAIL, TEST_USER_ID, TEST_ROLE);

        // When
        boolean isValid = jwtUtil.validateToken(expiredToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    void testValidateToken_TamperedToken_ReturnsFalse() {
        // Given
        String validToken = jwtUtil.generateToken(TEST_EMAIL, TEST_USER_ID, TEST_ROLE);
        String tamperedToken = validToken.substring(0, validToken.length() - 5) + "AAAAA"; // Tamper with signature

        // When
        boolean isValid = jwtUtil.validateToken(tamperedToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    void testGetEmailFromToken_Success() {
        // Given
        String token = jwtUtil.generateToken(TEST_EMAIL, TEST_USER_ID, TEST_ROLE);

        // When
        String extractedEmail = jwtUtil.getEmailFromToken(token);

        // Then
        assertNotNull(extractedEmail);
        assertEquals(TEST_EMAIL, extractedEmail);
    }

    @Test
    void testGetEmailFromToken_InvalidToken_ThrowsException() {
        // Given
        String invalidToken = "invalid.token.here";

        // When & Then
        assertThrows(Exception.class, () -> {
            jwtUtil.getEmailFromToken(invalidToken);
        });
    }

    @Test
    void testGenerateToken_DifferentUsersSameTime_DifferentTokens() {
        // Given
        String email1 = "user1@example.com";
        String email2 = "user2@example.com";

        // When
        String token1 = jwtUtil.generateToken(email1, 1L, "RESIDENT");
        String token2 = jwtUtil.generateToken(email2, 2L, "ADMIN");

        // Then
        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);
    }

    @Test
    void testGenerateToken_SameUserDifferentTime_DifferentTokens() throws InterruptedException {
        // Given
        Thread.sleep(10); // Small delay to ensure different timestamps

        // When
        String token1 = jwtUtil.generateToken(TEST_EMAIL, TEST_USER_ID, TEST_ROLE);
        Thread.sleep(10);
        String token2 = jwtUtil.generateToken(TEST_EMAIL, TEST_USER_ID, TEST_ROLE);

        // Then
        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);
    }

    @Test
    void testValidateToken_NullToken_ReturnsFalse() {
        // Given
        String nullToken = null;

        // When
        boolean isValid = jwtUtil.validateToken(nullToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    void testValidateToken_EmptyToken_ReturnsFalse() {
        // Given
        String emptyToken = "";

        // When
        boolean isValid = jwtUtil.validateToken(emptyToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    void testValidateToken_TokenWithDifferentSecret_ReturnsFalse() {
        // Given - Generate token with one secret
        String token = jwtUtil.generateToken(TEST_EMAIL, TEST_USER_ID, TEST_ROLE);

        // Create new JwtUtil with different secret
        JwtUtil differentSecretJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(differentSecretJwtUtil, "secret", "different-secret-key-that-is-at-least-256-bits-long-for-testing-purposes-only");
        ReflectionTestUtils.setField(differentSecretJwtUtil, "expiration", TEST_EXPIRATION);

        // When
        boolean isValid = differentSecretJwtUtil.validateToken(token);

        // Then
        assertFalse(isValid);
    }

    @Test
    void testGenerateToken_WithSpecialCharactersInEmail_Success() {
        // Given
        String specialEmail = "test+user@example.co.uk";

        // When
        String token = jwtUtil.generateToken(specialEmail, TEST_USER_ID, TEST_ROLE);
        String extractedEmail = jwtUtil.getEmailFromToken(token);

        // Then
        assertNotNull(token);
        assertEquals(specialEmail, extractedEmail);
    }

    @Test
    void testGenerateToken_WithLongEmail_Success() {
        // Given
        String longEmail = "very.long.email.address.for.testing.purposes@example-domain.com";

        // When
        String token = jwtUtil.generateToken(longEmail, TEST_USER_ID, TEST_ROLE);
        String extractedEmail = jwtUtil.getEmailFromToken(token);

        // Then
        assertNotNull(token);
        assertEquals(longEmail, extractedEmail);
    }
}

