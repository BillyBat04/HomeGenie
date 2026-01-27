package com.homegenie.platform.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Identity Platform Service - Platform-level Authentication and Identity Management
 * 
 * This service is part of the Platform Layer refactoring (Phase 1)
 * Goal: Extract authentication logic from User Service to create reusable platform capability
 * 
 * Migration Strategy: Strangler Fig Pattern
 * - Run in parallel with User Service (shadow mode)
 * - Gradual traffic migration (0% → 100%)
 * - Shared database (no data migration)
 * - Backward compatible API contracts
 * 
 * @version 1.0.0
 * @since Phase 1 - January 2026
 */
@SpringBootApplication
@EnableJpaAuditing
public class IdentityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityServiceApplication.class, args);
    }
}
