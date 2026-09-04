package com.aoms.aomsbackend.attendance.service.impl;

import com.aoms.aomsbackend.attendance.entity.AttendanceRecord;
import com.aoms.aomsbackend.attendance.entity.AttendanceStatus;
import com.aoms.aomsbackend.attendance.entity.OooRequest;
import com.aoms.aomsbackend.attendance.entity.PublicHoliday;
import com.aoms.aomsbackend.attendance.entity.RemoteRequest;
import com.aoms.aomsbackend.attendance.repository.AttendanceRecordRepository;
import com.aoms.aomsbackend.attendance.repository.AttendanceStampLogRepository;
import com.aoms.aomsbackend.attendance.repository.OooRequestRepository;
import com.aoms.aomsbackend.attendance.repository.PublicHolidayRepository;
import com.aoms.aomsbackend.attendance.repository.RemoteRequestRepository;
import com.aoms.aomsbackend.auth.entity.User;
import com.aoms.aomsbackend.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AttendancePass2ServiceImplTest {

    @Mock private AttendanceRecordRepository attendanceRecordRepository;
    @Mock private OooRequestRepository oooRequestRepository;
    @Mock private RemoteRequestRepository remoteRequestRepository;
    @Mock private PublicHolidayRepository publicHolidayRepository;
    @Mock private UserRepository userRepository;
    @Mock private AttendanceStampLogRepository stampLogRepository;

    @InjectMocks
    private AttendancePass2ServiceImpl service;

    private static final UUID BUILDING_ID = UUID.randomUUID();
    private static final UUID OFFICE_ID   = UUID.randomUUID();
    private static final LocalDate DATE   = LocalDate.of(2026, 4, 10);

    @BeforeEach
    void stubDefaults() {
        when(oooRequestRepository
                .findByBuildingIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        any(), any(), any(), any()))
                .thenReturn(List.of());
        when(remoteRequestRepository
                .findByBuildingIdAndStatusAndRequestDate(any(), any(), any()))
                .thenReturn(List.of());
        when(publicHolidayRepository.findByBuildingIdAndHolidayDate(any(), any()))
                .thenReturn(Optional.empty());
        when(userRepository.findActiveUsersByOrganisation(any()))
                .thenReturn(List.of());
        when(stampLogRepository.findByJobNameAndLocationIdAndTargetDate(any(), any(), any()))
                .thenReturn(Optional.empty());
    }

    // ── Overlay: ON_LEAVE ──────────────────────────────────────────────────────

    @Test
    void overlay_withApprovedOoo_setsOnLeaveAndLinksLeaveRequestId() {
        UUID userId = UUID.randomUUID();
        UUID leaveId = UUID.randomUUID();

        AttendanceRecord record = recordWithNoSession(userId);
        OooRequest ooo = oooRequest(leaveId, userId, DATE.minusDays(1), DATE.plusDays(1));

        when(attendanceRecordRepository.findByBuildingIdAndRecordDate(BUILDING_ID, DATE))
                .thenReturn(List.of(record));
        when(oooRequestRepository
                .findByBuildingIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        eq(BUILDING_ID), eq("APPROVED"), eq(DATE), eq(DATE)))
                .thenReturn(List.of(ooo));

        service.overlay(BUILDING_ID, OFFICE_ID, DATE);

        verify(attendanceRecordRepository).save(argThat(saved ->
                AttendanceStatus.ON_LEAVE == saved.getStatus() &&
                leaveId.equals(saved.getLeaveRequestId())));
    }

    // ── Overlay: REMOTE ────────────────────────────────────────────────────────

    @Test
    void overlay_withApprovedRemote_setsRemoteAndLinksRemoteRequestId() {
        UUID userId = UUID.randomUUID();
        UUID remoteId = UUID.randomUUID();

        AttendanceRecord record = recordWithNoSession(userId);
        RemoteRequest remote = remoteRequest(remoteId, userId);

        when(attendanceRecordRepository.findByBuildingIdAndRecordDate(BUILDING_ID, DATE))
                .thenReturn(List.of(record));
        when(remoteRequestRepository
                .findByBuildingIdAndStatusAndRequestDate(BUILDING_ID, "APPROVED", DATE))
                .thenReturn(List.of(remote));

        service.overlay(BUILDING_ID, OFFICE_ID, DATE);

        verify(attendanceRecordRepository).save(argThat(saved ->
                AttendanceStatus.REMOTE == saved.getStatus() &&
                remoteId.equals(saved.getRemoteRequestId())));
    }

    // ── Overlay: PUBLIC_HOLIDAY ────────────────────────────────────────────────

    @Test
    void overlay_withPublicHoliday_setsPublicHolidayOnEligibleRecords() {
        UUID userId = UUID.randomUUID();
        AttendanceRecord record = recordWithNoSession(userId);

        when(attendanceRecordRepository.findByBuildingIdAndRecordDate(BUILDING_ID, DATE))
                .thenReturn(List.of(record));
        when(publicHolidayRepository.findByBuildingIdAndHolidayDate(BUILDING_ID, DATE))
                .thenReturn(Optional.of(new PublicHoliday()));

        service.overlay(BUILDING_ID, OFFICE_ID, DATE);

        verify(attendanceRecordRepository).save(argThat(saved ->
                AttendanceStatus.PUBLIC_HOLIDAY == saved.getStatus()));
    }

    // ── Priority: ON_LEAVE beats PUBLIC_HOLIDAY ────────────────────────────────

    @Test
    void overlay_withOooAndPublicHoliday_onLeaveWins() {
        UUID userId = UUID.randomUUID();
        UUID leaveId = UUID.randomUUID();

        AttendanceRecord record = recordWithNoSession(userId);
        OooRequest ooo = oooRequest(leaveId, userId, DATE, DATE);

        when(attendanceRecordRepository.findByBuildingIdAndRecordDate(BUILDING_ID, DATE))
                .thenReturn(List.of(record));
        when(oooRequestRepository
                .findByBuildingIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        eq(BUILDING_ID), eq("APPROVED"), eq(DATE), eq(DATE)))
                .thenReturn(List.of(ooo));
        when(publicHolidayRepository.findByBuildingIdAndHolidayDate(BUILDING_ID, DATE))
                .thenReturn(Optional.of(new PublicHoliday()));

        service.overlay(BUILDING_ID, OFFICE_ID, DATE);

        verify(attendanceRecordRepository).save(argThat(saved ->
                AttendanceStatus.ON_LEAVE == saved.getStatus()));
    }

    // ── Guard: badge-based record not touched ──────────────────────────────────

    @Test
    void overlay_withWorkSessionPresent_recordNotTouched() {
        UUID userId = UUID.randomUUID();
        AttendanceRecord record = recordWithNoSession(userId);
        record.setWorkSessionId(UUID.randomUUID());

        when(attendanceRecordRepository.findByBuildingIdAndRecordDate(BUILDING_ID, DATE))
                .thenReturn(List.of(record));
        when(oooRequestRepository
                .findByBuildingIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        any(), any(), any(), any()))
                .thenReturn(List.of(oooRequest(UUID.randomUUID(), userId, DATE, DATE)));

        service.overlay(BUILDING_ID, OFFICE_ID, DATE);

        verify(attendanceRecordRepository, never()).save(argThat(saved ->
                AttendanceStatus.ON_LEAVE == saved.getStatus()));
    }

    // ── Guard: overridden record not touched ───────────────────────────────────

    @Test
    void overlay_withIsOverriddenTrue_recordNotTouched() {
        UUID userId = UUID.randomUUID();
        AttendanceRecord record = recordWithNoSession(userId);
        record.setOverridden(true);

        when(attendanceRecordRepository.findByBuildingIdAndRecordDate(BUILDING_ID, DATE))
                .thenReturn(List.of(record));
        when(remoteRequestRepository
                .findByBuildingIdAndStatusAndRequestDate(any(), any(), any()))
                .thenReturn(List.of(remoteRequest(UUID.randomUUID(), userId)));

        service.overlay(BUILDING_ID, OFFICE_ID, DATE);

        verify(attendanceRecordRepository, never()).save(argThat(saved ->
                AttendanceStatus.REMOTE == saved.getStatus()));
    }

    // ── ABSENT sweep ───────────────────────────────────────────────────────────

    @Test
    void overlay_withNoRecord_createsAbsentWithOfficeId() {
        User user = activeUser(DATE.minusDays(30));

        when(attendanceRecordRepository.findByBuildingIdAndRecordDate(BUILDING_ID, DATE))
                .thenReturn(List.of());
        when(userRepository.findActiveUsersByOrganisation(BUILDING_ID))
                .thenReturn(List.of(user));
        when(attendanceRecordRepository.findByUserIdAndRecordDate(user.getId(), DATE))
                .thenReturn(Optional.empty());

        service.overlay(BUILDING_ID, OFFICE_ID, DATE);

        verify(attendanceRecordRepository).save(argThat(saved ->
                AttendanceStatus.ABSENT == saved.getStatus() &&
                OFFICE_ID.equals(saved.getOfficeId()) &&
                BUILDING_ID.equals(saved.getBuildingId())));
    }

    @Test
    void overlay_withPreEmploymentDate_absentNotCreated() {
        User user = activeUser(DATE.plusDays(1));

        when(attendanceRecordRepository.findByBuildingIdAndRecordDate(BUILDING_ID, DATE))
                .thenReturn(List.of());
        when(userRepository.findActiveUsersByOrganisation(BUILDING_ID))
                .thenReturn(List.of(user));

        service.overlay(BUILDING_ID, OFFICE_ID, DATE);

        verify(attendanceRecordRepository, never()).save(any(AttendanceRecord.class));
    }

    @Test
    void overlay_withExistingRecord_absentNotCreated() {
        UUID userId = UUID.randomUUID();
        AttendanceRecord existing = recordWithNoSession(userId);

        when(attendanceRecordRepository.findByBuildingIdAndRecordDate(BUILDING_ID, DATE))
                .thenReturn(List.of(existing));
        when(userRepository.findActiveUsersByOrganisation(BUILDING_ID))
                .thenReturn(List.of(activeUser(userId, DATE.minusDays(30))));

        service.overlay(BUILDING_ID, OFFICE_ID, DATE);

        verify(attendanceRecordRepository, never()).save(argThat(saved ->
                AttendanceStatus.ABSENT == saved.getStatus()));
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private AttendanceRecord recordWithNoSession(UUID userId) {
        AttendanceRecord r = new AttendanceRecord();
        r.setUserId(userId);
        r.setBuildingId(BUILDING_ID);
        r.setOfficeId(OFFICE_ID);
        r.setRecordDate(DATE);
        r.setStatus(AttendanceStatus.ABSENT);
        r.setOverridden(false);
        r.setPassRunId(UUID.randomUUID());
        return r;
    }

    private OooRequest oooRequest(UUID id, UUID userId, LocalDate start, LocalDate end) {
        return OooRequest.builder()
                .id(id)
                .employeeId(userId)
                .buildingId(BUILDING_ID)
                .oooType("ANNUAL_LEAVE")
                .startDate(start)
                .endDate(end)
                .status("APPROVED")
                .build();
    }

    private RemoteRequest remoteRequest(UUID id, UUID userId) {
        return RemoteRequest.builder()
                .id(id)
                .employeeId(userId)
                .buildingId(BUILDING_ID)
                .requestDate(DATE)
                .requestType("ONE_OFF")
                .status("APPROVED")
                .build();
    }

    private User activeUser(LocalDate employmentStart) {
        return activeUser(UUID.randomUUID(), employmentStart);
    }

    private User activeUser(UUID id, LocalDate employmentStart) {
        return User.builder()
                .id(id)
                .employmentStartDate(employmentStart)
                .build();
    }
}
