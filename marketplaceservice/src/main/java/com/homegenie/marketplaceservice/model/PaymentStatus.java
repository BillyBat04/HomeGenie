package com.homegenie.marketplaceservice.model;


public enum PaymentStatus {
    PENDING,    // Payment not yet initiated
    PAID,       // Payment successful
    FAILED,     // Payment failed
    REFUNDED    // Payment refunded
}
