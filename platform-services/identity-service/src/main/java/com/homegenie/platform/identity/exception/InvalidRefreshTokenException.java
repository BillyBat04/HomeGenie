package com.homegenie.platform.identity.exception;

/**
 * Exception thrown when refresh token is invalid
 */
public class InvalidRefreshTokenException extends IdentityException {
    
    public InvalidRefreshTokenException() {
        super("Invalid or expired refresh token");
    }
    
    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
