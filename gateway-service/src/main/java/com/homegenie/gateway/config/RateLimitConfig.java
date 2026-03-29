package com.homegenie.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;


@Configuration
public class RateLimitConfig {
    
 
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {

            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                return Mono.just("user:" + userId);
            }

            String xff = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {

                String clientIp = xff.split(",")[0].trim();
                return Mono.just("ip:" + clientIp);
            }

            var remoteAddress = exchange.getRequest().getRemoteAddress();
            if (remoteAddress != null) {
                String remoteIp = remoteAddress.getAddress().getHostAddress();
                return Mono.just("ip:" + remoteIp);
            }
            
            String path = exchange.getRequest().getPath().value();
            return Mono.just("unknown:" + path);
        };
    }
}
