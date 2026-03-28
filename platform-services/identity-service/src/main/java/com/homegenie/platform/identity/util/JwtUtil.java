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


@Component
@Slf4j
@RequiredArgsConstructor
public class JwtUtil {

    private final RsaKeyConfig rsaKeyConfig;

    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration;

    
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

    
    
    @Deprecated(forRemoval = true)
    public boolean validateToken(String token) {
        
        throw new UnsupportedOperationException(
            "Use Spring Security's JwtDecoder (NimbusJwtDecoder via jwk-set-uri) for validation. " +
            "This method was kept only for compile compatibility and will be removed.");
    }

    public Long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }
}
