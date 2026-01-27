package com.homegenie.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient Configuration for Shadow Mode
 * 
 * Provides WebClient bean used by ShadowModeFilter to call Identity Service
 * 
 * @version 1.0.0
 * @since Phase 2 - January 2026
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
