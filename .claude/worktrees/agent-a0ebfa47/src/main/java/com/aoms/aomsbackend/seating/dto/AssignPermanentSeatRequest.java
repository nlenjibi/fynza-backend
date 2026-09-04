package com.aoms.aomsbackend.seating.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AssignPermanentSeatRequest {

    @NotNull(message = "User ID is required.")
    private UUID userId;
}
