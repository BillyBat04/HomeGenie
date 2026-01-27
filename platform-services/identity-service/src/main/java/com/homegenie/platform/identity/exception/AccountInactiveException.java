package com.homegenie.platform.identity.exception;

/**
 * Exception thrown when account is inactive
 */
public class AccountInactiveException extends IdentityException {
    
    public AccountInactiveException() {
        super("Account is deactivated. Please contact support.");
    }
}
