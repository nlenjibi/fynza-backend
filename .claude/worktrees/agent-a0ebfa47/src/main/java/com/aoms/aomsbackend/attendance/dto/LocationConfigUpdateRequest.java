package com.aoms.aomsbackend.attendance.dto;

import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalTime;

@Data
public class LocationConfigUpdateRequest {

    private LocalTime workStartTime;

    @Positive(message = "latenessThresholdMinutes must be a positive integer")
    private Integer latenessThresholdMinutes;

    @Positive(message = "minPresenceDurationMinutes must be a positive integer")
    private Integer minPresenceDurationMinutes;

    private LocalTime noShowReleaseTime;

    @Positive(message = "hotDeskBookingWindowDays must be a positive integer")
    private Integer hotDeskBookingWindowDays;

    @Positive(message = "bookingCancellationCutoffHours must be a positive integer")
    private Integer bookingCancellationCutoffHours;

    @Positive(message = "sessionGapThresholdHours must be a positive integer")
    private Integer sessionGapThresholdHours;

    public boolean isEmpty() {
        return workStartTime == null
                && latenessThresholdMinutes == null
                && minPresenceDurationMinutes == null
                && noShowReleaseTime == null
                && hotDeskBookingWindowDays == null
                && bookingCancellationCutoffHours == null
                && sessionGapThresholdHours == null;
    }
}
