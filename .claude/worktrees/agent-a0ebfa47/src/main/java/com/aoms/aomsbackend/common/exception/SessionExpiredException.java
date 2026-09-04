package com.aoms.aomsbackend.common.exception;

public class SessionExpiredException extends AuthException {

    public SessionExpiredException() {
        super("Session invalid or expired.");
    }
    public SessionExpiredException(String message) {
        super(message);
    }
}
