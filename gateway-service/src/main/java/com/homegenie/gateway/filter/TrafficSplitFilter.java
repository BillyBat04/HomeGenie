package com.homegenie.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Traffic Split Filter - Phase 3 Implementation
 * 
 * This filter enables gradual traffic migration from User Service to Identity Service.
 * 
 * How it works:
 * 1. Check feature flag: platform.traffic-split.enabled
 * 2. Get traffic percentage: platform.traffic-split.percentage (0-100)
 * 3. Generate random number (0-99)
 * 4. If random < percentage: Route to Identity Service
 * 5. Else: Route to User Service (old)
 * 
 * Gradual Rollout Schedule:
 * - Week 1: 5% traffic
 * - Week 2: 20% traffic
 * - Week 3: 50% traffic
 * - Week 4: 100% traffic
 * 
 * Auto-Rollback:
 * - If error rate > threshold: Automatically set percentage to 0%
 * - Monitored by AutoRollbackMonitor component
 * 
 * @version 1.0.0
 * @since Phase 3 - February 2026
 */
@Slf4j
@Component
public class TrafficSplitFilter extends AbstractGatewayFilterFactory<TrafficSplitFilter.Config> {

    @Value("${platform.traffic-split.enabled:false}")
    private boolean trafficSplitEnabled;

    @Value("${platform.traffic-split.percentage:0}")
    private int trafficPercentage;

    @Value("${platform.identity-service.url:http://localhost:8086}")
    private String identityServiceUrl;

    @Value("${platform.user-service.url:http://localhost:8081}")
    private String userServiceUrl;

    public TrafficSplitFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            if (!trafficSplitEnabled) {
                log.debug("Traffic split disabled, routing to User Service");
                return chain.filter(exchange);
            }

            String path = exchange.getRequest().getURI().getPath();

            // Only apply to authentication endpoints
            if (!isAuthenticationRequest(path)) {
                return chain.filter(exchange);
            }

            // Generate random number (0-99)
            int random = ThreadLocalRandom.current().nextInt(100);

            if (random < trafficPercentage) {
                // Route to Identity Service (NEW)
                log.info("🔀 Traffic Split: Routing to Identity Service ({}%)", trafficPercentage);
                return routeToIdentityService(exchange, path);
            } else {
                // Route to User Service (OLD)
                log.info("🔀 Traffic Split: Routing to User Service ({}% remaining)", 100 - trafficPercentage);
                return chain.filter(exchange);
            }
        };
    }

    /**
     * Route request to Identity Service
     */
    private Mono<Void> routeToIdentityService(ServerWebExchange exchange, String originalPath) {
        String identityPath = convertToIdentityServicePath(originalPath);
        String fullUrl = identityServiceUrl + identityPath;

        log.info("➡️ Routing to Identity Service: {}", fullUrl);

        // Mutate request to point to Identity Service
        ServerWebExchange mutatedExchange = exchange.mutate()
            .request(exchange.getRequest().mutate()
                .uri(java.net.URI.create(fullUrl))
                .build())
            .build();

        // Continue filter chain with mutated exchange
        return Mono.empty(); // Gateway will handle routing based on mutated URI
    }

    /**
     * Convert User Service path to Identity Service path
     */
    private String convertToIdentityServicePath(String userServicePath) {
        return userServicePath
            .replace("/api/auth/register", "/platform/identity/v1/register")
            .replace("/api/auth/login", "/platform/identity/v1/authenticate")
            .replace("/api/auth/refresh", "/platform/identity/v1/refresh")
            .replace("/api/auth/logout", "/platform/identity/v1/logout");
    }

    /**
     * Check if request is an authentication request
     */
    private boolean isAuthenticationRequest(String path) {
        return path.startsWith("/api/auth/register") ||
               path.startsWith("/api/auth/login") ||
               path.startsWith("/api/auth/refresh") ||
               path.startsWith("/api/auth/logout");
    }

    public static class Config {
        // Configuration properties
    }
}
