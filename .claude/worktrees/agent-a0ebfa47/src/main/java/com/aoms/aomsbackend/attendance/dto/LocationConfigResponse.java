package com.aoms.aomsbackend.attendance.dto;

import lombok.Builder;
import lombok.Value;

import com.aoms.aomsbackend.attendance.entity.SeatVisibilityMode;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Value
@Builder
public class LocationConfigResponse {
    UUID id;
    UUID buildingId;
    LocalTime workStartTime;
    Integer latenessThresholdMinutes;
    Integer minPresenceDurationMinutes;
    LocalTime noShowReleaseTime;
    Integer hotDeskBookingWindowDays;
    Integer bookingCancellationCutoffHours;
    SeatVisibilityMode seatVisibilityMode;
    Integer sessionGapThresholdHours;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
