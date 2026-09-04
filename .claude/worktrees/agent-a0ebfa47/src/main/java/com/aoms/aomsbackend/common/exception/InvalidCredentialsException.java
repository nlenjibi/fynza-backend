package com.aoms.aomsbackend.common.exception;

public class InvalidCredentialsException extends AuthException {

    public InvalidCredentialsException() {
        super("Invalid email or password.");
    }
}
