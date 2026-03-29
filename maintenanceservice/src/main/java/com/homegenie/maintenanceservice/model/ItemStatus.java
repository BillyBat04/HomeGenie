package com.homegenie.maintenanceservice.model;


public enum ItemStatus {
    /**
     * Item is active and in use
     */
    ACTIVE,
    
    /**
     * Item is currently being repaired
     */
    UNDER_REPAIR,
    
    /**
     * Item is temporarily not in use
     */
    INACTIVE,
    
    /**
     * Item has been retired/disposed
     */
    RETIRED;

    /**
     * Check if item can have maintenance scheduled
     */
    public boolean canScheduleMaintenance() {
        return this == ACTIVE || this == INACTIVE;
    }

    /**
     * Check if item needs immediate attention
     */
    public boolean needsAttention() {
        return this == UNDER_REPAIR;
    }
}
