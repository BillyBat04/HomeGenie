package com.homegenie.marketplaceservice.model;

/**
 * Price unit enum for service pricing model
 */
public enum PriceUnit {
    PER_JOB,    // Fixed price per job
    PER_HOUR,   // Hourly rate
    PER_SQFT,   // Per square foot (for cleaning, painting)
    PER_DAY     // Daily rate
}
