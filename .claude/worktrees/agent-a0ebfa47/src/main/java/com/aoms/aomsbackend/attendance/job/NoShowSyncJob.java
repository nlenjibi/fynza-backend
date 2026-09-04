package com.aoms.aomsbackend.attendance.job;

import com.aoms.aomsbackend.attendance.service.NoShowSyncService;
import com.aoms.aomsbackend.config.SchedulingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class NoShowSyncJob {

    private final NoShowSyncService noShowSyncService;

    @Scheduled(cron = "${sync.no-show-cron:0 0 0 * * *}")
    public void run() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        try {
            int count = noShowSyncService.syncForDate(yesterday);
            log.info("No-show sync job finished: synced={} for date={}", count, yesterday);
        } catch (Exception e) {
            log.error("No-show sync job failed for date={}", yesterday, e);
        }
    }
}
