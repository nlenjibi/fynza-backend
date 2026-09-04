package com.aoms.aomsbackend.attendance.service;

import java.time.LocalDate;

public interface NoShowSyncService {

    int syncForDate(LocalDate date);
}
