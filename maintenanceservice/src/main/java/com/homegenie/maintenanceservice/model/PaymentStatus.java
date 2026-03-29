package com.homegenie.maintenanceservice.model;


public enum PaymentStatus {

    /** Payment creation is queued — set immediately when request is marked COMPLETED. */
    PENDING,
    /** Payment was successfully created in payment-service. Terminal state. */
    SUCCESS,
    /**
     * Payment creation failed (payment-service was down or returned an error).
     * The retry scheduler will attempt again on the next run.
     */
    FAILED
}
