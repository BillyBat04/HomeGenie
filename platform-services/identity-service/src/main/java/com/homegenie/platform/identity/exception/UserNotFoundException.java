package com.homegenie.platform.identity.exception;


public class UserNotFoundException extends IdentityException {
    
    public UserNotFoundException(Long userId) {
        super("User not found with ID: " + userId);
    }
    
    public UserNotFoundException(String email) {
        super("User not found with email: " + email);
    }
}
