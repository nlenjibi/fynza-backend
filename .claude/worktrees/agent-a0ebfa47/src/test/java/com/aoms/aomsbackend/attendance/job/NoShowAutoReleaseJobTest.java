package com.aoms.aomsbackend.attendance.job;

import com.aoms.aomsbackend.attendance.entity.LocationConfig;
import com.aoms.aomsbackend.attendance.entity.OfficeBuilding;
import com.aoms.aomsbackend.attendance.repository.LocationConfigRepository;
import com.aoms.aomsbackend.attendance.repository.OfficeBuildingRepository;
import com.aoms.aomsbackend.attendance.service.NoShowReleaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NoShowAutoReleaseJobTest {

    @Mock private OfficeBuildingRepository officeBuildingRepository;
    @Mock private LocationConfigRepository locationConfigRepository;
    @Mock private NoShowReleaseService noShowReleaseService;

    private static final UUID BUILDING_ID = UUID.randomUUID();
    private static final String TIMEZONE  = "Africa/Accra";
    private static final ZoneId ZONE      = ZoneId.of(TIMEZONE);


    private NoShowAutoReleaseJob jobAt(LocalTime clockTime) {
        Instant instant = LocalDate.of(2026, 4, 27).atTime(clockTime).atZone(ZONE).toInstant();
        Clock fixed = Clock.fixed(instant, ZONE);
        return new NoShowAutoReleaseJob(officeBuildingRepository, locationConfigRepository,
                noShowReleaseService, fixed);
    }

    @BeforeEach
    void stubDefaults() {
        OfficeBuilding building = new OfficeBuilding();
        building.setId(BUILDING_ID);
        building.setActive(true);
        when(officeBuildingRepository.findByActiveTrue()).thenReturn(List.of(building));
        when(officeBuildingRepository.findTimezoneByBuildingId(BUILDING_ID))
                .thenReturn(Optional.of(TIMEZONE));
    }

    // ── Timezone cutoff: before release time → skip ──────────────────────────

    @Test
    void run_currentTimeBeforeReleaseTime_skipsLocation() {
        LocationConfig config = locationConfig(LocalTime.of(10, 0));
        when(locationConfigRepository.findByBuildingId(BUILDING_ID)).thenReturn(Optional.of(config));

        NoShowAutoReleaseJob job = jobAt(LocalTime.of(9, 30));
        job.run();

        verify(noShowReleaseService, never()).releaseNoShows(any(), any(), any());
    }

    // ── Timezone cutoff: at or after release time → execute ──────────────────

    @Test
    void run_currentTimeAtReleaseTime_callsService() {
        LocationConfig config = locationConfig(LocalTime.of(10, 0));
        when(locationConfigRepository.findByBuildingId(BUILDING_ID)).thenReturn(Optional.of(config));

        NoShowAutoReleaseJob job = jobAt(LocalTime.of(10, 0));
        job.run();

        verify(noShowReleaseService).releaseNoShows(any(UUID.class), any(LocalDate.class), any(ZoneId.class));
    }

    // ── No LocationConfig → skip ─────────────────────────────────────────────

    @Test
    void run_noLocationConfig_skipsLocation() {
        when(locationConfigRepository.findByBuildingId(BUILDING_ID)).thenReturn(Optional.empty());

        NoShowAutoReleaseJob job = jobAt(LocalTime.of(11, 0));
        job.run();

        verify(noShowReleaseService, never()).releaseNoShows(any(), any(), any());
    }

    // ── No release time configured → skip ────────────────────────────────────

    @Test
    void run_noShowReleaseTimeNull_skipsLocation() {
        LocationConfig config = locationConfig(null);
        when(locationConfigRepository.findByBuildingId(BUILDING_ID)).thenReturn(Optional.of(config));

        NoShowAutoReleaseJob job = jobAt(LocalTime.of(11, 0));
        job.run();

        verify(noShowReleaseService, never()).releaseNoShows(any(), any(), any());
    }

    // ── No timezone found → skip ─────────────────────────────────────────────

    @Test
    void run_noTimezoneFound_skipsLocation() {
        LocationConfig config = locationConfig(LocalTime.of(10, 0));
        when(locationConfigRepository.findByBuildingId(BUILDING_ID)).thenReturn(Optional.of(config));
        when(officeBuildingRepository.findTimezoneByBuildingId(BUILDING_ID)).thenReturn(Optional.empty());

        NoShowAutoReleaseJob job = jobAt(LocalTime.of(11, 0));
        job.run();

        verify(noShowReleaseService, never()).releaseNoShows(any(), any(), any());
    }

    // ── Service throws → job continues without propagating ──────────────────

    @Test
    void run_serviceThrows_doesNotPropagateException() {
        LocationConfig config = locationConfig(LocalTime.of(10, 0));
        when(locationConfigRepository.findByBuildingId(BUILDING_ID)).thenReturn(Optional.of(config));
        doThrow(new RuntimeException("DB failure"))
                .when(noShowReleaseService).releaseNoShows(any(), any(), any());

        NoShowAutoReleaseJob job = jobAt(LocalTime.of(11, 0));
        // should not throw
        job.run();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private LocationConfig locationConfig(LocalTime releaseTime) {
        LocationConfig c = new LocationConfig();
        c.setBuildingId(BUILDING_ID);
        c.setNoShowReleaseTime(releaseTime);
        return c;
    }
}
