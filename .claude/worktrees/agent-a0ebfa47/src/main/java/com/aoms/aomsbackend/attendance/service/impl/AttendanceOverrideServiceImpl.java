package com.aoms.aomsbackend.attendance.service.impl;

import com.aoms.aomsbackend.attendance.dto.AttendanceRecordOverrideRequest;
import com.aoms.aomsbackend.attendance.dto.AttendanceRecordOverrideResponse;
import com.aoms.aomsbackend.attendance.dto.AttendanceRecordRevertRequest;
import com.aoms.aomsbackend.attendance.entity.AttendanceRecord;
import com.aoms.aomsbackend.attendance.entity.AttendanceStatus;
import com.aoms.aomsbackend.attendance.repository.AttendanceRecordRepository;
import com.aoms.aomsbackend.attendance.service.AttendanceOverrideService;
import com.aoms.aomsbackend.common.exception.BadRequestException;
import com.aoms.aomsbackend.common.exception.ConflictException;
import com.aoms.aomsbackend.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttendanceOverrideServiceImpl implements AttendanceOverrideService {

    private final AttendanceRecordRepository repository;

    @Override
    @Transactional
    public AttendanceRecordOverrideResponse override(UUID id, AttendanceRecordOverrideRequest request, UUID actorId) {
        AttendanceRecord attendanceRecord = findActiveRecord(id);

        if (attendanceRecord.isOverridden()) {
            throw new ConflictException(
                    "Record is already overridden. Revert before overriding again.",
                    "ALREADY_OVERRIDDEN");
        }

        validateOverrideStatus(attendanceRecord, request.getStatus());

        attendanceRecord.setOriginalStatus(attendanceRecord.getStatus().name());
        attendanceRecord.setStatus(request.getStatus());
        attendanceRecord.setOverridden(true);
        attendanceRecord.setOverrideReason(request.getOverrideReason());
        attendanceRecord.setOverriddenAt(OffsetDateTime.now());
        attendanceRecord.setOverriddenBy(actorId);

        return toResponse(repository.save(attendanceRecord));
    }

    @Override
    @Transactional
    public AttendanceRecordOverrideResponse revert(UUID id, AttendanceRecordRevertRequest request, UUID actorId) {
        AttendanceRecord attendanceRecord = findActiveRecord(id);

        if (!attendanceRecord.isOverridden()) {
            throw new ConflictException("Record is not overridden.", "NOT_OVERRIDDEN");
        }

        attendanceRecord.setStatus(AttendanceStatus.valueOf(attendanceRecord.getOriginalStatus()));
        attendanceRecord.setOverridden(false);
        attendanceRecord.setRevertReasons(appendRevertReason(attendanceRecord.getRevertReasons(), request.getRevertReason(), actorId));

        return toResponse(repository.save(attendanceRecord));
    }

    private void validateOverrideStatus(AttendanceRecord attendanceRecord, AttendanceStatus status) {
        if (status == AttendanceStatus.PUBLIC_HOLIDAY && attendanceRecord.getWorkSessionId() != null) {
            throw new BadRequestException("Cannot mark a day with work sessions as PUBLIC_HOLIDAY.");
        }
    }

    private String appendRevertReason(String existingReasons, String revertReason, UUID actorId) {
        String entry = OffsetDateTime.now() + " | actor=" + actorId + " | reason=" + revertReason;
        if (existingReasons == null || existingReasons.isBlank()) {
            return entry;
        }
        return existingReasons + System.lineSeparator() + entry;
    }

    private AttendanceRecord findActiveRecord(UUID id) {
        AttendanceRecord attendanceRecord = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Attendance record not found: " + id));
        if (attendanceRecord.getDeletedAt() != null) {
            throw new NotFoundException("Attendance record not found: " + id);
        }
        return attendanceRecord;
    }

    private AttendanceRecordOverrideResponse toResponse(AttendanceRecord attendanceRecord) {
        return AttendanceRecordOverrideResponse.builder()
                .id(attendanceRecord.getId())
                .userId(attendanceRecord.getUserId())
                .buildingId(attendanceRecord.getBuildingId())
                .recordDate(attendanceRecord.getRecordDate())
                .status(attendanceRecord.getStatus())
                .workSessionId(attendanceRecord.getWorkSessionId())
                .isOverridden(attendanceRecord.isOverridden())
                .overrideReason(attendanceRecord.getOverrideReason())
                .originalStatus(attendanceRecord.getOriginalStatus() == null ? null : AttendanceStatus.valueOf(attendanceRecord.getOriginalStatus()))
                .revertReasons(attendanceRecord.getRevertReasons())
                .overriddenBy(attendanceRecord.getOverriddenBy())
                .overriddenAt(attendanceRecord.getOverriddenAt())
                .createdAt(attendanceRecord.getCreatedAt())
                .updatedAt(attendanceRecord.getUpdatedAt())
                .build();
    }
}
