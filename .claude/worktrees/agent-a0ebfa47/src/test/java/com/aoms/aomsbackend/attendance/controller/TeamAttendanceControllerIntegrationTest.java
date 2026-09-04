package com.aoms.aomsbackend.attendance.controller;

import com.aoms.aomsbackend.attendance.dto.CalendarDayResponse;
import com.aoms.aomsbackend.attendance.dto.CalendarRecordEntry;
import com.aoms.aomsbackend.attendance.dto.TeamAttendanceRecordResponse;
import com.aoms.aomsbackend.attendance.entity.AttendanceStatus;
import com.aoms.aomsbackend.attendance.exception.NotADirectReportException;
import com.aoms.aomsbackend.attendance.service.TeamAttendanceService;
import com.aoms.aomsbackend.auth.constant.SessionAttribute;
import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.auth.service.UserRoleAccessService;
import com.aoms.aomsbackend.common.exception.GlobalExceptionHandler;
import com.aoms.aomsbackend.common.responses.PaginatedResponse;
import com.aoms.aomsbackend.config.interceptor.LocationRoleInterceptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TeamAttendanceControllerIntegrationTest {
    @Mock
    private TeamAttendanceService teamAttendanceService;

    @Mock
    private UserRoleAccessService userRoleAccessService;

    private MockMvc mockMvc;
    private UUID userId;
    private UUID orgId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orgId = UUID.randomUUID();

        TeamAttendanceController controller = new TeamAttendanceController(teamAttendanceService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(new LocationRoleInterceptor(userRoleAccessService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getTeamAttendance_withoutSession_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/attendance/team")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void getTeamAttendance_withManagerRole_returns200() throws Exception {
        when(userRoleAccessService.hasAccess(eq(userId), eq(orgId), eq(UserRoleType.MANAGER)))
                .thenReturn(true);

        var response = PaginatedResponse.from(new PageImpl<>(List.of(
                TeamAttendanceRecordResponse.builder()
                        .employeeId(UUID.randomUUID())
                        .employeeName("Alice Smith")
                        .recordDate(LocalDate.of(2026, 3, 14))
                        .status(AttendanceStatus.PRESENT)
                        .totalDurationMinutes(480)
                        .isLate(false)
                        .minutesLate(0)
                        .isOverridden(false)
                        .build()
        )));

        when(teamAttendanceService.getTeamAttendance(eq(userId), isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(response);

        mockMvc.perform(authenticatedGet("/api/v1/attendance/team"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].employeeName").value("Alice Smith"))
                .andExpect(jsonPath("$.data.content[0].status").value("PRESENT"));
    }

    @Test
    void getTeamCalendar_returns200WithRecords() throws Exception {
        when(userRoleAccessService.hasAccess(eq(userId), eq(orgId), eq(UserRoleType.MANAGER)))
                .thenReturn(true);

        var calendarResponse = CalendarDayResponse.builder()
                .date(LocalDate.of(2026, 3, 14))
                .records(List.of(
                        CalendarRecordEntry.builder()
                                .employeeId(UUID.randomUUID())
                                .employeeName("Alice Smith")
                                .status(AttendanceStatus.PRESENT)
                                .isOverridden(false)
                                .build(),
                        CalendarRecordEntry.builder()
                                .employeeId(UUID.randomUUID())
                                .employeeName("Bob Jones")
                                .status(null)
                                .isOverridden(null)
                                .build()
                ))
                .build();

        when(teamAttendanceService.getTeamCalendar(eq(userId), eq(LocalDate.of(2026, 3, 14))))
                .thenReturn(calendarResponse);

        mockMvc.perform(authenticatedGet("/api/v1/attendance/team/calendar")
                        .param("date", "2026-03-14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.date").value("2026-03-14"))
                .andExpect(jsonPath("$.data.records", hasSize(2)))
                .andExpect(jsonPath("$.data.records[1].status").doesNotExist());
    }

    @Test
    void getExport_returns200WithCsvContentType() throws Exception {
        when(userRoleAccessService.hasAccess(eq(userId), eq(orgId), eq(UserRoleType.MANAGER)))
                .thenReturn(true);

        mockMvc.perform(authenticatedGet("/api/v1/attendance/team/export")
                        .param("fromDate", "2026-03-01")
                        .param("toDate", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv; charset=utf-8"))
                .andExpect(header().string("Content-Disposition",
                        containsString("team-attendance-2026-03-01-2026-03-31.csv")));
    }

    @Test
    void getExport_windowTooLarge_returns400WithCode() throws Exception {
        when(userRoleAccessService.hasAccess(eq(userId), eq(orgId), eq(UserRoleType.MANAGER)))
                .thenReturn(true);

        mockMvc.perform(authenticatedGet("/api/v1/attendance/team/export")
                        .param("fromDate", "2026-01-01")
                        .param("toDate", "2026-05-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code.md").value("EXPORT_WINDOW_TOO_LARGE"));
    }

    @Test
    void getTeamAttendance_nonDirectReportFilter_returns403() throws Exception {
        UUID nonReportId = UUID.randomUUID();
        when(userRoleAccessService.hasAccess(eq(userId), eq(orgId), eq(UserRoleType.MANAGER)))
                .thenReturn(true);

        when(teamAttendanceService.getTeamAttendance(eq(userId), eq(nonReportId), isNull(), isNull(), isNull(), any()))
                .thenThrow(new NotADirectReportException());

        mockMvc.perform(authenticatedGet("/api/v1/attendance/team")
                        .param("employeeId", nonReportId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Employee is not a direct report."));
    }

    private MockHttpServletRequestBuilder authenticatedGet(String url) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of()));
        return get(url)
                .sessionAttr(SessionAttribute.USER_ID.getKey(), userId.toString())
                .header("X-Organization-Id", orgId.toString())
                .contentType(MediaType.APPLICATION_JSON);
    }
}
