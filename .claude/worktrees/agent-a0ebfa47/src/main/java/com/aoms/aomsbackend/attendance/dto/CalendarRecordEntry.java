package com.aoms.aomsbackend.attendance.dto;

import com.aoms.aomsbackend.attendance.entity.AttendanceStatus;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

/**
 * Single entry in the calendar snapshot representing one direct report's
 * attendance for the queried date. {@code status} is {@code null} when
 * no attendance record exists for that employee on the given date.
 */
@Value
@Builder
public class CalendarRecordEntry {
    UUID employeeId;
    String employeeName;
    AttendanceStatus status;
    Boolean isOverridden;
}
