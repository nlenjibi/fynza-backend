package com.aoms.aomsbackend.attendance.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * Request body for updating an existing public holiday.
 * Both fields are optional; at least one should be provided (checked at service layer).
 * Past holiday validation is enforced at the service layer.
 */
@Data
public class PublicHolidayUpdateRequest {

    /** New date for the holiday — if provided, must not conflict with an existing holiday on that date. */
    private LocalDate holidayDate;

    /** New display name for the holiday. */
    @Size(max = 100, message = "name must not exceed 100 characters")
    private String name;
}
