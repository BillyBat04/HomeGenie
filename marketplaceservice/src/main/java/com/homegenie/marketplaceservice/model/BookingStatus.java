package com.homegenie.marketplaceservice.model;


public enum BookingStatus {
    PENDING,      // Customer created booking, waiting for payment
    CONFIRMED,    // Payment successful, provider assigned
    IN_PROGRESS,  // Provider started the service
    COMPLETED,    // Service completed successfully
    CANCELLED,    // Booking cancelled by customer or provider
    REFUNDED      // Booking refunded after cancellation
}
