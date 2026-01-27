package com.homegenie.marketplaceservice.model;

/**
 * Booking status enum
 * 
 * Flow: PENDING → CONFIRMED (after payment) → IN_PROGRESS → COMPLETED
 * Alternative: PENDING → CANCELLED → REFUNDED
 */
public enum BookingStatus {
    PENDING,      // Customer created booking, waiting for payment
    CONFIRMED,    // Payment successful, provider assigned
    IN_PROGRESS,  // Provider started the service
    COMPLETED,    // Service completed successfully
    CANCELLED,    // Booking cancelled by customer or provider
    REFUNDED      // Booking refunded after cancellation
}
