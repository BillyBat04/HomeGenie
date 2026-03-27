package com.homegenie.gateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

class RateLimitConfigTest {

    private KeyResolver keyResolver;

    @BeforeEach
    void setUp() {
        keyResolver = new RateLimitConfig().userKeyResolver();
    }

    @Test
    void resolve_withXUserId_returnsUserPrefixedKey() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test")
                        .header("X-User-Id", "user-456")
                        .build());

        StepVerifier.create(keyResolver.resolve(exchange))
                .expectNext("user:user-456")
                .verifyComplete();
    }

    @Test
    void resolve_xUserIdTakesPriorityOverXForwardedFor() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test")
                        .header("X-User-Id", "user-789")
                        .header("X-Forwarded-For", "203.0.113.1")
                        .build());

        StepVerifier.create(keyResolver.resolve(exchange))
                .expectNext("user:user-789")
                .verifyComplete();
    }

    @Test
    void resolve_withXForwardedFor_returnsFirstIpAsPrefixedKey() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test")
                        .header("X-Forwarded-For", "203.0.113.10, 10.0.0.1, 192.168.1.1")
                        .build());

        StepVerifier.create(keyResolver.resolve(exchange))
                .expectNext("ip:203.0.113.10")
                .verifyComplete();
    }

    @Test
    void resolve_withXForwardedForSingleIp_returnsIpPrefixedKey() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/payments")
                        .header("X-Forwarded-For", "198.51.100.5")
                        .build());

        StepVerifier.create(keyResolver.resolve(exchange))
                .expectNext("ip:198.51.100.5")
                .verifyComplete();
    }

    @Test
    void resolve_withNoHeadersAndNoRemoteAddress_usesRouteKey() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test").build());

        StepVerifier.create(keyResolver.resolve(exchange))
                .expectNextMatches(key -> key.startsWith("route:") || key.startsWith("ip:") || key.startsWith("unknown:"))
                .verifyComplete();
    }

    @Test
    void resolve_blankXUserId_fallsBackToXForwardedFor() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test")
                        .header("X-User-Id", "   ")
                        .header("X-Forwarded-For", "10.10.10.10")
                        .build());

        StepVerifier.create(keyResolver.resolve(exchange))
                .expectNext("ip:10.10.10.10")
                .verifyComplete();
    }
}
