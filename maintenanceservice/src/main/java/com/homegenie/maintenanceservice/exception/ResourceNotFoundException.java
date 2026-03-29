package com.homegenie.maintenanceservice.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceType, Long id) {
        super(resourceType + " not found with id: " + id);
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
