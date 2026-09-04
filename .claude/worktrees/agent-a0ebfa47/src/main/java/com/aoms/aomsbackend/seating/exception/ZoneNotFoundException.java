package com.aoms.aomsbackend.seating.exception;

import com.aoms.aomsbackend.common.exception.NotFoundException;

import java.util.UUID;

public class ZoneNotFoundException extends NotFoundException {

    public ZoneNotFoundException(UUID zoneId) {
        super("Zone not found: " + zoneId);
    }
}
