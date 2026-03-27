package com.homegenie.gateway.filter;

import com.homegenie.gateway.config.MiniAppConfig;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@Component
public class MiniAppFilter implements GlobalFilter, Ordered {

    private static final String MINI_APP_HEADER = "X-MINI-APP";
    private static final String DEFAULT_MINI_APP = "unknown";
    private static final String REQUEST_START_TIME_ATTR = "miniAppRequestStartTime";

    private final MiniAppConfig miniAppConfig;
    private final MeterRegistry meterRegistry;

    public MiniAppFilter(MiniAppConfig miniAppConfig, MeterRegistry meterRegistry) {
        this.miniAppConfig = miniAppConfig;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        
        
        String miniAppId = request.getHeaders().getFirst(MINI_APP_HEADER);
        
        
        if (miniAppId == null || miniAppId.isBlank()) {
            miniAppId = inferMiniAppFromPath(path);
        }
        
        
        if (!isValidMiniApp(miniAppId)) {
            log.warn("Invalid mini-app ID: {} for path: {}", miniAppId, path);
            miniAppId = DEFAULT_MINI_APP;
        }
        
        final String finalMiniAppId = miniAppId;
        
        
        log.info("🎯 Mini-App Request: {} → {} {}", 
                finalMiniAppId, request.getMethod(), path);
        
        
        exchange.getAttributes().put(REQUEST_START_TIME_ATTR, Instant.now());
        
        
        incrementRequestCounter(finalMiniAppId, path);
        
        
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(MINI_APP_HEADER, finalMiniAppId)
                .build();
        
        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();
        
        
        return chain.filter(mutatedExchange)
                .doFinally(signalType -> {
                    recordRequestDuration(mutatedExchange, finalMiniAppId);
                    recordResponseStatus(mutatedExchange, finalMiniAppId);
                })
                .doOnError(error -> {
                    incrementErrorCounter(finalMiniAppId, error.getClass().getSimpleName());
                    log.error("✗ Mini-App Request Failed: {} - Error: {}", 
                            finalMiniAppId, error.getMessage());
                });
    }

    private String inferMiniAppFromPath(String path) {
        return miniAppConfig.getEnabledApps().stream()
                .filter(app -> app.getRoutes().stream()
                        .anyMatch(route -> path.startsWith(route.replace("/**", ""))))
                .map(MiniAppConfig.MiniApp::getId)
                .findFirst()
                .orElse(DEFAULT_MINI_APP);
    }

    private boolean isValidMiniApp(String miniAppId) {
        if (miniAppId == null || miniAppId.isBlank()) {
            return false;
        }
        return miniAppConfig.isEnabled(miniAppId);
    }

    private void incrementRequestCounter(String miniAppId, String path) {
        try {
            Counter.builder("gateway_requests_total")
                    .description("Total Gateway requests per mini-app")
                    .tag("mini_app", miniAppId)
                    .tag("path_prefix", extractPathPrefix(path))
                    .register(meterRegistry)
                    .increment();
        } catch (Exception e) {
            log.warn("Failed to increment request counter: {}", e.getMessage());
        }
    }

    private void recordRequestDuration(ServerWebExchange exchange, String miniAppId) {
        try {
            Instant startTime = exchange.getAttribute(REQUEST_START_TIME_ATTR);
            if (startTime != null) {
                long durationMs = java.time.Duration.between(startTime, Instant.now()).toMillis();
                
                Timer.builder("gateway_request_duration_seconds")
                        .description("Request duration per mini-app")
                        .tag("mini_app", miniAppId)
                        .tag("method", exchange.getRequest().getMethod().name())
                        .register(meterRegistry)
                        .record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            log.warn("Failed to record request duration: {}", e.getMessage());
        }
    }

    private void recordResponseStatus(ServerWebExchange exchange, String miniAppId) {
        try {
            ServerHttpResponse response = exchange.getResponse();
            HttpStatus statusCode = (HttpStatus) response.getStatusCode();
            
            if (statusCode != null) {
                Counter.builder("gateway_responses_total")
                        .description("Total Gateway responses per mini-app")
                        .tag("mini_app", miniAppId)
                        .tag("status_code", String.valueOf(statusCode.value()))
                        .tag("status_family", statusCode.series().name())
                        .register(meterRegistry)
                        .increment();
            }
        } catch (Exception e) {
            log.warn("Failed to record response status: {}", e.getMessage());
        }
    }

    private void incrementErrorCounter(String miniAppId, String errorType) {
        try {
            Counter.builder("gateway_errors_total")
                    .description("Total Gateway errors per mini-app")
                    .tag("mini_app", miniAppId)
                    .tag("error_type", errorType)
                    .register(meterRegistry)
                    .increment();
        } catch (Exception e) {
            log.warn("Failed to increment error counter: {}", e.getMessage());
        }
    }

    private String extractPathPrefix(String path) {
        if (path.startsWith("/api/")) {
            String[] parts = path.substring(5).split("/");
            return parts.length > 0 ? parts[0] : "unknown";
        }
        return "unknown";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
