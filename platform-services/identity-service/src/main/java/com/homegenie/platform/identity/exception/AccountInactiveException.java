package com.homegenie.platform.identity.exception;


public class AccountInactiveException extends IdentityException {
    
    public AccountInactiveException() {
        super("Account is deactivated. Please contact support.");
    }
}
