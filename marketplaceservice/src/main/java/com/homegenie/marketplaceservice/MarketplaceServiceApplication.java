package com.homegenie.marketplaceservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Marketplace Service - Mini-App #2 for HomeGenie Super App
 * 
 * Enables external service provider booking (plumbers, electricians, etc.)
 * when internal maintenance team is not available.
 * 
 * Key Features:
 * - Provider management
 * - Service catalog
 * - Booking flow with payment integration
 * - Review & rating system
 * - Cross-domain recommendations
 * 
 * Mini-App Context: marketplace
 * Port: 8085
 * Database: homegenie_marketplace
 * 
 * @author HomeGenie Platform Team
 * @version 1.0.0
 * @since 2026-01-17
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableKafka
public class MarketplaceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarketplaceServiceApplication.class, args);
    }
}
