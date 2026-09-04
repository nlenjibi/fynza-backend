package com.aoms.aomsbackend.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class LocationDowChartDto {
    UUID buildingId;
    String department;
    int year;
    int month;
    int weekNumber;
    String dayOfWeek;
    String dayOfWeekShort;
    int dayOrder;
    long inOfficeCount;
    long remoteCount;
    long onLeaveCount;
    long absentCount;
    long totalCount;
}
