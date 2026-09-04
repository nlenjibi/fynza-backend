package com.aoms.aomsbackend.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BadRequestWithCodeException extends AomsException {

    private final String code;

    public BadRequestWithCodeException(String code, String message) {
        super(message, HttpStatus.BAD_REQUEST);
        this.code = code;
    }
}
