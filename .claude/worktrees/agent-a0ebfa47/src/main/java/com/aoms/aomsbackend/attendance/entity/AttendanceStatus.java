package com.aoms.aomsbackend.attendance.entity;

/**
 * Attendance status values stored in the {@code attendance_status_enum} reference table.
 * Mapped as {@code VARCHAR(30)} in the {@code attendance_record.status} column.
 */
public enum AttendanceStatus {
    PRESENT,
    LATE,
    INSUFFICIENT_HOURS,
    ABSENT,
    REMOTE,
    ON_LEAVE,
    PUBLIC_HOLIDAY
}
