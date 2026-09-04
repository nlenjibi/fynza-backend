package com.aoms.aomsbackend.attendance.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Releases confirmed seat bookings for employees who did not badge in on a given date.
 *
 * <p>Implementations must be idempotent: repeated calls for the same {@code buildingId}
 * and {@code date} must produce no additional side effects.
 */
public interface NoShowReleaseService {

    /**
     * Finds all {@code CONFIRMED} seat bookings for {@code buildingId} on {@code date},
     * checks each employee for a {@code BADGE_IN} event, and transitions bookings with no
     * badge-in to {@code RELEASED} while creating a corresponding {@link com.aoms.aomsbackend.attendance.entity.NoShowRecord}.
     *
     * @param buildingId the building whose bookings are evaluated
     * @param date       the calendar date to process
     * @param zoneId     the building's local timezone, used to anchor the badge-in window
     */
    void releaseNoShows(UUID buildingId, LocalDate date, ZoneId zoneId);
}
