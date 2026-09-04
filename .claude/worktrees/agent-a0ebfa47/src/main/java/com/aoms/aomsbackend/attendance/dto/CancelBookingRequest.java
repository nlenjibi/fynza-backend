package com.aoms.aomsbackend.attendance.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for the cancel-booking endpoint.
 * The body itself is optional; when provided, {@code cancellationReason} may be included.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CancelBookingRequest {
    @Size(max = 500, message = "Cancellation reason must not exceed 500 characters")
    private String cancellationReason;
}
