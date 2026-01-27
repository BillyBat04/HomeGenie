package com.homegenie.marketplaceservice.model;

/**
 * Payment status enum for booking
 * 
 * Tracks payment state separately from booking status for better visibility.
 */
public enum PaymentStatus {
    PENDING,    // Payment not yet initiated
    PAID,       // Payment successful
    FAILED,     // Payment failed
    REFUNDED    // Payment refunded
}
