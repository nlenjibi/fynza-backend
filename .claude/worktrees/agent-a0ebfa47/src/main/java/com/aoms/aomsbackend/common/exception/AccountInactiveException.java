package com.aoms.aomsbackend.common.exception;

public class AccountInactiveException extends AuthException {

    // Same message as InvalidCredentialsException — prevents user enumeration.
    public AccountInactiveException() {
        super("Inactive Account");
    }
}
