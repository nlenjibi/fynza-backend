package com.aoms.aomsbackend.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends AomsException {

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
