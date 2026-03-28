package com.homegenie.maintenanceservice.model;


public enum ItemCategory {
    /**
     * Air conditioning units, fans, ventilation systems
     */
    HVAC,
    
    /**
     * Refrigerators, freezers, ice makers
     */
    REFRIGERATION,
    
    /**
     * Washing machines, dryers, dishwashers
     */
    LAUNDRY,
    
    /**
     * Water heaters, boilers, pumps
     */
    WATER_HEATING,
    
    /**
     * TVs, sound systems, entertainment devices
     */
    ELECTRONICS,
    
    /**
     * Ovens, stoves, microwaves, kitchen appliances
     */
    KITCHEN_APPLIANCES,
    
    /**
     * Furniture requiring maintenance
     */
    FURNITURE,
    
    /**
     * Security systems, cameras, alarms
     */
    SECURITY_SYSTEMS,
    
    /**
     * Lighting fixtures, bulbs, smart lights
     */
    LIGHTING,
    
    /**
     * Plumbing fixtures, taps, pipes (as items)
     */
    PLUMBING_FIXTURES,
    
    /**
     * Other household items
     */
    OTHER;

    /**
     * Get corresponding maintenance category for AI classification
     */
    public Category toMaintenanceCategory() {
        return switch (this) {
            case HVAC -> Category.HVAC;
            case REFRIGERATION, LAUNDRY, KITCHEN_APPLIANCES, ELECTRONICS -> Category.OTHERS;
            case WATER_HEATING, PLUMBING_FIXTURES -> Category.PLUMBING;
            case LIGHTING -> Category.ELECTRICAL;
            case FURNITURE, SECURITY_SYSTEMS, OTHER -> Category.OTHERS;
        };
    }
}
