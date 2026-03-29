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
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${gateway.actuator.username:admin}")
    private String actuatorUsername;

    @Value("${gateway.actuator.password:changeme}")
    private String actuatorPassword;



 
    @Bean
    public SecurityWebFilterChain actuatorSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .securityMatcher(ServerWebExchangeMatchers.pathMatchers("/actuator/**"))
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/actuator/health/**").permitAll()
                .pathMatchers("/actuator/info").permitAll()
                .pathMatchers("/actuator/**").authenticated()
            )
            .httpBasic(basic -> {})
            .build();
    }


    @Bean
    public SecurityWebFilterChain apiSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                
                .pathMatchers(
                    "/api/users/register",
                    "/api/users/login",
                    "/api/auth/**",
                    "/platform/identity/v1/register",
                    "/platform/identity/v1/authenticate",
                    "/platform/identity/v1/refresh",
                    "/platform/identity/v1/logout",
                    "/.well-known/jwks.json",
                    "/fallback/**"
                ).permitAll()
                .anyExchange().authenticated()
            )

            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
            .build();
    }

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
