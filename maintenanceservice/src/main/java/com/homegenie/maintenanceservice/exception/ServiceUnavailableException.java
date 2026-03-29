package com.homegenie.maintenanceservice.exception;

public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String serviceName) {
        super(serviceName + " is currently unavailable. Please try again later.");
    }
    public ServiceUnavailableException(String serviceName, Throwable cause) {
        super(serviceName + " is currently unavailable. Please try again later.", cause);
    }
}
