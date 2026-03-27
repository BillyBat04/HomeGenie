package com.homegenie.gateway.filter;

import com.homegenie.gateway.config.PublicRoutesConfig;
import com.homegenie.gateway.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private final JwtTokenProvider jwtTokenProvider;
    private final PublicRoutesConfig publicRoutesConfig;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, PublicRoutesConfig publicRoutesConfig) {
        super(Config.class);
        this.jwtTokenProvider = jwtTokenProvider;
        this.publicRoutesConfig = publicRoutesConfig;
    }
    
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();
            
            // Add correlation ID for all requests
            String requestId = UUID.randomUUID().toString();
            
            // Public routes - skip JWT validation
            if (isPublicRoute(path)) {
                log.info("Public route accessed: path={}, requestId={}", path, requestId);
                ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-Request-Id", requestId)
                    .build();
                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            }
            
            // Extract JWT token
            String token = extractToken(request);
            if (token == null) {
                log.warn("Missing authorization token: path={}, requestId={}", path, requestId);
                return onError(exchange, "Missing authorization token", HttpStatus.UNAUTHORIZED);
            }
            
            // Validate token
            if (!jwtTokenProvider.validateToken(token)) {
                log.warn("Invalid or expired token: path={}, requestId={}", path, requestId);
                return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
            }
            
            // Extract user information
            try {
                String userId = jwtTokenProvider.getUserIdFromToken(token);
                String role = jwtTokenProvider.getRoleFromToken(token);
                String email = jwtTokenProvider.getEmailFromToken(token);

                ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Id", userId != null ? userId : "")
                    .header("X-User-Role", role != null ? role : "")
                    .header("X-User-Email", email != null ? email : "")
                    .header("X-Request-Id", requestId)
                    .build();

                log.info("Authenticated request: userId={}, role={}, path={}, requestId={}",
                    userId, role, path, requestId);

                return chain.filter(exchange.mutate().request(modifiedRequest).build());
                
            } catch (Exception e) {
                log.error("Error extracting user info from token: {}", e.getMessage());
                return onError(exchange, "Invalid token claims", HttpStatus.UNAUTHORIZED);
            }
        };
    }
    
    private boolean isPublicRoute(String path) {
        return publicRoutesConfig.isPublicRoute(path);
    }
    
    /**
     * Extract JWT token from Authorization header
     */
    private String extractToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
    
    @SuppressWarnings("null")
    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");

        String errorBody = String.format("{\"error\":\"%s\",\"message\":\"%s\"}",
            status.getReasonPhrase(), message);

        log.error("Authentication error: {} - {}", status, message);

        return response.writeWith(
            Mono.just(response.bufferFactory().wrap(errorBody.getBytes(StandardCharsets.UTF_8)))
        );
    }
    
    public static class Config {
    }
}
