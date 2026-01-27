package com.homegenie.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;

/**
 * Security Configuration for API Gateway
 * 
 * Security Features:
 * - JWT authentication via custom filter for API routes
 * - HTTP Basic Auth for actuator endpoints (monitoring)
 * - Separate security chains for API vs management endpoints
 * 
 * Week 2 Enhancement: Secured actuator endpoints
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    
    @Value("${management.actuator.username:admin}")
    private String actuatorUsername;
    
    @Value("${management.actuator.password:changeme}")
    private String actuatorPassword;
    
    /**
     * Security chain for actuator endpoints
     * Requires HTTP Basic Authentication
     */
    @Bean
    public SecurityWebFilterChain actuatorSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .securityMatcher(exchange -> {
                String path = exchange.getRequest().getURI().getPath();
                if (path.startsWith("/actuator")) {
                    return ServerWebExchangeMatcher.MatchResult.match();
                }
                return ServerWebExchangeMatcher.MatchResult.notMatch();
            })
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/actuator/health/**").permitAll() // K8s health checks
                .pathMatchers("/actuator/info").permitAll() // Public info
                .pathMatchers("/actuator/**").authenticated() // Secure all other actuator endpoints
            )
            .httpBasic(basic -> {}) // Enable HTTP Basic Auth
            .build();
    }
    
    /**
     * Security chain for API routes
     * JWT authentication handled by custom filter
     */
    @Bean
    public SecurityWebFilterChain apiSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .anyExchange().permitAll() // JWT filter handles auth
            )
            .build();
    }
    
    /**
     * User details service for actuator authentication
     * Credentials from environment variables or application.yml
     */
    @Bean
    public MapReactiveUserDetailsService actuatorUserDetailsService() {
        UserDetails user = User.builder()
            .username(actuatorUsername)
            .password(passwordEncoder().encode(actuatorPassword))
            .roles("ACTUATOR")
            .build();
        return new MapReactiveUserDetailsService(user);
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
