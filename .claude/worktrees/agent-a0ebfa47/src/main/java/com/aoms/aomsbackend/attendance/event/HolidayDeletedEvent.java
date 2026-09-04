package com.aoms.aomsbackend.attendance.event;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Published inside the delete transaction after a past public holiday is removed.
 * Carries the resolved {@code officeId} so the listener can call Pass 2 without an extra DB lookup.
 * Handled by {@link HolidayRestampListener} once the transaction commits.
 */
public record HolidayDeletedEvent(UUID locationId, UUID officeId, LocalDate date) {}
