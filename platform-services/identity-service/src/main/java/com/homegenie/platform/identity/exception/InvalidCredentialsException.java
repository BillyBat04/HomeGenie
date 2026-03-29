package com.homegenie.platform.identity.exception;


public class InvalidCredentialsException extends IdentityException {
    
    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
    
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
