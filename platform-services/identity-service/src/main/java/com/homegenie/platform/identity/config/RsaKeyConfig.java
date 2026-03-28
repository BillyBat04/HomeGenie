package com.homegenie.platform.identity.config;

import com.nimbusds.jose.jwk.RSAKey;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

@Slf4j
@Getter
@Configuration
public class RsaKeyConfig {

    @Value("${rsa.private-key-pem:}")
    private String privateKeyPem;

    @Value("${rsa.public-key-pem:}")
    private String publicKeyPem;

    private RSAKey rsaKey;

    @PostConstruct
    public void init() throws Exception {
        if (!privateKeyPem.isBlank() && !publicKeyPem.isBlank()) {
            rsaKey = loadFromPem();
            log.info("RSA key pair loaded from PEM configuration");
        } else {
            rsaKey = generateEphemeral();
            log.warn("RSA_PRIVATE_KEY_PEM / RSA_PUBLIC_KEY_PEM not configured — " +
                     "using generated ephemeral key. All tokens will be invalidated on restart. " +
                     "Set rsa.private-key-pem and rsa.public-key-pem in production.");
        }
    }

    public RSAPublicKey getPublicKey() {
        try {
            return (RSAPublicKey) rsaKey.toPublicKey();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load RSA public key", e);
        }
    }

    public RSAPrivateKey getPrivateKey() {
        try {
            return (RSAPrivateKey) rsaKey.toPrivateKey();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load RSA private key", e);
        }
    }

    

    private RSAKey generateEphemeral() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var keyPair = generator.generateKeyPair();
        return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID(UUID.randomUUID().toString())
                .build();
    }

    private RSAKey loadFromPem() throws Exception {
        
        var privatePem = privateKeyPem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        var publicPem = publicKeyPem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        var decoder = java.util.Base64.getDecoder();
        var keyFactory = java.security.KeyFactory.getInstance("RSA");

        var privateKeySpec = new java.security.spec.PKCS8EncodedKeySpec(decoder.decode(privatePem));
        var publicKeySpec = new java.security.spec.X509EncodedKeySpec(decoder.decode(publicPem));

        var privateKey = (RSAPrivateKey) keyFactory.generatePrivate(privateKeySpec);
        var publicKey = (RSAPublicKey) keyFactory.generatePublic(publicKeySpec);

        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
    }
}
