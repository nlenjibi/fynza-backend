package com.aoms.aomsbackend.attendance.dto;

import lombok.Builder;
import lombok.Value;

/**
 * Response returned after deleting a public holiday.
 * {@code restampQueued} is {@code true} when the deleted holiday was in the past
 * and a restamp message has been published to the SNS topic so that Pass 2 can
 * re-stamp attendance records for the affected date.
 */
@Value
@Builder
public class DeleteHolidayResponse {

    /** Whether a Pass 2 restamp was triggered for the deleted holiday's date. */
    boolean restampQueued;
}
