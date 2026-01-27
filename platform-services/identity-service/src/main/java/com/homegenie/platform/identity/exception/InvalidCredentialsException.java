package com.homegenie.platform.identity.exception;

/**
 * Exception thrown when user credentials are invalid
 */
public class InvalidCredentialsException extends IdentityException {
    
    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
    
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
