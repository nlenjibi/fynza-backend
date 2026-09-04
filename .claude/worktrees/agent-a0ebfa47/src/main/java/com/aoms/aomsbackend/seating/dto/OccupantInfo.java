package com.aoms.aomsbackend.seating.dto;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class OccupantInfo {
    UUID employeeId;
    String name;
    String department;
}
