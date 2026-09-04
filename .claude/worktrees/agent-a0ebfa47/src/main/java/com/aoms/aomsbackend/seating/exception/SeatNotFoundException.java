package com.aoms.aomsbackend.seating.exception;

import com.aoms.aomsbackend.common.exception.NotFoundException;

import java.util.UUID;

public class SeatNotFoundException extends NotFoundException {

    public SeatNotFoundException(UUID seatId) {
        super("Seat not found: " + seatId);
    }
}
