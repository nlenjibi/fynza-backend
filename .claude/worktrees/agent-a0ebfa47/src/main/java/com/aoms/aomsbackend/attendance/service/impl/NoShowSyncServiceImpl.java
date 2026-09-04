package com.aoms.aomsbackend.attendance.service.impl;

import com.aoms.aomsbackend.attendance.repository.NoShowReadModelRepository;
import com.aoms.aomsbackend.attendance.service.NoShowSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoShowSyncServiceImpl implements NoShowSyncService {

    private final NoShowReadModelRepository noShowReadModelRepository;

    @Override
    public int syncForDate(LocalDate date) {
        log.info("Starting no-show sync for date={}", date);
        int inserted = noShowReadModelRepository.syncFromNoShowRecord(date);
        log.info("No-show sync complete: inserted={} for date={}", inserted, date);
        return inserted;
    }
}
