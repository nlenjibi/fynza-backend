package com.aoms.aomsbackend.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class AomsException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected AomsException(String message, HttpStatus status) {
        this(message, status, null);
    }

    protected AomsException(String message, HttpStatus status, String code) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
