package com.aoms.aomsbackend.common.exception;

public class TokenVerificationException extends AuthException {

    public TokenVerificationException() {
        super("Token verification failed.");
    }
}
