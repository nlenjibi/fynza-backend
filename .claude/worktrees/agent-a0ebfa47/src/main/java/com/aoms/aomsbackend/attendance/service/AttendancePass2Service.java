package com.aoms.aomsbackend.attendance.service;

import java.time.LocalDate;
import java.util.UUID;

public interface AttendancePass2Service {
    void overlay(UUID buildingId, UUID officeId, LocalDate date);
}
