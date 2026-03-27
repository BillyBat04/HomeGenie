package com.homegenie.marketplaceservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.util.Collections;
import java.util.List;

/**
 * Security configuration for Marketplace Service.
 *
 * All requests must carry a valid JWT Bearer token issued by the platform's
 * UserService / API Gateway — except a set of read-only (GET) public endpoints
 * that guests and the Maintenance Service cross-domain call need.
 *
 * Fine-grained role checks (ADMIN vs USER) are declared directly on the
 * controller methods using @PreAuthorize (enabled by @EnableMethodSecurity).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // enables @PreAuthorize / @PostAuthorize on methods
public class SecurityConfig {

    /** Shared HMAC secret — must be the same value used by user-service (JWT_SECRET env var). */
    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * Decodes and verifies HS256 tokens signed by user-service.
     * Spring Boot backs off its auto-configuration when this bean is present.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKeySpec key = new SecretKeySpec(jwtSecret.getBytes(), "HMACSHA256");
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    /**
     * Maps the custom "role" claim (e.g. "ADMIN") to a Spring GrantedAuthority
     * with the required ROLE_ prefix so that @PreAuthorize("hasRole('ADMIN')")
     * works correctly.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString("role");
            if (role == null || role.isBlank()) {
                return Collections.emptyList();
            }
            return List.of(new SimpleGrantedAuthority("ROLE_" + role));
        });
        return converter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Marketplace is a stateless REST API — no session needed
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // CSRF is irrelevant for stateless JWT APIs
            .csrf(csrf -> csrf.disable())

            .authorizeHttpRequests(auth -> auth
                // ── Public read-only endpoints ───────────────────────────
                // Anyone (including the Maintenance Service cross-domain call) can
                // browse the catalog and get recommendations without a token.
                .requestMatchers(HttpMethod.GET,
                    "/api/marketplace/services",
                    "/api/marketplace/services/**",
                    "/api/marketplace/recommendations",
                    "/api/marketplace/providers",
                    "/api/marketplace/providers/**"
                ).permitAll()

                // ── Actuator / OpenAPI (internal monitoring) ─────────────
                .requestMatchers(
                    "/actuator/**",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()

                // Everything else requires a valid JWT
                .anyRequest().authenticated()
            )

            // Validate JWT tokens using the JwtDecoder + JwtAuthenticationConverter beans above
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }
}
