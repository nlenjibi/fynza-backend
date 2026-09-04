package com.aoms.aomsbackend.attendance.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for the calendar snapshot endpoint. Contains the queried date
 * and one {@link CalendarRecordEntry} per direct report.
 */
@Value
@Builder
public class CalendarDayResponse {
    LocalDate date;
    List<CalendarRecordEntry> records;
}
