package com.homegenie.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Rate Limiting Configuration
 * 
 * Provides key resolver for rate limiting based on:
 * - Authenticated users: userId
 * - Anonymous users: client IP (X-Forwarded-For aware)
 * - Fallback: per-route bucket to prevent shared quota attacks
 * 
 * Production-safe improvements:
 * - X-Forwarded-For support for LB/proxy environments
 * - No shared "unknown" bucket (prevents self-DDOS)
 * - Graceful degradation per route
 */
@Configuration
public class RateLimitConfig {
    
    /**
     * Key resolver for rate limiting
     * Priority: userId → X-Forwarded-For → RemoteAddress → per-route fallback
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            // 1. Authenticated user (highest priority)
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                return Mono.just("user:" + userId);
            }
            
            // 2. Extract real client IP from X-Forwarded-For (proxy/LB aware)
            String xff = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                // X-Forwarded-For: client, proxy1, proxy2
                // Take first IP (original client)
                String clientIp = xff.split(",")[0].trim();
                return Mono.just("ip:" + clientIp);
            }
            
            // 3. Fallback to RemoteAddress (direct connection)
            if (exchange.getRequest().getRemoteAddress() != null) {
                String remoteIp = exchange.getRequest().getRemoteAddress()
                    .getAddress()
                    .getHostAddress();
                return Mono.just("ip:" + remoteIp);
            }
            
            // 4. Last resort – per-route bucket (prevents shared quota attack)
            // Each route gets independent bucket instead of shared "unknown"
            String path = exchange.getRequest().getPath().value();
            return Mono.just("unknown:" + path);
        };
    }
}
