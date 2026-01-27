package com.homegenie.marketplaceservice.model;

/**
 * Provider status enum
 * 
 * PENDING_VERIFICATION: New provider, waiting for admin verification
 * ACTIVE: Verified provider, can accept bookings
 * SUSPENDED: Temporarily disabled (quality issues, complaints)
 * BLOCKED: Permanently banned
 */
public enum ProviderStatus {
    PENDING_VERIFICATION,
    ACTIVE,
    SUSPENDED,
    BLOCKED
}
