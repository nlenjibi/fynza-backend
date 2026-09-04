package com.aoms.aomsbackend.attendance.job;

import com.aoms.aomsbackend.attendance.entity.LocationConfig;
import com.aoms.aomsbackend.attendance.entity.OfficeBuilding;
import com.aoms.aomsbackend.attendance.repository.LocationConfigRepository;
import com.aoms.aomsbackend.attendance.repository.OfficeBuildingRepository;
import com.aoms.aomsbackend.attendance.service.NoShowReleaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * Scheduled job that automatically releases confirmed seat bookings where no badge-in
 * event was recorded for the booking date, effectively freeing up unoccupied seats.
 *
 * <p>The job is cron-driven (default: 10:00 AM daily) and iterates every active building.
 * Each building is processed independently so a failure in one does not block the others.
 * Cron expression is configurable via the {@code NO_SHOW_RELEASE_CRON} environment variable.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoShowAutoReleaseJob {

    private final OfficeBuildingRepository officeBuildingRepository;
    private final LocationConfigRepository locationConfigRepository;
    private final NoShowReleaseService noShowReleaseService;
    private final Clock clock;

    /**
     * Entry point invoked by the Spring scheduler.
     *
     * <p>Loads all active buildings and delegates each to {@link #processBuilding}.
     * Exceptions thrown by a single building are caught and logged so that remaining
     * buildings continue to be processed in the same run.
     */
    @Scheduled(cron = "${NO_SHOW_RELEASE_CRON:0 0 10 * * *}")
    public void run() {
        LocalDate today = LocalDate.now(clock);
        log.info("NoShowAutoReleaseJob triggered for {}", today);

        List<OfficeBuilding> activeBuildings = officeBuildingRepository.findByActiveTrue();
        for (OfficeBuilding building : activeBuildings) {
            try {
                processBuilding(building, today);
            } catch (Exception e) {
                log.error("No-show release failed for building {}: {}", building.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Applies the no-show release logic for a single building.
     *
     * <p>A building is skipped (with a log entry) if any of the following preconditions
     * are not met:
     * <ul>
     *   <li>A {@link LocationConfig} exists for the building.</li>
     *   <li>{@code LocationConfig.noShowReleaseTime} is set.</li>
     *   <li>A timezone can be resolved from the building's parent {@code organisation}.</li>
     *   <li>The current wall-clock time in that timezone has reached or passed
     *       {@code noShowReleaseTime}.</li>
     * </ul>
     *
     * @param building the active building to process
     * @param today    the calendar date for which bookings are evaluated
     */
    private void processBuilding(OfficeBuilding building, LocalDate today) {
        Optional<LocationConfig> configOpt = locationConfigRepository.findByBuildingId(building.getId());
        if (configOpt.isEmpty()) {
            log.debug("Skipping building {} — no LocationConfig found", building.getId());
            return;
        }

        LocationConfig config = configOpt.get();
        if (config.getNoShowReleaseTime() == null) {
            log.debug("Skipping building {} — no_show_release_time not configured", building.getId());
            return;
        }

        Optional<String> timezoneOpt = officeBuildingRepository.findTimezoneByBuildingId(building.getId());
        if (timezoneOpt.isEmpty()) {
            log.warn("Skipping building {} — no timezone found in organisation", building.getId());
            return;
        }

        ZoneId zone = ZoneId.of(timezoneOpt.get());
        LocalTime currentLocalTime = LocalTime.now(clock.withZone(zone));
        if (currentLocalTime.isBefore(config.getNoShowReleaseTime())) {
            log.debug("Skipping building {} — current time {} has not yet reached release time {}",
                    building.getId(), currentLocalTime, config.getNoShowReleaseTime());
            return;
        }

        noShowReleaseService.releaseNoShows(building.getId(), today, zone);
    }
}
