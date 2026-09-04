package com.aoms.aomsbackend.common.exception;

import org.springframework.http.HttpStatus;

public class AuthException extends AomsException {

    public AuthException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
