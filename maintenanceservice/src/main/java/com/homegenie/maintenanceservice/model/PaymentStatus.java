package com.homegenie.maintenanceservice.model;

/**
 * Tracks whether the payment for a COMPLETED maintenance request was created successfully.
 *
 * Why an enum instead of a boolean?
 * - PENDING vs FAILED are two different states that need different handling:
 *   PENDING = "we haven't tried yet" (set right when request is marked COMPLETED)
 *   FAILED  = "we tried and payment-service rejected/timed-out" (retry may help)
 *   SUCCESS = "payment created, nothing to do"
 * - This makes the retry scheduler logic clear: retry PENDING + FAILED, skip SUCCESS.
 * - Easier to add more states later (e.g. CANCELLED) without schema changes.
 */
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
