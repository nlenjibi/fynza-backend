package com.aoms.aomsbackend.attendance.service.impl;

import com.aoms.aomsbackend.attendance.dto.AttendanceDetailDto;
import com.aoms.aomsbackend.attendance.dto.AttendanceExportDto;
import com.aoms.aomsbackend.attendance.dto.AttendanceSummaryDto;
import com.aoms.aomsbackend.attendance.dto.AttendanceSummaryResponse;
import com.aoms.aomsbackend.attendance.dto.HrAttendanceRecordResponse;
import com.aoms.aomsbackend.attendance.entity.AttendanceStatus;
import com.aoms.aomsbackend.attendance.repository.EmployeeRepository;
import com.aoms.aomsbackend.attendance.repository.HrAttendanceRepository;
import com.aoms.aomsbackend.attendance.service.HrAttendanceService;
import com.aoms.aomsbackend.common.responses.PaginatedResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HrAttendanceServiceImpl implements HrAttendanceService {

    private static final Logger log = LoggerFactory.getLogger(HrAttendanceServiceImpl.class);

    private static final String CSV_HEADER =
            "Employee Name,Employee Code,Department,Team,Rank,Date,Status,First Badge In (Local),Last Badge Out (Local),Duration (mins),Minutes Late,Late,Overridden";

    private final EmployeeRepository employeeRepository;
    private final HrAttendanceRepository hrAttendanceRepository;

    @Override
    public PaginatedResponse<HrAttendanceRecordResponse> getLocationAttendance(
            UUID locationId,
            String department,
            UUID managerId,
            UUID employeeId,
            LocalDate fromDate,
            LocalDate toDate,
            List<AttendanceStatus> statuses,
            int page,
            int size,
            Sort.Direction order
    ) {
        List<UUID> userIds = resolveUserIds(locationId, department, managerId, employeeId);
        if (userIds.isEmpty()) {
            return PaginatedResponse.from(Page.empty(PageRequest.of(page, size)));
        }

        List<String> statusFilter = toStatusStrings(statuses);
        Pageable pageable = PageRequest.of(page, size, Sort.by(order, "record_date"));

        Page<AttendanceDetailDto> records = hrAttendanceRepository
                .findByLocationAndUsers(locationId, userIds, fromDate, toDate, statusFilter, pageable);

        return PaginatedResponse.from(records.map(this::toResponse));
    }

    @Override
    public AttendanceSummaryResponse getSummary(UUID locationId, LocalDate date) {
        AttendanceSummaryDto dto = hrAttendanceRepository.getSummary(locationId, date);
        if (dto == null) {
            return AttendanceSummaryResponse.builder().date(date).build();
        }
        return AttendanceSummaryResponse.builder()
                .date(date)
                .totalPresent(dto.getTotalPresent())
                .totalLate(dto.getTotalLate())
                .totalAbsent(dto.getTotalAbsent())
                .totalRemote(dto.getTotalRemote())
                .totalOnLeave(dto.getTotalOnLeave())
                .totalPublicHoliday(dto.getTotalPublicHoliday())
                .totalInsufficientHours(dto.getTotalInsufficientHours())
                .build();
    }

    @Override
    public void streamHrAttendanceCsv(
            UUID locationId,
            String department,
            UUID managerId,
            UUID employeeId,
            LocalDate fromDate,
            LocalDate toDate,
            List<AttendanceStatus> statuses,
            OutputStream outputStream
    ) {
        List<UUID> userIds = resolveUserIds(locationId, department, managerId, employeeId);
        List<String> statusFilter = toStatusStrings(statuses);

        try (Stream<AttendanceExportDto> records = userIds.isEmpty()
                ? Stream.empty()
                : hrAttendanceRepository.streamByLocationAndUsers(locationId, userIds, fromDate, toDate, statusFilter);
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
            writer.println(CSV_HEADER);
            records.forEach(row -> writeCsvRow(writer, row));
            writer.flush();
        }
    }

    private void writeCsvRow(PrintWriter writer, AttendanceExportDto row) {
        writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                nullSafe(row.getEmployeeName()),
                nullSafe(row.getEmployeeCode()),
                nullSafe(row.getDepartment()),
                nullSafe(row.getTeam()),
                nullSafe(row.getRank()),
                row.getRecordDate(),
                row.getStatus(),
                row.getFirstBadgeInLocal() != null ? row.getFirstBadgeInLocal() : "",
                row.getLastBadgeOutLocal() != null ? row.getLastBadgeOutLocal() : "",
                nullSafe(row.getTotalDurationMinutes()),
                nullSafe(row.getMinutesLate()),
                Boolean.TRUE.equals(row.getIsLate()),
                Boolean.TRUE.equals(row.getIsOverridden()));
    }

    private List<UUID> resolveUserIds(UUID locationId, String department, UUID managerId, UUID employeeId) {
        String normalizedDepartment = (department == null || department.isBlank()) ? null : department;

        if (employeeId != null) {
            return List.of(employeeId);
        }

        if (managerId == null) {
            return employeeRepository.findActiveEmployeeIdsByPrimaryBuildingIdAndDepartment(locationId, normalizedDepartment);
        }

        return employeeRepository.findLocationSubtreeEmployeeIds(locationId, normalizedDepartment, managerId)
                .stream()
                .map(UUID::fromString)
                .toList();
    }

    private HrAttendanceRecordResponse toResponse(AttendanceDetailDto dto) {
        if (dto == null) {
            log.warn("Null DTO encountered while mapping attendance detail");
            return null;
        }
        return HrAttendanceRecordResponse.builder()
                .employeeId(dto.getEmployeeId())
                .employeeName(dto.getEmployeeFullName())
                .department(dto.getDepartment())
                .recordDate(dto.getRecordDate())
                .status(AttendanceStatus.valueOf(dto.getStatus()))
                .firstBadgeIn(dto.getFirstBadgeIn() != null ? dto.getFirstBadgeIn().toInstant() : null)
                .lastBadgeOut(dto.getLastBadgeOut() != null ? dto.getLastBadgeOut().toInstant() : null)
                .totalDurationMinutes(dto.getTotalDurationMinutes())
                .isLate(dto.getIsLate())
                .minutesLate(dto.getMinutesLate())
                .isOverridden(Boolean.TRUE.equals(dto.getIsOverridden()))
                .build();
    }

    private List<String> toStatusStrings(List<AttendanceStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return Stream.of(AttendanceStatus.values()).map(Enum::name).toList();
        }
        return statuses.stream().map(Enum::name).toList();
    }

    private String nullSafe(Object value) {
        return value != null ? value.toString() : "";
    }
}
