package com.homegenie.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Global filter that propagates authenticated user identity to downstream services
 * as request headers.
 *
 * <p>Spring Security's oauth2ResourceServer already validated the Bearer JWT
 * (via JWKS) before this filter runs. We only need to read the already-decoded
 * {@link Jwt} from the {@link SecurityContext} and inject:
 * <ul>
 *   <li>{@code X-User-Id}    — numeric user id from the {@code userId} claim
 *   <li>{@code X-User-Role}  — role string from the {@code role} claim
 *   <li>{@code X-User-Email} — email from the JWT subject
 *   <li>{@code X-Request-Id} — correlation id (always injected)
 * </ul>
 *
 * <p>No JWT parsing happens here — Spring Security owns that responsibility.
 */
@Slf4j
@Component
public class UserContextPropagationFilter implements GlobalFilter, Ordered {

    @Override
    public int getOrder() {
        // Run after Spring Security filters (which are at Ordered.HIGHEST_PRECEDENCE)
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestId = UUID.randomUUID().toString();

        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(auth -> auth instanceof JwtAuthenticationToken)
                .cast(JwtAuthenticationToken.class)
                .flatMap(authToken -> {
                    Jwt jwt = authToken.getToken();

                    Object userIdClaim = jwt.getClaim("userId");
                    String userId = userIdClaim != null ? userIdClaim.toString() : "";
                    String role   = jwt.getClaimAsString("role");
                    String email  = jwt.getSubject();

                    log.info("Forwarding request: userId={}, role={}, path={}, requestId={}",
                            userId, role, exchange.getRequest().getURI().getPath(), requestId);

                    ServerHttpRequest modified = exchange.getRequest().mutate()
                            .header("X-User-Id", userId)
                            .header("X-User-Role", role != null ? role : "")
                            .header("X-User-Email", email != null ? email : "")
                            .header("X-Request-Id", requestId)
                            .build();

                    return chain.filter(exchange.mutate().request(modified).build());
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // Unauthenticated request on a public route — just add correlation id
                    ServerHttpRequest modified = exchange.getRequest().mutate()
                            .header("X-Request-Id", requestId)
                            .build();
                    return chain.filter(exchange.mutate().request(modified).build());
                }));
    }
}
