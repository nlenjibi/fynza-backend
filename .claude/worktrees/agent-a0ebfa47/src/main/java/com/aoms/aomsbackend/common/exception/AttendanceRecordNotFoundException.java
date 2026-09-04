package com.aoms.aomsbackend.common.exception;

import org.springframework.http.HttpStatus;

public class AttendanceRecordNotFoundException extends AomsException {

    public AttendanceRecordNotFoundException() {
        super("Attendance record not found.", HttpStatus.NOT_FOUND);
    }
}
