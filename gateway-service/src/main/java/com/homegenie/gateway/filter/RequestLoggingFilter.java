package com.homegenie.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;


@Slf4j
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {
    
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String REQUEST_START_TIME = "requestStartTime";
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        
        String requestIdHeader = request.getHeaders().getFirst(REQUEST_ID_HEADER);
        final String requestId = (requestIdHeader == null || requestIdHeader.isBlank()) 
            ? UUID.randomUUID().toString() 
            : requestIdHeader;
        
       
        exchange.getResponse().getHeaders().add(REQUEST_ID_HEADER, requestId);
        
       
        exchange.getAttributes().put(REQUEST_START_TIME, Instant.now());
        
      
        String clientIp = getClientIp(request);
        
        log.info("→ Request [{}] {} {} from {} - User-Agent: {}", 
            requestId,
            request.getMethod(),
            request.getURI().getPath(),
            clientIp,
            request.getHeaders().getFirst("User-Agent"));
        
      
        return chain.filter(exchange)
            .doFinally(signalType -> {
                ServerHttpResponse response = exchange.getResponse();
                Instant startTime = exchange.getAttribute(REQUEST_START_TIME);
                
                long duration = startTime != null 
                    ? Duration.between(startTime, Instant.now()).toMillis()
                    : 0;
                
                log.info("← Response [{}] {} {} - Status: {} - Duration: {}ms",
                    requestId,
                    request.getMethod(),
                    request.getURI().getPath(),
                    response.getStatusCode(),
                    duration);
                
                
                if (duration > 1000) {
                    log.warn("⚠ Slow request detected [{}] - {}ms", requestId, duration);
                }
            })
            .doOnError(error -> {
                log.error("✗ Request failed [{}] {} {} - Error: {}",
                    requestId,
                    request.getMethod(),
                    request.getURI().getPath(),
                    error.getMessage());
            });
    }

    private String getClientIp(ServerHttpRequest request) {
        String xff = request.getHeaders().getFirst("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            
            return xff.split(",")[0].trim();
        }
        
        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }
        
        return "unknown";
    }
    

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
