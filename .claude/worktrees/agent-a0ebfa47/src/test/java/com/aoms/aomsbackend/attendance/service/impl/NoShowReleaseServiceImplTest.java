package com.aoms.aomsbackend.attendance.service.impl;

import com.aoms.aomsbackend.attendance.repository.AttendanceStampLogRepository;
import com.aoms.aomsbackend.attendance.repository.BadgeEventRepository;
import com.aoms.aomsbackend.attendance.repository.NoShowRecordRepository;
import com.aoms.aomsbackend.attendance.repository.NoShowSeatBookingRepository;
import com.aoms.aomsbackend.seating.entity.SeatBooking;
import com.aoms.aomsbackend.seating.entity.SeatBookingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NoShowReleaseServiceImplTest {

    @Mock private NoShowSeatBookingRepository seatBookingRepository;
    @Mock private BadgeEventRepository badgeEventRepository;
    @Mock private NoShowRecordRepository noShowRecordRepository;
    @Mock private AttendanceStampLogRepository stampLogRepository;
    @Mock private NoShowBookingReleaseProcessor bookingReleaseProcessor;
    @Mock private Clock clock;

    @InjectMocks
    private NoShowReleaseServiceImpl service;

    private static final UUID    BUILDING_ID = UUID.randomUUID();
    private static final LocalDate DATE       = LocalDate.of(2026, 4, 27);
    private static final ZoneId   ZONE        = ZoneId.of("Africa/Accra");

    @BeforeEach
    void stubDefaults() {
        when(clock.instant()).thenReturn(Instant.now());
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
        when(stampLogRepository.findByJobNameAndLocationIdAndTargetDate(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(badgeEventRepository.findUserIdsWithBadgeIn(any(), any(), any(), any()))
                .thenReturn(Set.of());
        when(noShowRecordRepository.existsBySeatBookingId(any()))
                .thenReturn(false);
    }

    // ── No badge-in → processor.release called ──────────────────────────────

    @Test
    void releaseNoShows_noBadgeIn_delegatesToProcessor() {
        UUID bookingId = UUID.randomUUID();
        UUID userId    = UUID.randomUUID();
        SeatBooking booking = confirmedBooking(bookingId, userId);

        when(seatBookingRepository.findByBuildingIdAndBookingDateAndStatus(BUILDING_ID, DATE, SeatBookingStatus.CONFIRMED))
                .thenReturn(List.of(booking));
        when(badgeEventRepository.findUserIdsWithBadgeIn(any(), any(), any(), any()))
                .thenReturn(Set.of());

        service.releaseNoShows(BUILDING_ID, DATE, ZONE);

        verify(bookingReleaseProcessor).release(argThat(b ->
                bookingId.equals(b.getId())), any(LocalDate.class));
    }

    // ── Badge-in exists → processor not called ──────────────────────────────

    @Test
    void releaseNoShows_badgeInExists_skipsBooking() {
        UUID userId    = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        SeatBooking booking = confirmedBooking(bookingId, userId);

        when(seatBookingRepository.findByBuildingIdAndBookingDateAndStatus(BUILDING_ID, DATE, SeatBookingStatus.CONFIRMED))
                .thenReturn(List.of(booking));
        when(badgeEventRepository.findUserIdsWithBadgeIn(any(), any(), any(), any()))
                .thenReturn(Set.of(userId));

        service.releaseNoShows(BUILDING_ID, DATE, ZONE);

        verify(bookingReleaseProcessor, never()).release(any(), any());
    }

    // ── Already-released → processor not called (idempotency guard) ─────────

    @Test
    void releaseNoShows_alreadyHasNoShowRecord_skipsBooking() {
        UUID bookingId = UUID.randomUUID();
        SeatBooking booking = confirmedBooking(bookingId, UUID.randomUUID());

        when(seatBookingRepository.findByBuildingIdAndBookingDateAndStatus(BUILDING_ID, DATE, SeatBookingStatus.CONFIRMED))
                .thenReturn(List.of(booking));
        when(noShowRecordRepository.existsBySeatBookingId(bookingId))
                .thenReturn(true);

        service.releaseNoShows(BUILDING_ID, DATE, ZONE);

        verify(bookingReleaseProcessor, never()).release(any(), any());
    }

    // ── Idempotent re-run: no confirmed bookings ─────────────────────────────

    @Test
    void releaseNoShows_idempotentRerun_noConfirmedBookings_logsZeroCounts() {
        when(seatBookingRepository.findByBuildingIdAndBookingDateAndStatus(BUILDING_ID, DATE, SeatBookingStatus.CONFIRMED))
                .thenReturn(List.of());

        service.releaseNoShows(BUILDING_ID, DATE, ZONE);

        verify(bookingReleaseProcessor, never()).release(any(), any());
        verify(stampLogRepository).save(argThat(log ->
                "SUCCESS".equals(log.getStatus()) &&
                NoShowReleaseServiceImpl.JOB_NAME.equals(log.getJobName()) &&
                0 == log.getRecordsProcessed()));
    }

    // ── Multiple bookings: mixed badge-in / no badge-in ──────────────────────

    @Test
    void releaseNoShows_mixedBookings_onlyReleasesNoBadgeIn() {
        UUID userWithBadge    = UUID.randomUUID();
        UUID userWithoutBadge = UUID.randomUUID();
        UUID bookingWithBadge    = UUID.randomUUID();
        UUID bookingWithoutBadge = UUID.randomUUID();

        SeatBooking b1 = confirmedBooking(bookingWithBadge, userWithBadge);
        SeatBooking b2 = confirmedBooking(bookingWithoutBadge, userWithoutBadge);

        when(seatBookingRepository.findByBuildingIdAndBookingDateAndStatus(BUILDING_ID, DATE, SeatBookingStatus.CONFIRMED))
                .thenReturn(List.of(b1, b2));
        when(badgeEventRepository.findUserIdsWithBadgeIn(any(), any(), any(), any()))
                .thenReturn(Set.of(userWithBadge));

        service.releaseNoShows(BUILDING_ID, DATE, ZONE);

        verify(bookingReleaseProcessor).release(argThat(b ->
                bookingWithoutBadge.equals(b.getId())), any(LocalDate.class));
    }

    // ── Job log persisted ─────────────────────────────────────────────────────

    @Test
    void releaseNoShows_always_persistsJobLog() {
        when(seatBookingRepository.findByBuildingIdAndBookingDateAndStatus(BUILDING_ID, DATE, SeatBookingStatus.CONFIRMED))
                .thenReturn(List.of());

        service.releaseNoShows(BUILDING_ID, DATE, ZONE);

        verify(stampLogRepository).save(argThat(log ->
                NoShowReleaseServiceImpl.JOB_NAME.equals(log.getJobName()) &&
                BUILDING_ID.equals(log.getLocationId()) &&
                DATE.equals(log.getTargetDate()) &&
                log.getCompletedAt() != null));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private SeatBooking confirmedBooking(UUID id, UUID userId) {
        SeatBooking b = new SeatBooking();
        b.setId(id);
        b.setUserId(userId);
        b.setBuildingId(BUILDING_ID);
        b.setBookingDate(DATE);
        b.setStatus(SeatBookingStatus.CONFIRMED);
        return b;
    }
}
