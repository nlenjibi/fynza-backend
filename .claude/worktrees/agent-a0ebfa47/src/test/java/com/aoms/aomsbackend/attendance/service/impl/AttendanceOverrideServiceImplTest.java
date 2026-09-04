package com.aoms.aomsbackend.attendance.service.impl;

import com.aoms.aomsbackend.attendance.dto.AttendanceRecordOverrideRequest;
import com.aoms.aomsbackend.attendance.dto.AttendanceRecordOverrideResponse;
import com.aoms.aomsbackend.attendance.dto.AttendanceRecordRevertRequest;
import com.aoms.aomsbackend.attendance.entity.AttendanceRecord;
import com.aoms.aomsbackend.attendance.entity.AttendanceStatus;
import com.aoms.aomsbackend.attendance.repository.AttendanceRecordRepository;
import com.aoms.aomsbackend.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceOverrideServiceImplTest {

    @Mock
    private AttendanceRecordRepository repository;

    @InjectMocks
    private AttendanceOverrideServiceImpl service;

    @Test
    void override_publicHolidayWithWorkSession_throwsBadRequest() {
        AttendanceRecord attendanceRecord = baseRecord();
        attendanceRecord.setWorkSessionId(UUID.randomUUID());
        when(repository.findById(attendanceRecord.getId())).thenReturn(Optional.of(attendanceRecord));

        AttendanceRecordOverrideRequest request = new AttendanceRecordOverrideRequest();
        request.setStatus(AttendanceStatus.PUBLIC_HOLIDAY);
        request.setOverrideReason("invalid override");
        UUID actorId = UUID.randomUUID();
        UUID recordId = attendanceRecord.getId();

        assertThatThrownBy(() -> service.override(recordId, request, actorId))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cannot mark a day with work sessions as PUBLIC_HOLIDAY.");
    }

    @Test
    void revert_appendsRevertReason_andKeepsAuditHistory() {
        UUID actorId = UUID.randomUUID();
        AttendanceRecord attendanceRecord = baseRecord();
        attendanceRecord.setStatus(AttendanceStatus.PRESENT);
        attendanceRecord.setOriginalStatus("ABSENT");
        attendanceRecord.setOverridden(true);
        attendanceRecord.setOverrideReason("original override reason");
        attendanceRecord.setRevertReasons("2026-04-27T10:00:00Z | actor=11111111-1111-1111-1111-111111111111 | reason=first revert");
        attendanceRecord.setOverriddenBy(UUID.randomUUID());
        attendanceRecord.setOverriddenAt(OffsetDateTime.parse("2026-04-27T10:00:00Z"));
        when(repository.findById(attendanceRecord.getId())).thenReturn(Optional.of(attendanceRecord));
        when(repository.save(any(AttendanceRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AttendanceRecordRevertRequest request = new AttendanceRecordRevertRequest();
        request.setRevertReason("second revert");

        AttendanceRecordOverrideResponse response = service.revert(attendanceRecord.getId(), request, actorId);

        ArgumentCaptor<AttendanceRecord> captor = ArgumentCaptor.forClass(AttendanceRecord.class);
        org.mockito.Mockito.verify(repository).save(captor.capture());
        AttendanceRecord saved = captor.getValue();

        assertThat(saved.isOverridden()).isFalse();
        assertThat(saved.getStatus()).isEqualTo(AttendanceStatus.ABSENT);
        assertThat(saved.getOriginalStatus()).isEqualTo("ABSENT");
        assertThat(saved.getOverrideReason()).isEqualTo("original override reason");
        assertThat(saved.getOverriddenBy()).isNotNull();
        assertThat(saved.getOverriddenAt()).isEqualTo(OffsetDateTime.parse("2026-04-27T10:00:00Z"));
        assertThat(saved.getRevertReasons()).contains("first revert");
        assertThat(saved.getRevertReasons()).contains("second revert");
        assertThat(saved.getRevertReasons()).contains(actorId.toString());
        assertThat(response.getOriginalStatus()).isEqualTo(AttendanceStatus.ABSENT);
        assertThat(response.getRevertReasons()).contains("second revert");
    }

    private AttendanceRecord baseRecord() {
        AttendanceRecord attendanceRecord = new AttendanceRecord();
        attendanceRecord.setId(UUID.randomUUID());
        attendanceRecord.setUserId(UUID.randomUUID());
        attendanceRecord.setOfficeId(UUID.randomUUID());
        attendanceRecord.setBuildingId(UUID.randomUUID());
        attendanceRecord.setRecordDate(LocalDate.of(2025, 1, 10));
        attendanceRecord.setStatus(AttendanceStatus.ABSENT);
        attendanceRecord.setOverridden(false);
        attendanceRecord.setPassRunId(UUID.randomUUID());
        attendanceRecord.setCreatedAt(OffsetDateTime.parse("2026-04-27T09:00:00Z"));
        attendanceRecord.setUpdatedAt(OffsetDateTime.parse("2026-04-27T09:00:00Z"));
        return attendanceRecord;
    }
}
