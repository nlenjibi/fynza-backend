package com.aoms.aomsbackend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a resource already exists and a duplicate would violate a unique constraint.
 * Maps to HTTP 409 Conflict. The {@code code.md} field is serialised into the error response
 * body so the client can distinguish specific conflict reasons.
 */
public class ConflictException extends AomsException {

    /**
     * Constructs a ConflictException with a human-readable message and a machine-readable code.md.
     *
     * @param message descriptive explanation of the conflict
     * @param code    machine-readable error code.md (e.g. "HOLIDAY_ALREADY_EXISTS")
     */
    public ConflictException(String message, String code) {
        super(message, HttpStatus.CONFLICT, code);
    }
}
