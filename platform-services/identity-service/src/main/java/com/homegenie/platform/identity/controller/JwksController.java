package com.homegenie.platform.identity.controller;

import com.homegenie.platform.identity.config.RsaKeyConfig;
import com.nimbusds.jose.jwk.JWKSet;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Exposes the JSON Web Key Set so every downstream service can fetch the
 * RSA public key and verify JWT signatures without sharing a secret.
 *
 * <p>Spring Security's {@code jwk-set-uri} property points here, e.g.:
 * <pre>
 * spring.security.oauth2.resourceserver.jwt.jwk-set-uri=
 *   http://identity-service:8086/.well-known/jwks.json
 * </pre>
 */
@RestController
@RequiredArgsConstructor
public class JwksController {

    private final RsaKeyConfig rsaKeyConfig;

    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> jwks() {
        JWKSet jwkSet = new JWKSet(rsaKeyConfig.getRsaKey().toPublicJWK());
        return jwkSet.toJSONObject();
    }
}
