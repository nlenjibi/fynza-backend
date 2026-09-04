package com.aoms.aomsbackend.seating.exception;

import com.aoms.aomsbackend.common.exception.NotFoundException;

import java.util.UUID;

public class FloorNotFoundException extends NotFoundException {

    public FloorNotFoundException(UUID floorId) {
        super("Floor not found: " + floorId);
    }
}
