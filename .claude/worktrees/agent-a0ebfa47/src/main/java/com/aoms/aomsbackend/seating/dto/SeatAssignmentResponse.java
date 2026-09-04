package com.aoms.aomsbackend.seating.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SeatAssignmentResponse {

    private UUID seatId;
    private String seatLabel;
    private UUID assignedUserId;
    private String assignedUserName;
}
