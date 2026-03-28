package com.homegenie.platform.identity.util;

import com.homegenie.platform.identity.config.RsaKeyConfig;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;

/**
 * JWT token generator for Identity Platform Service.
 *
 * <p>Signs access tokens with the RSA private key (RS256).  The matching
 * public key is exposed at {@code /.well-known/jwks.json} so that every
 * downstream service can verify tokens without sharing any secret.
 *
 * <p>Validation is delegated entirely to Spring Security's NimbusJwtDecoder
 * (configured in SecurityConfig via jwk-set-uri). This class only generates.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtUtil {

    private final RsaKeyConfig rsaKeyConfig;

    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration;

    /**
     * Generate a signed RS256 JWT access token.
     *
     * <p>The token contains:
     * <ul>
     *   <li>{@code sub}    — user's email address
     *   <li>{@code userId} — internal numeric user id
     *   <li>{@code role}   — RESIDENT / TECHNICIAN / ADMIN
     * </ul>
     */
    public String generateToken(String email, Long userId, String role) {
        try {
            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(email)
                    .claim("userId", userId)
                    .claim("role", role)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusMillis(accessTokenExpiration)))
                    .build();

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(rsaKeyConfig.getRsaKey().getKeyID())
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claims);
            signedJWT.sign(new RSASSASigner(rsaKeyConfig.getPrivateKey()));
            return signedJWT.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate JWT token", e);
        }
    }

    // intentional placeholder so existing callers that still reference validateToken()
    // in tests compile — Spring Security's NimbusJwtDecoder is the real validator.
    @Deprecated(forRemoval = true)
    public boolean validateToken(String token) {
        // Validation is handled by Spring Security. Do not implement custom parsing here.
        throw new UnsupportedOperationException(
            "Use Spring Security's JwtDecoder (NimbusJwtDecoder via jwk-set-uri) for validation. " +
            "This method was kept only for compile compatibility and will be removed.");
    }

    public Long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }
}
