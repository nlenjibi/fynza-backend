package com.aoms.aomsbackend.attendance.exception;

import com.aoms.aomsbackend.common.exception.AomsException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when the {@code employee_id} query filter refers to an employee
 * who is not a direct report of the authenticated manager. Returns HTTP 403.
 */
public class NotADirectReportException extends AomsException {
    public NotADirectReportException() {
        super("Employee is not a direct report.", HttpStatus.FORBIDDEN);
    }
}
