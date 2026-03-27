package com.homegenie.gateway.filter;

import com.homegenie.gateway.config.TrafficSplitState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

@Slf4j
@Component
public class TrafficSplitFilter extends AbstractGatewayFilterFactory<TrafficSplitFilter.Config> {

    private final TrafficSplitState trafficSplitState;

    @Value("${platform.identity-service.url:http://localhost:8086}")
    private String identityServiceUrl;

    @Value("${platform.user-service.url:http://localhost:8081}")
    private String userServiceUrl;

    public TrafficSplitFilter(TrafficSplitState trafficSplitState) {
        super(Config.class);
        this.trafficSplitState = trafficSplitState;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            if (!trafficSplitState.isEnabled()) {
                log.debug("Traffic split disabled, routing to User Service");
                return chain.filter(exchange);
            }

            String path = exchange.getRequest().getURI().getPath();

            if (!isAuthenticationRequest(path)) {
                return chain.filter(exchange);
            }

            int currentPercentage = trafficSplitState.getTrafficPercentage();
            int random = java.util.concurrent.ThreadLocalRandom.current().nextInt(100);

            if (random < currentPercentage) {
                log.info("Traffic Split: Routing to Identity Service ({}%)", currentPercentage);
                return routeToIdentityService(exchange, chain, path);
            } else {
                log.info("Traffic Split: Routing to User Service ({}% remaining)", 100 - currentPercentage);
                return chain.filter(exchange);
            }
        };
    }

    @SuppressWarnings("null")
    private Mono<Void> routeToIdentityService(ServerWebExchange exchange, GatewayFilterChain chain, String originalPath) {
        String identityPath = convertToIdentityServicePath(originalPath);
        String fullUrl = identityServiceUrl + identityPath;

        log.info("➡️ Routing to Identity Service: {}", fullUrl);

        try {
            URI identityUri = URI.create(fullUrl);
            ServerWebExchange mutatedExchange = exchange.mutate()
                .request(exchange.getRequest().mutate()
                    .uri(identityUri)
                    .build())
                .build();
            return chain.filter(mutatedExchange);
        } catch (IllegalArgumentException e) {
            log.error("Invalid Identity Service URI: {} - Falling back to User Service", fullUrl);
            return chain.filter(exchange);
        }
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
