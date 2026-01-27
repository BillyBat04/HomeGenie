package com.homegenie.platform.identity.exception;

/**
 * Exception thrown when email already exists
 */
public class EmailAlreadyExistsException extends IdentityException {
    
    public EmailAlreadyExistsException(String email) {
        super("Email already registered: " + email);
    }
}
