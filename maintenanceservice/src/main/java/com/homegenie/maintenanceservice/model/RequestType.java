package com.homegenie.maintenanceservice.model;

/**
 * Type of maintenance request
 */
public enum RequestType {
    /**
     * Scheduled/preventive maintenance for an item
     */
    SCHEDULED,
    
    /**
     * Ad-hoc/reactive request (something broke)
     */
    AD_HOC,
    
    /**
     * Emergency repair request
     */
    EMERGENCY;

    /**
     * Check if request is proactive
     */
    public boolean isProactive() {
        return this == SCHEDULED;
    }

    /**
     * Check if request is reactive
     */
    public boolean isReactive() {
        return this == AD_HOC || this == EMERGENCY;
    }
}
