package com.aoms.aomsbackend.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * Request body for creating a new public holiday.
 * Both fields are mandatory; past-date validation is enforced at the service layer
 * because the rule differs by role (HR is blocked, SUPER_ADMIN is not).
 */
@Data
public class PublicHolidayCreateRequest {

    /** The date of the public holiday. Must not be null. */
    @NotNull(message = "holidayDate is required")
    private LocalDate holidayDate;

    /** The display name of the holiday (e.g. "Christmas Day"). */
    @NotBlank(message = "name is required")
    @Size(max = 100, message = "name must not exceed 100 characters")
    private String name;
}
