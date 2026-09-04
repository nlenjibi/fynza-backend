package com.aoms.aomsbackend.attendance.service.impl;

import com.aoms.aomsbackend.attendance.dto.CalendarDayResponse;
import com.aoms.aomsbackend.attendance.dto.CalendarRecordEntry;
import com.aoms.aomsbackend.attendance.dto.TeamAttendanceRecordResponse;
import com.aoms.aomsbackend.attendance.entity.AttendanceRecord;
import com.aoms.aomsbackend.attendance.entity.AttendanceStatus;
import com.aoms.aomsbackend.attendance.entity.Employee;
import com.aoms.aomsbackend.attendance.entity.WorkSession;
import com.aoms.aomsbackend.attendance.exception.EmployeeNotFoundException;
import com.aoms.aomsbackend.attendance.exception.ExportWindowTooLargeException;
import com.aoms.aomsbackend.attendance.exception.NotADirectReportException;
import com.aoms.aomsbackend.attendance.repository.AttendanceRecordRepository;
import com.aoms.aomsbackend.attendance.repository.EmployeeRepository;
import com.aoms.aomsbackend.attendance.service.TeamAttendanceService;
import com.aoms.aomsbackend.auth.repository.UserRepository;
import com.aoms.aomsbackend.common.responses.PaginatedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of {@link TeamAttendanceService} that enforces direct-report
 * scoping and location boundaries when querying attendance data.
 *
 * <p>All queries are scoped to the manager's direct reports at the same
 * primary building. The authenticated user is mapped to an {@code Employee}
 * record via {@code User.ssoUserId → Employee.ssoUserId}.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamAttendanceServiceImpl implements TeamAttendanceService {
    private static final long MAX_EXPORT_DAYS = 90;
    private static final String CSV_HEADER = "Employee Name,Employee ID,Date,Status,First Badge In,Last Badge Out,Duration (mins),Minutes Late,Overridden";
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;

    /** {@inheritDoc} */
    @Override
    public PaginatedResponse<TeamAttendanceRecordResponse> getTeamAttendance(
            UUID userId,
            UUID employeeIdFilter,
            LocalDate fromDate,
            LocalDate toDate,
            List<AttendanceStatus> statuses,
            Pageable pageable
    ) {

        Employee manager = resolveManagerEmployee(userId);
        Map<UUID, Employee> directReports = getDirectReportMap(manager);

        validateDirectReportFilter(employeeIdFilter, directReports.keySet());

        List<UUID> targetIds = employeeIdFilter != null
                ? List.of(employeeIdFilter)
                : new ArrayList<>(directReports.keySet());

        List<AttendanceStatus> statusFilter = (statuses == null || statuses.isEmpty()) ? null : statuses;

        Page<AttendanceRecord> page = attendanceRecordRepository.findTeamAttendance(
                targetIds, fromDate, toDate, statusFilter, pageable
        );

        Page<TeamAttendanceRecordResponse> dtoPage = page.map(record -> toResponse(record, directReports));

        return PaginatedResponse.from(dtoPage);
    }

    /** {@inheritDoc} */
    @Override
    public CalendarDayResponse getTeamCalendar(UUID userId, LocalDate date) {
        Employee manager = resolveManagerEmployee(userId);
        Map<UUID, Employee> directReports = getDirectReportMap(manager);

        List<AttendanceRecord> records = attendanceRecordRepository.findByUserIdsAndDate(new ArrayList<>(directReports.keySet()), date);

        Map<UUID, AttendanceRecord> recordsByUserId = records.stream().collect(Collectors.toMap(AttendanceRecord::getUserId, Function.identity(), (a, b) -> a));

        List<CalendarRecordEntry> entries = directReports.values().stream()
                .map(emp -> {
                    AttendanceRecord record = recordsByUserId.get(emp.getId());
                    return CalendarRecordEntry.builder()
                            .employeeId(emp.getId())
                            .employeeName(emp.getDisplayName())
                            .status(record != null ? record.getStatus() : null)
                            .isOverridden(record != null ? record.isOverridden() : null)
                            .build();
                })
                .toList();

        return CalendarDayResponse.builder()
                .date(date)
                .records(entries)
                .build();
    }

    /** {@inheritDoc} */
    @Override
    public void streamTeamAttendanceCsv(
            UUID userId,
            LocalDate fromDate,
            LocalDate toDate,
            UUID employeeIdFilter,
            List<AttendanceStatus> statuses,
            OutputStream outputStream
    ) {
        if (ChronoUnit.DAYS.between(fromDate, toDate) > MAX_EXPORT_DAYS) {
            throw new ExportWindowTooLargeException();
        }

        Employee manager = resolveManagerEmployee(userId);
        Map<UUID, Employee> directReports = getDirectReportMap(manager);

        validateDirectReportFilter(employeeIdFilter, directReports.keySet());

        List<UUID> targetIds = employeeIdFilter != null
                ? List.of(employeeIdFilter)
                : new ArrayList<>(directReports.keySet());

        List<AttendanceStatus> statusFilter = (statuses == null || statuses.isEmpty()) ? null : statuses;

        try (Stream<AttendanceRecord> records = attendanceRecordRepository.streamTeamAttendanceList(
                     targetIds, fromDate, toDate, statusFilter);
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
            writer.println(CSV_HEADER);
            records.forEach(record -> {
                Employee emp = directReports.get(record.getUserId());
                WorkSession ws = record.getWorkSession();
                writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                        emp != null ? emp.getDisplayName() : "",
                        record.getUserId(),
                        record.getRecordDate(),
                        record.getStatus(),
                        ws != null && ws.getFirstBadgeIn() != null ? ws.getFirstBadgeIn() : "",
                        ws != null && ws.getLastBadgeOut() != null ? ws.getLastBadgeOut() : "",
                        ws != null && ws.getTotalDurationMinutes() != null ? ws.getTotalDurationMinutes() : "",
                        ws != null && ws.getMinutesLate() != null ? ws.getMinutesLate() : "",
                        record.isOverridden());
            });
            writer.flush();
        }
    }

    /**
     * Maps an authenticated user ID to the corresponding {@link Employee} record
     * via the SSO user ID link ({@code User.ssoUserId → Employee.ssoUserId}).
     *
     * @param userId the authenticated user's UUID from the session
     * @return the matching Employee record
     * @throws EmployeeNotFoundException if the user or linked employee cannot be found
     */
    private Employee resolveManagerEmployee(UUID userId) {
        var user = userRepository.findById(userId).orElseThrow(() -> new EmployeeNotFoundException("User not found."));

        String ssoUserId = user.getSsoUserId();
        if (ssoUserId == null) {
            throw new EmployeeNotFoundException("User has no linked employee record.");
        }

        return employeeRepository.findBySsoUserId(ssoUserId).orElseThrow(() -> new EmployeeNotFoundException("Employee record not found for user."));
    }

    /**
     * Loads all active direct reports for the given manager at the same primary building
     * and returns them as a map keyed by employee ID for O(1) lookups.
     */
    private Map<UUID, Employee> getDirectReportMap(Employee manager) {
        List<Employee> directReports = employeeRepository.findByManagerIdAndPrimaryBuildingIdAndActiveTrueAndDeletedAtIsNull(manager.getId(), manager.getPrimaryBuildingId());

        return directReports.stream().collect(Collectors.toMap(Employee::getId, Function.identity()));
    }

    /**
     * Validates that the optional employee ID filter refers to an actual direct report.
     *
     * @throws NotADirectReportException if the employee is not in the direct report set
     */
    private void validateDirectReportFilter(UUID employeeIdFilter, Set<UUID> directReportIds) {
        if (employeeIdFilter != null && !directReportIds.contains(employeeIdFilter)) {
            throw new NotADirectReportException();
        }
    }

    /**
     * Converts an {@link AttendanceRecord} entity into a {@link TeamAttendanceRecordResponse} DTO,
     * enriching it with employee name and work session details.
     */
    private TeamAttendanceRecordResponse toResponse(AttendanceRecord record, Map<UUID, Employee> employeeMap) {
        Employee emp = employeeMap.get(record.getUserId());
        WorkSession ws = record.getWorkSession();

        return TeamAttendanceRecordResponse.builder()
                .employeeId(record.getUserId())
                .employeeName(emp != null ? emp.getDisplayName() : null)
                .recordDate(record.getRecordDate())
                .status(record.getStatus())
                .firstBadgeIn(ws != null ? ws.getFirstBadgeIn() : null)
                .lastBadgeOut(ws != null ? ws.getLastBadgeOut() : null)
                .totalDurationMinutes(ws != null ? ws.getTotalDurationMinutes() : null)
                .isLate(ws != null ? ws.getIsLate() : null)
                .minutesLate(ws != null ? ws.getMinutesLate() : null)
                .isOverridden(record.isOverridden())
                .build();
    }
}
