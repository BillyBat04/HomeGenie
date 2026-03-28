package com.homegenie.platform.identity.exception;


public class EmailAlreadyExistsException extends IdentityException {
    
    public EmailAlreadyExistsException(String email) {
        super("Email already registered: " + email);
    }
}
