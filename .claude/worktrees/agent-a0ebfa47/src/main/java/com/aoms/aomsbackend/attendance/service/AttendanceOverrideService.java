package com.aoms.aomsbackend.attendance.service;

import com.aoms.aomsbackend.attendance.dto.AttendanceRecordOverrideRequest;
import com.aoms.aomsbackend.attendance.dto.AttendanceRecordOverrideResponse;
import com.aoms.aomsbackend.attendance.dto.AttendanceRecordRevertRequest;

import java.util.UUID;

/**
 * The interface Attendance override service.
 */
public interface AttendanceOverrideService {

    /**
     * Override attendance record response.
     *
     * @param id      the id
     * @param request the request
     * @param actorId the actor id
     * @return the attendance record response
     */
    AttendanceRecordOverrideResponse override(UUID id, AttendanceRecordOverrideRequest request, UUID actorId);

    /**
     * Revert attendance record response.
     *
     * @param id      the id
     * @param request the request
     * @param actorId the actor id
     * @return the attendance record response
     */
    AttendanceRecordOverrideResponse revert(UUID id, AttendanceRecordRevertRequest request, UUID actorId);
}
