package com.aoms.aomsbackend.common.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends AomsException {

    public BadRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }

    /**
     * Constructs a BadRequestException with a human-readable message and a machine-readable error code.md.
     *
     * @param message descriptive explanation
     * @param code    machine-readable code.md serialised into the error response (e.g. "PAST_HOLIDAY_IMMUTABLE")
     */
    public BadRequestException(String message, String code) {
        super(message, HttpStatus.BAD_REQUEST, code);
    }
}