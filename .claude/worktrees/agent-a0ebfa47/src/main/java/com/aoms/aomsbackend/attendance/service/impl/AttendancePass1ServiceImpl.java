package com.aoms.aomsbackend.attendance.service.impl;

import com.aoms.aomsbackend.attendance.service.AttendancePass1Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@Slf4j
public class AttendancePass1ServiceImpl implements AttendancePass1Service {

    @Override
    public void processBadgeEvents(String locationId, LocalDate date) {
        log.info("Processing badge events for location {} on {}", locationId, date);
    }
}