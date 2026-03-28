package com.homegenie.platform.identity.exception;


public class IdentityException extends RuntimeException {
    
    public IdentityException(String message) {
        super(message);
    }
    
    public IdentityException(String message, Throwable cause) {
        super(message, cause);
    }
}
