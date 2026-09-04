package com.aoms.aomsbackend.attendance.exception;

import com.aoms.aomsbackend.common.exception.AomsException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when the authenticated user cannot be mapped to an {@code Employee} record
 * in the attendance database. Returns HTTP 404.
 */
public class EmployeeNotFoundException extends AomsException {
    public EmployeeNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
