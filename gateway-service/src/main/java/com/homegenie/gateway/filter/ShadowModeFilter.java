package com.homegenie.gateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.*;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class ShadowModeFilter extends AbstractGatewayFilterFactory<ShadowModeFilter.Config> {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    
    private final AtomicLong shadowCallsTotal = new AtomicLong(0);
    private final AtomicLong shadowMismatchesTotal = new AtomicLong(0);
    private final AtomicLong shadowErrorsTotal = new AtomicLong(0);
    
    
    private static final Set<String> IGNORE_FIELDS = Set.of(
        "timestamp", "iat", "exp", "jti", "nbf", 
        "refreshToken", "tokenId", "sessionId"
    );

    @Value("${platform.shadow-mode.enabled:false}")
    private boolean shadowModeEnabled;

    @Value("${platform.shadow-mode.log-mismatches:true}")
    private boolean logMismatches;

    @Value("${platform.shadow-mode.alert-on-mismatch:false}")
    private boolean alertOnMismatch;

    @Value("${platform.identity-service.url:http:
    private String identityServiceUrl;

    public ShadowModeFilter(WebClient.Builder webClientBuilder) {
        super(Config.class);
        this.webClient = webClientBuilder.build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            if (!shadowModeEnabled) {
                log.debug("Shadow mode disabled, skipping");
                return chain.filter(exchange);
            }

            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            
            if (!isAuthenticationRequest(path)) {
                return chain.filter(exchange);
            }

            log.info("Shadow Mode: Intercepting request to {}", path);

            
            return DataBufferUtils.join(exchange.getRequest().getBody())
                .flatMap(bodyDataBuffer -> {
                    byte[] cachedBodyBytes = new byte[bodyDataBuffer.readableByteCount()];
                    bodyDataBuffer.read(cachedBodyBytes);
                    DataBufferUtils.release(bodyDataBuffer);
                    
                    String requestBodyString = new String(cachedBodyBytes, StandardCharsets.UTF_8);
                    log.debug("Cached request body: {}", requestBodyString);

                    
                    DataBuffer bodyBufferForUserService = exchange.getResponse()
                        .bufferFactory()
                        .wrap(cachedBodyBytes);

                    
                    ServerWebExchange modifiedExchange = exchange.mutate()
                        .request(decorator -> decorator
                            .headers(httpHeaders -> {})  
                            
                        )
                        .build();

                    
                    ServerHttpRequest decoratedRequest = new org.springframework.http.server.reactive.ServerHttpRequestDecorator(exchange.getRequest()) {
                        @Override
                        public reactor.core.publisher.Flux<DataBuffer> getBody() {
                            return reactor.core.publisher.Flux.just(bodyBufferForUserService);
                        }
                    };

                    
                    return chain.filter(modifiedExchange.mutate().request(decoratedRequest).build())
                        .doOnSuccess(response -> {
                            
                            log.info("User Service responded, now calling Identity Service in shadow...");
                            callIdentityServiceShadow(path, requestBodyString, request.getHeaders())
                                .subscribe(
                                    shadowResponse -> log.info("Shadow call completed"),
                                    error -> log.error("Shadow call failed: {}", error.getMessage())
                                );
                        });
                });
        };
    }

        private Mono<String> callIdentityServiceShadow(String originalPath, String requestBody, HttpHeaders headers) {
        String identityPath = convertToIdentityServicePath(originalPath);
        String fullUrl = identityServiceUrl + identityPath;

        log.info("Shadow calling Identity Service: {}", fullUrl);
        
        shadowCallsTotal.incrementAndGet();
        long startTime = System.currentTimeMillis();

        return webClient
            .post()
            .uri(fullUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .timeout(Duration.ofSeconds(5))
            .doOnSuccess(response -> {
                long duration = System.currentTimeMillis() - startTime;
                log.info("Identity Service shadow response received ({}ms)", duration);
                log.debug("Response: {}", response);
            })
            .doOnError(error -> {
                shadowErrorsTotal.incrementAndGet();
                log.error("Identity Service shadow call failed: {}", error.getMessage());
                if (alertOnMismatch) {
                    sendAlert("Identity Service shadow call failed", error.getMessage());
                }
            });
    }

        private String convertToIdentityServicePath(String userServicePath) {
        return userServicePath
            .replace("/api/auth/register", "/platform/identity/v1/register")
            .replace("/api/auth/login", "/platform/identity/v1/authenticate")
            .replace("/api/auth/refresh", "/platform/identity/v1/refresh")
            .replace("/api/auth/logout", "/platform/identity/v1/logout");
    }

        private boolean isAuthenticationRequest(String path) {
        return path.startsWith("/api/auth/register") ||
               path.startsWith("/api/auth/login") ||
               path.startsWith("/api/auth/refresh") ||
               path.startsWith("/api/auth/logout");
    }

        private void compareResponses(String userServiceResponse, String identityServiceResponse) {
        try {
            JsonNode userJson = objectMapper.readTree(userServiceResponse);
            JsonNode identityJson = objectMapper.readTree(identityServiceResponse);
            
            List<String> differences = new ArrayList<>();
            compareJsonNodes("", userJson, identityJson, differences);
            
            if (differences.isEmpty()) {
                log.info("Shadow Mode: Responses match perfectly");
            } else {
                shadowMismatchesTotal.incrementAndGet();
                
                log.warn("SHADOW MODE MISMATCH DETECTED!");
                log.warn("Found {} differences:", differences.size());
                differences.forEach(diff -> log.warn("  - {}", diff));
                
                if (logMismatches) {
                    log.warn("User Service Response: {}", userServiceResponse);
                    log.warn("Identity Service Response: {}", identityServiceResponse);
                }
                
                if (alertOnMismatch) {
                    sendAlert("Shadow Mode Response Mismatch", 
                        String.format("Differences: %d\n%s", differences.size(), String.join("\n", differences)));
                }
            }
        } catch (Exception e) {
            log.error("Failed to compare responses: {}", e.getMessage());
        }
    }
    
        private void compareJsonNodes(String path, JsonNode node1, JsonNode node2, List<String> differences) {
        if (node1.getNodeType() != node2.getNodeType()) {
            differences.add(String.format("%s: Type mismatch (%s vs %s)", 
                path, node1.getNodeType(), node2.getNodeType()));
            return;
        }
        
        if (node1.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields1 = node1.fields();
            while (fields1.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields1.next();
                String fieldName = entry.getKey();
                String fieldPath = path.isEmpty() ? fieldName : path + "." + fieldName;
                
                
                if (IGNORE_FIELDS.contains(fieldName)) {
                    log.debug("Skipping comparison for ignored field: {}", fieldPath);
                    continue;
                }
                
                if (node2.has(fieldName)) {
                    compareJsonNodes(fieldPath, entry.getValue(), node2.get(fieldName), differences);
                } else {
                    differences.add(String.format("%s: Missing in Identity Service response", fieldPath));
                }
            }
            
            
            Iterator<String> fields2 = node2.fieldNames();
            while (fields2.hasNext()) {
                String fieldName = fields2.next();
                if (!node1.has(fieldName) && !IGNORE_FIELDS.contains(fieldName)) {
                    String fieldPath = path.isEmpty() ? fieldName : path + "." + fieldName;
                    differences.add(String.format("%s: Extra field in Identity Service response", fieldPath));
                }
            }
        } else if (node1.isArray()) {
            if (node1.size() != node2.size()) {
                differences.add(String.format("%s: Array size mismatch (%d vs %d)", 
                    path, node1.size(), node2.size()));
                return;
            }
            
            for (int i = 0; i < node1.size(); i++) {
                compareJsonNodes(path + "[" + i + "]", node1.get(i), node2.get(i), differences);
            }
        } else {
            
            if (!node1.equals(node2)) {
                differences.add(String.format("%s: Value mismatch ('%s' vs '%s')", 
                    path, node1.asText(), node2.asText()));
            }
        }
    }

        private void sendAlert(String title, String message) {
        log.error("🚨 ALERT: {} - {}", title, message);
    }
    
        public Map<String, Long> getMetrics() {
        return Map.of(
            "shadow_calls_total", shadowCallsTotal.get(),
            "shadow_mismatches_total", shadowMismatchesTotal.get(),
            "shadow_errors_total", shadowErrorsTotal.get()
        );
    }

    public static class Config {
        
    }
}
