package com.homegenie.platform.identity.exception;

/**
 * Base exception for Identity Service
 */
public class IdentityException extends RuntimeException {
    
    public IdentityException(String message) {
        super(message);
    }
    
    public IdentityException(String message, Throwable cause) {
        super(message, cause);
    }
}
