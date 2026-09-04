package com.aoms.aomsbackend.common.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends AomsException {

    public ForbiddenException() {
        super("You do not have permission to access this resource.", HttpStatus.FORBIDDEN);
    }
    private ForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }

     public static ForbiddenException forMissingRole(String role) {
        return new ForbiddenException("Access denied. Required role: " + role);
    }
}
