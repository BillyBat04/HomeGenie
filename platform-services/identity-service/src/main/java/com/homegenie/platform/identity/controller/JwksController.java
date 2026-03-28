package com.homegenie.platform.identity.controller;

import com.homegenie.platform.identity.config.RsaKeyConfig;
import com.nimbusds.jose.jwk.JWKSet;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


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
