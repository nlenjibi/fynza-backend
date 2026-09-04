package com.aoms.aomsbackend.attendance.service;

import java.time.LocalDate;

public interface AttendancePass1Service {
    void processBadgeEvents(String locationId, LocalDate date);
}