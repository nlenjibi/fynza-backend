package com.aoms.aomsbackend.common.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends AomsException {

    public NotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
