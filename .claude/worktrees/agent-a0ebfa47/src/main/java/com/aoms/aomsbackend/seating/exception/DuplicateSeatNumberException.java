package com.aoms.aomsbackend.seating.exception;

import com.aoms.aomsbackend.common.exception.BadRequestException;

public class DuplicateSeatNumberException extends BadRequestException {

    public DuplicateSeatNumberException(String seatNumber) {
        super("Seat number already exists in this zone: " + seatNumber);
    }
}
