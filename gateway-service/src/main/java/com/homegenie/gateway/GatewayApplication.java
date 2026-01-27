package com.homegenie.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * HomeGenie API Gateway Application
 * 
 * Responsibilities:
 * - Routing & load distribution
 * - JWT authentication
 * - CORS handling
 * - Rate limiting
 * - Request logging & metrics
 * 
 * @author HomeGenie Team
 * @version 1.0.0
 */
@SpringBootApplication
public class GatewayApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
