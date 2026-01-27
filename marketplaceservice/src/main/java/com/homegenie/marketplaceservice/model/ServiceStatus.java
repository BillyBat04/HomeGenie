package com.homegenie.marketplaceservice.model;

/**
 * Service status enum
 */
public enum ServiceStatus {
    ACTIVE,      // Service available for booking
    INACTIVE,    // Service temporarily disabled by provider
    SUSPENDED    // Service suspended by admin
}
