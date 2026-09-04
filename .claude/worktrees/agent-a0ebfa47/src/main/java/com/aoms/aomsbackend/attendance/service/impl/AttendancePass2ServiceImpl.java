package com.aoms.aomsbackend.attendance.service.impl;

import com.aoms.aomsbackend.attendance.entity.AttendanceRecord;
import com.aoms.aomsbackend.attendance.entity.AttendanceStampLog;
import com.aoms.aomsbackend.attendance.entity.AttendanceStatus;
import com.aoms.aomsbackend.attendance.entity.OooRequest;
import com.aoms.aomsbackend.attendance.entity.RemoteRequest;
import com.aoms.aomsbackend.attendance.repository.AttendanceRecordRepository;
import com.aoms.aomsbackend.attendance.repository.AttendanceStampLogRepository;
import com.aoms.aomsbackend.attendance.repository.OooRequestRepository;
import com.aoms.aomsbackend.attendance.repository.PublicHolidayRepository;
import com.aoms.aomsbackend.attendance.repository.RemoteRequestRepository;
import com.aoms.aomsbackend.attendance.service.AttendancePass2Service;
import com.aoms.aomsbackend.auth.entity.User;
import com.aoms.aomsbackend.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendancePass2ServiceImpl implements AttendancePass2Service {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final OooRequestRepository oooRequestRepository;
    private final RemoteRequestRepository remoteRequestRepository;
    private final PublicHolidayRepository publicHolidayRepository;
    private final UserRepository userRepository;
    private final AttendanceStampLogRepository stampLogRepository;

    @Override
    @Transactional
    public void overlay(UUID buildingId, UUID officeId, LocalDate date) {
        log.info("Starting Pass 2 overlay for building {} on {}", buildingId, date);

        UUID runId = UUID.randomUUID();
        int onLeaveCount = 0;
        int remoteCount = 0;
        int holidayCount = 0;
        int absentCount;
        int skipped = 0;
        int failed = 0;

        try {
            // ── Bulk fetch (3 queries) ────────────────────────────────────────
            Map<UUID, UUID> leaveRequestByUser = oooRequestRepository
                    .findByBuildingIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                            buildingId, "APPROVED", date, date)
                    .stream()
                    .collect(Collectors.toMap(OooRequest::getEmployeeId, OooRequest::getId,
                            (first, second) -> first));

            Map<UUID, UUID> remoteRequestByUser = remoteRequestRepository
                    .findByBuildingIdAndStatusAndRequestDate(buildingId, "APPROVED", date)
                    .stream()
                    .collect(Collectors.toMap(RemoteRequest::getEmployeeId, RemoteRequest::getId,
                            (first, second) -> first));

            boolean isPublicHoliday = publicHolidayRepository
                    .findByBuildingIdAndHolidayDate(buildingId, date)
                    .isPresent();

            // ── Apply overlays ────────────────────────────────────────────────
            List<AttendanceRecord> existingRecords =
                    attendanceRecordRepository.findByBuildingIdAndRecordDate(buildingId, date);

            for (AttendanceRecord attendanceRecord : existingRecords) {
                if (attendanceRecord.getWorkSessionId() != null || attendanceRecord.isOverridden()) {
                    skipped++;
                    continue;
                }
                int result = applyOverlayToRecord(attendanceRecord,
                        leaveRequestByUser, remoteRequestByUser, isPublicHoliday);
                if (result == 0) {
                    onLeaveCount++;
                } else if (result == 1) {
                    holidayCount++;
                } else if (result == 2) {
                    remoteCount++;
                } else if (result == -1) {
                    failed++;
                }
                // result == -2 means skipped/skipped or no change
            }

            // ── ABSENT sweep ──────────────────────────────────────────────────
            absentCount = performAbsentSweep(buildingId, officeId, date, existingRecords);

            int totalProcessed = onLeaveCount + remoteCount + holidayCount + absentCount;
            logJobExecution(buildingId, date, "SUCCESS",
                    totalProcessed, skipped, failed, runId);
            log.info("Pass 2 done — onLeave={}, remote={}, holiday={}, absent={}, skipped={}, failed={}",
                    onLeaveCount, remoteCount, holidayCount, absentCount, skipped, failed);

        } catch (Exception e) {
            log.error("Pass 2 failed for building {} on {}: {}", buildingId, date, e.getMessage());
            logJobExecution(buildingId, date, "FAILED", 0, skipped, failed, runId);
            throw e;
        }
    }

    private int applyOverlayToRecord(AttendanceRecord attendanceRecord,
                                     Map<UUID, UUID> leaveRequestByUser,
                                     Map<UUID, UUID> remoteRequestByUser,
                                     boolean isPublicHoliday) {
        try {
            UUID userId = attendanceRecord.getUserId();
            if (leaveRequestByUser.containsKey(userId)) {
                attendanceRecord.setStatus(AttendanceStatus.ON_LEAVE);
                attendanceRecord.setLeaveRequestId(leaveRequestByUser.get(userId));
                attendanceRecordRepository.save(attendanceRecord);
                return 0; // ON_LEAVE
            } else if (isPublicHoliday) {
                attendanceRecord.setStatus(AttendanceStatus.PUBLIC_HOLIDAY);
                attendanceRecordRepository.save(attendanceRecord);
                return 1; // PUBLIC_HOLIDAY
            } else if (remoteRequestByUser.containsKey(userId)) {
                attendanceRecord.setStatus(AttendanceStatus.REMOTE);
                attendanceRecord.setRemoteRequestId(remoteRequestByUser.get(userId));
                attendanceRecordRepository.save(attendanceRecord);
                return 2; // REMOTE
            }
            return -2; // no change
        } catch (Exception e) {
            log.error("Error applying overlay to record {}: {}", attendanceRecord.getId(), e.getMessage());
            return -1; // FAILED
        }
    }

    private int performAbsentSweep(UUID buildingId, UUID officeId, LocalDate date,
                                   List<AttendanceRecord> existingRecords) {
        Set<UUID> accountedFor = existingRecords.stream()
                .map(AttendanceRecord::getUserId)
                .collect(Collectors.toSet());

        List<User> activeUsers = userRepository.findActiveUsersByOrganisation(buildingId);

        int absentCount = 0;
        for (User user : activeUsers) {
            if (accountedFor.contains(user.getId())) continue;
            boolean shouldSkip = user.getEmploymentStartDate() != null
                    && user.getEmploymentStartDate().isAfter(date);
            if (!shouldSkip) {
                Optional<AttendanceRecord> existing =
                        attendanceRecordRepository.findByUserIdAndRecordDate(user.getId(), date);
                if (existing.isEmpty()) {
                    AttendanceRecord absent = new AttendanceRecord();
                    absent.setUserId(user.getId());
                    absent.setOfficeId(officeId);
                    absent.setBuildingId(buildingId);
                    absent.setRecordDate(date);
                    absent.setStatus(AttendanceStatus.ABSENT);
                    absent.setOverridden(false);
                    absent.setPassRunId(UUID.randomUUID());
                    attendanceRecordRepository.save(absent);
                    absentCount++;
                }
            }
        }

        if (absentCount > 0) {
            log.info("Created {} ABSENT records", absentCount);
        }
        return absentCount;
    }

    private void logJobExecution(UUID locationId, LocalDate targetDate,
                                 String status, int processed, int skipped, int failed, UUID runId) {
        Optional<AttendanceStampLog> existing =
                stampLogRepository.findByJobNameAndLocationIdAndTargetDate("attendance_pass2", locationId, targetDate);

        AttendanceStampLog entry = existing.orElseGet(AttendanceStampLog::new);
        entry.setJobName("attendance_pass2");
        entry.setLocationId(locationId);
        entry.setTargetDate(targetDate);
        entry.setStatus(status);
        entry.setRecordsProcessed(processed);
        entry.setRecordsSkipped(skipped);
        entry.setRecordsFailed(failed);
        entry.setRunId(runId);

        if (existing.isEmpty()) {
            entry.setStartedAt(LocalDateTime.now());
        }
        entry.setCompletedAt(LocalDateTime.now());
        stampLogRepository.save(entry);
    }
}