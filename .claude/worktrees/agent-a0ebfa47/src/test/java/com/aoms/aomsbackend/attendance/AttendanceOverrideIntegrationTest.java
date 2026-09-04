package com.aoms.aomsbackend.attendance;

import com.aoms.aomsbackend.attendance.dto.AttendanceRecordOverrideRequest;
import com.aoms.aomsbackend.attendance.dto.AttendanceRecordRevertRequest;
import com.aoms.aomsbackend.attendance.entity.AttendanceRecord;
import com.aoms.aomsbackend.attendance.entity.AttendanceStatus;
import com.aoms.aomsbackend.attendance.repository.AttendanceRecordRepository;
import com.aoms.aomsbackend.auth.constant.SessionAttribute;
import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.auth.service.UserRoleAccessService;
import com.aoms.aomsbackend.config.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class AttendanceOverrideIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private AttendanceRecordRepository repository;

    @MockitoBean
    private UserRoleAccessService userRoleAccessService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final UUID superAdminId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @AfterEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    void override_setsAllFieldsCorrectly() throws Exception {
        AttendanceRecord attendanceRecord = seedRecord(AttendanceStatus.ABSENT, false);

        when(userRoleAccessService.hasAccess(eq(superAdminId), any(), eq(UserRoleType.SUPER_ADMIN))).thenReturn(true);

        AttendanceRecordOverrideRequest request = new AttendanceRecordOverrideRequest();
        request.setStatus(AttendanceStatus.PRESENT);
        request.setOverrideReason("Correcting pipeline error");

        mockMvc.perform(patch("/api/v1/attendance-records/{id}/override", attendanceRecord.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), superAdminId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PRESENT"))
                .andExpect(jsonPath("$.data.isOverridden").value(true))
                .andExpect(jsonPath("$.data.originalStatus").value("ABSENT"))
                .andExpect(jsonPath("$.data.overrideReason").value("Correcting pipeline error"));

        AttendanceRecord updated = repository.findById(attendanceRecord.getId()).orElseThrow();
        assertThat(updated.isOverridden()).isTrue();
        assertThat(updated.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(updated.getOriginalStatus()).isEqualTo("ABSENT");
        assertThat(updated.getOverriddenBy()).isEqualTo(superAdminId);
        assertThat(updated.getOverriddenAt()).isNotNull();
    }

    @Test
    void override_alreadyOverridden_returns409() throws Exception {
        AttendanceRecord attendanceRecord = seedRecord(AttendanceStatus.PRESENT, true);

        when(userRoleAccessService.hasAccess(eq(superAdminId), any(), eq(UserRoleType.SUPER_ADMIN))).thenReturn(true);

        AttendanceRecordOverrideRequest request = new AttendanceRecordOverrideRequest();
        request.setStatus(AttendanceStatus.LATE);
        request.setOverrideReason("reason");

        mockMvc.perform(patch("/api/v1/attendance-records/{id}/override", attendanceRecord.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), superAdminId.toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code.md").value("ALREADY_OVERRIDDEN"));
    }

    @Test
    void revert_restoresOriginalStatusAndKeepsAuditHistory() throws Exception {
        AttendanceRecord attendanceRecord = seedRecord(AttendanceStatus.PRESENT, true);
        attendanceRecord.setOriginalStatus("ABSENT");
        attendanceRecord.setOverriddenAt(OffsetDateTime.parse("2026-04-28T10:15:30Z"));
        repository.save(attendanceRecord);

        when(userRoleAccessService.hasAccess(eq(superAdminId), any(), eq(UserRoleType.SUPER_ADMIN))).thenReturn(true);

        AttendanceRecordRevertRequest request = new AttendanceRecordRevertRequest();
        request.setRevertReason("Override was a mistake");

        mockMvc.perform(patch("/api/v1/attendance-records/{id}/revert", attendanceRecord.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), superAdminId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ABSENT"))
                .andExpect(jsonPath("$.data.isOverridden").value(false))
                .andExpect(jsonPath("$.data.originalStatus").value("ABSENT"))
                .andExpect(jsonPath("$.data.overrideReason").value("original override reason"));

        AttendanceRecord reverted = repository.findById(attendanceRecord.getId()).orElseThrow();
        assertThat(reverted.isOverridden()).isFalse();
        assertThat(reverted.getStatus()).isEqualTo(AttendanceStatus.ABSENT);
        assertThat(reverted.getOriginalStatus()).isEqualTo("ABSENT");
        assertThat(reverted.getOverriddenAt()).isEqualTo(OffsetDateTime.parse("2026-04-28T10:15:30Z"));
        assertThat(reverted.getOverriddenBy()).isEqualTo(superAdminId);
        assertThat(reverted.getOverrideReason()).isEqualTo("original override reason");
        assertThat(reverted.getRevertReasons()).contains("Override was a mistake");
        assertThat(reverted.getRevertReasons()).contains(superAdminId.toString());
    }


    @Test
    void revert_notOverridden_returns409() throws Exception {
        AttendanceRecord attendanceRecord = seedRecord(AttendanceStatus.LATE, false);

        when(userRoleAccessService.hasAccess(eq(superAdminId), any(), eq(UserRoleType.SUPER_ADMIN))).thenReturn(true);

        AttendanceRecordRevertRequest request = new AttendanceRecordRevertRequest();
        request.setRevertReason("reason");

        mockMvc.perform(patch("/api/v1/attendance-records/{id}/revert", attendanceRecord.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), superAdminId.toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code.md").value("NOT_OVERRIDDEN"));
    }

    private AttendanceRecord seedRecord(AttendanceStatus status, boolean overridden) {
        AttendanceRecord attendanceRecord = new AttendanceRecord();
        attendanceRecord.setUserId(UUID.randomUUID());
        attendanceRecord.setOfficeId(UUID.randomUUID());
        attendanceRecord.setBuildingId(UUID.randomUUID());
        attendanceRecord.setRecordDate(LocalDate.of(2025, 1, 10));
        attendanceRecord.setStatus(status);
        attendanceRecord.setOverridden(overridden);
        attendanceRecord.setPassRunId(UUID.randomUUID());
        if (overridden) {
            attendanceRecord.setOverrideReason("original override reason");
            attendanceRecord.setOverriddenBy(superAdminId);
            attendanceRecord.setOverriddenAt(OffsetDateTime.parse("2026-04-28T10:00:00Z"));
            attendanceRecord.setOriginalStatus("ABSENT");
        }
        return repository.save(attendanceRecord);
    }
}
