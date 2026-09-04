package com.aoms.aomsbackend.attendance.service.impl;

import com.aoms.aomsbackend.attendance.entity.AttendanceStampLog;
import com.aoms.aomsbackend.seating.entity.SeatBooking;
import com.aoms.aomsbackend.seating.entity.SeatBookingStatus;
import com.aoms.aomsbackend.attendance.repository.AttendanceStampLogRepository;
import com.aoms.aomsbackend.attendance.repository.BadgeEventRepository;
import com.aoms.aomsbackend.attendance.repository.NoShowRecordRepository;
import com.aoms.aomsbackend.attendance.repository.NoShowSeatBookingRepository;
import com.aoms.aomsbackend.attendance.service.NoShowReleaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Releases confirmed seat bookings for employees who did not badge in on the booking date.
 *
 * <p>The implementation is designed to be idempotent: re-running for the same building and date
 * produces no side effects because only {@code CONFIRMED} bookings are fetched and a unique
 * constraint on {@code no_show_record.seat_booking_id} prevents duplicate records.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoShowReleaseServiceImpl implements NoShowReleaseService {

    static final String JOB_NAME = "no_show_auto_release";

    private final NoShowSeatBookingRepository seatBookingRepository;
    private final BadgeEventRepository badgeEventRepository;
    private final NoShowRecordRepository noShowRecordRepository;
    private final AttendanceStampLogRepository stampLogRepository;
    private final NoShowBookingReleaseProcessor bookingReleaseProcessor;
    private final Clock clock;

    /**
     * Releases all confirmed seat bookings at {@code buildingId} on {@code date} for which
     * no {@code BADGE_IN} event exists within the building's local calendar day.
     *
     * <p>Processing steps:
     * <ol>
     *   <li>Fetch all {@code CONFIRMED} bookings for the building and date (one query).</li>
     *   <li>Bulk-fetch the distinct user IDs that have a {@code BADGE_IN} event today at
     *       this building (one query — avoids N+1). The window is anchored to the building's
     *       local midnight so events from buildings ahead of UTC are not missed.</li>
     *   <li>For each booking: skip if the employee badged in or if a {@link com.aoms.aomsbackend.attendance.entity.NoShowRecord}
     *       already exists for that booking (idempotency guard).</li>
     *   <li>Otherwise: delegate to {@link NoShowBookingReleaseProcessor#release} which commits
     *       each booking in its own transaction — a failure for one booking does not roll back
     *       releases already committed for others.</li>
     *   <li>Persist a {@code job_execution_log} entry regardless of outcome.</li>
     * </ol>
     *
     * @param buildingId the building whose bookings are evaluated
     * @param date       the calendar date to process (normally today)
     * @param zoneId     the building's local timezone, used to anchor the badge-in window
     */
    @Override
    @Transactional
    public void releaseNoShows(UUID buildingId, LocalDate date, ZoneId zoneId) {
        log.info("Starting no-show release for building {} on {}", buildingId, date);
        UUID runId = UUID.randomUUID();
        int checked = 0, released = 0, skipped = 0, failed = 0;

        try {
            List<SeatBooking> confirmedBookings = seatBookingRepository
                    .findByBuildingIdAndBookingDateAndStatus(buildingId, date, SeatBookingStatus.CONFIRMED);

            checked = confirmedBookings.size();
            if (confirmedBookings.isEmpty()) {
                log.info("No CONFIRMED bookings for building {} on {}", buildingId, date);
                logJobExecution(buildingId, date, "SUCCESS", 0, 0, 0, 0, runId);
                return;
            }

            Set<UUID> userIds = confirmedBookings.stream()
                    .map(SeatBooking::getUserId)
                    .collect(Collectors.toSet());

            OffsetDateTime dayStart = date.atStartOfDay(zoneId).toOffsetDateTime();
            OffsetDateTime dayEnd = date.plusDays(1).atStartOfDay(zoneId).toOffsetDateTime();

            Set<UUID> usersWithBadgeIn = badgeEventRepository
                    .findUserIdsWithBadgeIn(buildingId, userIds, dayStart, dayEnd);

            for (SeatBooking booking : confirmedBookings) {
                try {
                    if (usersWithBadgeIn.contains(booking.getUserId())) {
                        skipped++;
                        continue;
                    }
                    if (noShowRecordRepository.existsBySeatBookingId(booking.getId())) {
                        skipped++;
                        continue;
                    }

                    bookingReleaseProcessor.release(booking, date);
                    released++;
                } catch (Exception e) {
                    failed++;
                    log.error("Error releasing booking {}: {}", booking.getId(), e.getMessage());
                }
            }

            logJobExecution(buildingId, date, "SUCCESS", checked, released, skipped, failed, runId);
            log.info("No-show release done — checked={}, released={}, skipped={}, failed={}",
                    checked, released, skipped, failed);

        } catch (Exception e) {
            log.error("No-show release failed for building {} on {}: {}", buildingId, date, e.getMessage());
            logJobExecution(buildingId, date, "FAILED", checked, released, skipped, failed, runId);
            throw e;
        }
    }

    /**
     * Upserts a {@code job_execution_log} row for the given building and date.
     *
     * <p>If a row already exists (e.g. from a prior run on the same day) its counters and
     * status are overwritten. {@code startedAt} is only set on first insert to preserve the
     * original start timestamp across re-runs.
     *
     * <p>Column semantics for this job:
     * <ul>
     *   <li>{@code records_processed} — total {@code CONFIRMED} bookings evaluated</li>
     *   <li>{@code records_released} — bookings transitioned to {@code RELEASED}</li>
     *   <li>{@code records_skipped} — bookings skipped (badge-in present or already released)</li>
     *   <li>{@code records_failed} — bookings that threw an unexpected error</li>
     * </ul>
     *
     * @param locationId the building ID
     * @param targetDate the date the job ran for
     * @param status     {@code "SUCCESS"} or {@code "FAILED"}
     * @param processed  total bookings checked
     * @param releasedCount  bookings transitioned to RELEASED
     * @param skipped    bookings not released
     * @param failed     bookings that encountered an error
     * @param runId      unique identifier for this execution
     */
    private void logJobExecution(UUID locationId, LocalDate targetDate,
                                 String status, int processed, int releasedCount,
                                 int skipped, int failed, UUID runId) {
        Optional<AttendanceStampLog> existing =
                stampLogRepository.findByJobNameAndLocationIdAndTargetDate(JOB_NAME, locationId, targetDate);

        AttendanceStampLog entry = existing.orElseGet(AttendanceStampLog::new);
        entry.setJobName(JOB_NAME);
        entry.setLocationId(locationId);
        entry.setTargetDate(targetDate);
        entry.setStatus(status);
        entry.setRecordsProcessed(processed);
        entry.setRecordsReleased(releasedCount);
        entry.setRecordsSkipped(skipped);
        entry.setRecordsFailed(failed);
        entry.setRunId(runId);

        if (existing.isEmpty()) {
            entry.setStartedAt(LocalDateTime.now(clock));
        }
        entry.setCompletedAt(LocalDateTime.now(clock));
        stampLogRepository.save(entry);
    }
}
