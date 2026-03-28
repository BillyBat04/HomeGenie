package com.homegenie.userservice.security;

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
 * JWT token generator for UserService (RS256).
 *
 * <p>Signs tokens with the RSA private key managed by {@link RsaKeyConfig}.
 * Validation is delegated to Spring Security's NimbusJwtDecoder via
 * {@code spring.security.oauth2.resourceserver.jwt.jwk-set-uri}.
 * This class only generates — it never parses or validates.
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
     * <p>Claims:
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
}
