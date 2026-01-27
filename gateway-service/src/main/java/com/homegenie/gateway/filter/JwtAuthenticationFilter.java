package com.homegenie.gateway.filter;

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

import java.util.UUID;

/**
 * JWT Authentication Filter for API Gateway
 * 
 * Responsibilities:
 * - Validate JWT tokens
 * - Extract user information
 * - Add identity headers (X-User-Id, X-User-Role, X-User-Email)
 * - Add correlation ID (X-Request-Id)
 * - Skip validation for public routes
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {
    
    private final JwtTokenProvider jwtTokenProvider;
    
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        super(Config.class);
        this.jwtTokenProvider = jwtTokenProvider;
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
                
                // Add headers for downstream services
                ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Role", role)
                    .header("X-User-Email", email)
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
    
    /**
     * Check if route is public (no authentication required)
     */
    private boolean isPublicRoute(String path) {
        return path.equals("/api/users/register") ||
               path.equals("/api/users/login") ||
               path.startsWith("/actuator/");
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
    
    /**
     * Handle authentication errors
     */
    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
        
        String errorBody = String.format("{\"error\":\"%s\",\"message\":\"%s\"}", 
            status.getReasonPhrase(), message);
        
        log.error("Authentication error: {} - {}", status, message);
        
        return response.writeWith(
            Mono.just(response.bufferFactory().wrap(errorBody.getBytes()))
        );
    }
    
    /**
     * Configuration class for filter
     */
    public static class Config {
        // Configuration properties can be added here if needed
    }
}
