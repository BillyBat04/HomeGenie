package com.homegenie.platform.identity.exception;


public class InvalidRefreshTokenException extends IdentityException {
    
    public InvalidRefreshTokenException() {
        super("Invalid or expired refresh token");
    }
    
    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
