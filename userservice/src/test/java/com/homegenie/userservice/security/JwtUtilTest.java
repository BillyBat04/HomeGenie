package com.homegenie.userservice.security;

import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JwtUtil} — RS256 token generation.
 *
 * <p>Validation is delegated to Spring Security's NimbusJwtDecoder.
 * These tests only cover the {@code generateToken()} path.
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;

    private static final Long TEST_EXPIRATION = 3_600_000L; // 1 hour
    private static final String TEST_EMAIL    = "test@example.com";
    private static final Long   TEST_USER_ID  = 1L;
    private static final String TEST_ROLE     = "RESIDENT";

    @BeforeEach
    void setUp() throws Exception {
        // Build a real RsaKeyConfig (generates ephemeral keypair) without Spring context
        RsaKeyConfig rsaKeyConfig = new RsaKeyConfig();
        ReflectionTestUtils.setField(rsaKeyConfig, "privateKeyPem", "");
        ReflectionTestUtils.setField(rsaKeyConfig, "publicKeyPem", "");
        rsaKeyConfig.init();

        jwtUtil = new JwtUtil(rsaKeyConfig);
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiration", TEST_EXPIRATION);
    }

    @Test
    void generateToken_returnsValidJwtFormat() {
        String token = jwtUtil.generateToken(TEST_EMAIL, TEST_USER_ID, TEST_ROLE);

        assertNotNull(token);
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT must have exactly three parts");
        assertTrue(token.startsWith("eyJ"), "JWT header must be Base64-encoded JSON");
    }

    @Test
    void generateToken_containsExpectedClaims() throws Exception {
        String token = jwtUtil.generateToken(TEST_EMAIL, TEST_USER_ID, TEST_ROLE);
        SignedJWT parsed = SignedJWT.parse(token);

        assertEquals(TEST_EMAIL, parsed.getJWTClaimsSet().getSubject());
        assertEquals(TEST_USER_ID.longValue(),
                ((Number) parsed.getJWTClaimsSet().getClaim("userId")).longValue());
        assertEquals(TEST_ROLE, parsed.getJWTClaimsSet().getStringClaim("role"));
    }

    @Test
    void generateToken_containsExpirationAndIssuedAt() throws Exception {
        String token = jwtUtil.generateToken(TEST_EMAIL, TEST_USER_ID, TEST_ROLE);
        SignedJWT parsed = SignedJWT.parse(token);

        assertNotNull(parsed.getJWTClaimsSet().getIssueTime());
        assertNotNull(parsed.getJWTClaimsSet().getExpirationTime());
        assertTrue(
            parsed.getJWTClaimsSet().getExpirationTime()
                  .after(parsed.getJWTClaimsSet().getIssueTime()),
            "Expiry must be after issue time"
        );
    }

    @Test
    void generateToken_usesRS256Algorithm() throws Exception {
        String token = jwtUtil.generateToken(TEST_EMAIL, TEST_USER_ID, TEST_ROLE);
        SignedJWT parsed = SignedJWT.parse(token);

        assertEquals("RS256", parsed.getHeader().getAlgorithm().getName());
    }

    @Test
    void generateToken_differentUsers_produceDifferentTokens() {
        String token1 = jwtUtil.generateToken("user1@example.com", 1L, "RESIDENT");
        String token2 = jwtUtil.generateToken("user2@example.com", 2L, "ADMIN");

        assertNotEquals(token1, token2);
    }

    @Test
    void generateToken_sameUserTwice_producesDifferentTokensDueToTime()
            throws InterruptedException {
        String token1 = jwtUtil.generateToken(TEST_EMAIL, TEST_USER_ID, TEST_ROLE);
        Thread.sleep(10);
        String token2 = jwtUtil.generateToken(TEST_EMAIL, TEST_USER_ID, TEST_ROLE);

        assertNotEquals(token1, token2, "Tokens issued at different times must differ");
    }

    @Test
    void generateToken_withSpecialCharactersInEmail() throws Exception {
        String specialEmail = "test+user@example.co.uk";
        String token = jwtUtil.generateToken(specialEmail, TEST_USER_ID, TEST_ROLE);

        SignedJWT parsed = SignedJWT.parse(token);
        assertEquals(specialEmail, parsed.getJWTClaimsSet().getSubject());
    }
}
