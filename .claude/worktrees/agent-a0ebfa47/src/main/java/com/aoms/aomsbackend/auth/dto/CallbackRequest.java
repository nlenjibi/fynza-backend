package com.aoms.aomsbackend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Value;

@Value
public class CallbackRequest {

    @NotBlank(message = "Token is required")
    String token;
}
