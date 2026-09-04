package com.aoms.aomsbackend.attendance.exception;

import com.aoms.aomsbackend.common.exception.AomsException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when the CSV export date range exceeds the maximum allowed window (90 days).
 * Returns HTTP 400 with error code.md {@code EXPORT_WINDOW_TOO_LARGE}.
 */
public class ExportWindowTooLargeException extends AomsException {
    public ExportWindowTooLargeException() {
        super("Export window cannot exceed 90 days.", HttpStatus.BAD_REQUEST, "EXPORT_WINDOW_TOO_LARGE");
    }
}
