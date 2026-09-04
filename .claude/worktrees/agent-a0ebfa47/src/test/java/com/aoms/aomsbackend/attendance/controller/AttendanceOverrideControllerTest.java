package com.aoms.aomsbackend.attendance.controller;

import com.aoms.aomsbackend.attendance.dto.AttendanceRecordOverrideRequest;
import com.aoms.aomsbackend.attendance.dto.AttendanceRecordOverrideResponse;
import com.aoms.aomsbackend.attendance.dto.AttendanceRecordRevertRequest;
import com.aoms.aomsbackend.attendance.entity.AttendanceRecord;
import com.aoms.aomsbackend.attendance.entity.AttendanceStatus;
import com.aoms.aomsbackend.attendance.service.AttendanceOverrideService;
import com.aoms.aomsbackend.auth.constant.SessionAttribute;
import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.auth.service.UserRoleAccessService;
import com.aoms.aomsbackend.common.exception.ConflictException;
import com.aoms.aomsbackend.common.exception.GlobalExceptionHandler;
import com.aoms.aomsbackend.common.exception.NotFoundException;
import com.aoms.aomsbackend.config.interceptor.LocationRoleInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AttendanceOverrideControllerTest {

    @Mock
    private AttendanceOverrideService service;

    @Mock
    private UserRoleAccessService userRoleAccessService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final UUID superAdminId = UUID.randomUUID();
    private final UUID recordId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        AttendanceOverrideController controller = new AttendanceOverrideController(service);
        LocationRoleInterceptor interceptor = new LocationRoleInterceptor(userRoleAccessService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(interceptor)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // --- Override ---

    @Test
    void override_asSuperAdmin_returns200() throws Exception {
        when(userRoleAccessService.hasAccess(eq(superAdminId), any(), eq(UserRoleType.SUPER_ADMIN))).thenReturn(true);
        when(service.override(eq(recordId), any(AttendanceRecordOverrideRequest.class), eq(superAdminId)))
                .thenReturn(sampleResponse(true));

        AttendanceRecordOverrideRequest request = new AttendanceRecordOverrideRequest();
        request.setStatus(AttendanceStatus.PRESENT);
        request.setOverrideReason("Correcting pipeline error");

        mockMvc.perform(patch("/api/v1/attendance-records/{id}/override", recordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), superAdminId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isOverridden").value(true));
    }

    @Test
    void override_nonSuperAdmin_returns403() throws Exception {
        UUID managerId = UUID.randomUUID();
        when(userRoleAccessService.hasAccess(eq(managerId), any(), eq(UserRoleType.SUPER_ADMIN))).thenReturn(false);

        AttendanceRecordOverrideRequest request = new AttendanceRecordOverrideRequest();
        request.setStatus(AttendanceStatus.PRESENT);
        request.setOverrideReason("reason");

        mockMvc.perform(patch("/api/v1/attendance-records/{id}/override", recordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), managerId.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void override_unauthenticated_returns403() throws Exception {
        AttendanceRecordOverrideRequest request = new AttendanceRecordOverrideRequest();
        request.setStatus(AttendanceStatus.PRESENT);
        request.setOverrideReason("reason");

        mockMvc.perform(patch("/api/v1/attendance-records/{id}/override", recordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void override_blankReason_returns400() throws Exception {
        when(userRoleAccessService.hasAccess(eq(superAdminId), any(), eq(UserRoleType.SUPER_ADMIN))).thenReturn(true);

        mockMvc.perform(patch("/api/v1/attendance-records/{id}/override", recordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PRESENT\",\"overrideReason\":\"   \"}")
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), superAdminId.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void override_missingStatus_returns400() throws Exception {
        when(userRoleAccessService.hasAccess(eq(superAdminId), any(), eq(UserRoleType.SUPER_ADMIN))).thenReturn(true);

        mockMvc.perform(patch("/api/v1/attendance-records/{id}/override", recordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"overrideReason\":\"reason\"}")
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), superAdminId.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void override_recordNotFound_returns404() throws Exception {
        when(userRoleAccessService.hasAccess(eq(superAdminId), any(), eq(UserRoleType.SUPER_ADMIN))).thenReturn(true);
        when(service.override(eq(recordId), any(), eq(superAdminId)))
                .thenThrow(new NotFoundException("Attendance record not found: " + recordId));

        AttendanceRecordOverrideRequest request = new AttendanceRecordOverrideRequest();
        request.setStatus(AttendanceStatus.PRESENT);
        request.setOverrideReason("reason");

        mockMvc.perform(patch("/api/v1/attendance-records/{id}/override", recordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), superAdminId.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void override_alreadyOverridden_returns409() throws Exception {
        when(userRoleAccessService.hasAccess(eq(superAdminId), any(), eq(UserRoleType.SUPER_ADMIN))).thenReturn(true);
        when(service.override(eq(recordId), any(), eq(superAdminId)))
                .thenThrow(new ConflictException("Record is already overridden. Revert before overriding again.",
                        "ALREADY_OVERRIDDEN"));

        AttendanceRecordOverrideRequest request = new AttendanceRecordOverrideRequest();
        request.setStatus(AttendanceStatus.PRESENT);
        request.setOverrideReason("reason");

        mockMvc.perform(patch("/api/v1/attendance-records/{id}/override", recordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), superAdminId.toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code.md").value("ALREADY_OVERRIDDEN"));
    }

    @Test
    void override_concurrentModification_returns409() throws Exception {
        when(userRoleAccessService.hasAccess(eq(superAdminId), any(), eq(UserRoleType.SUPER_ADMIN))).thenReturn(true);
        when(service.override(eq(recordId), any(), eq(superAdminId)))
                .thenThrow(new ObjectOptimisticLockingFailureException(AttendanceRecord.class, recordId));

        AttendanceRecordOverrideRequest request = new AttendanceRecordOverrideRequest();
        request.setStatus(AttendanceStatus.PRESENT);
        request.setOverrideReason("reason");

        mockMvc.perform(patch("/api/v1/attendance-records/{id}/override", recordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), superAdminId.toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code.md").value("CONCURRENT_MODIFICATION"));
    }

    // --- Revert ---

    @Test
    void revert_asSuperAdmin_returns200() throws Exception {
        when(userRoleAccessService.hasAccess(eq(superAdminId), any(), eq(UserRoleType.SUPER_ADMIN))).thenReturn(true);
        when(service.revert(eq(recordId), any(AttendanceRecordRevertRequest.class), eq(superAdminId)))
                .thenReturn(sampleResponse(false));

        AttendanceRecordRevertRequest request = new AttendanceRecordRevertRequest();
        request.setRevertReason("Override was incorrect");

        mockMvc.perform(patch("/api/v1/attendance-records/{id}/revert", recordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), superAdminId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isOverridden").value(false));
    }

    @Test
    void revert_notOverridden_returns409() throws Exception {
        when(userRoleAccessService.hasAccess(eq(superAdminId), any(), eq(UserRoleType.SUPER_ADMIN))).thenReturn(true);
        when(service.revert(eq(recordId), any(), eq(superAdminId)))
                .thenThrow(new ConflictException("Record is not overridden.", "NOT_OVERRIDDEN"));

        AttendanceRecordRevertRequest request = new AttendanceRecordRevertRequest();
        request.setRevertReason("reason");

        mockMvc.perform(patch("/api/v1/attendance-records/{id}/revert", recordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), superAdminId.toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code.md").value("NOT_OVERRIDDEN"));
    }

    @Test
    void revert_blankReason_returns400() throws Exception {
        when(userRoleAccessService.hasAccess(eq(superAdminId), any(), eq(UserRoleType.SUPER_ADMIN))).thenReturn(true);

        mockMvc.perform(patch("/api/v1/attendance-records/{id}/revert", recordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"revertReason\":\"\"}")
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), superAdminId.toString()))
                .andExpect(status().isBadRequest());
    }

    private AttendanceRecordOverrideResponse sampleResponse(boolean overridden) {
        return AttendanceRecordOverrideResponse.builder()
                .id(recordId)
                .userId(UUID.randomUUID())
                .buildingId(UUID.randomUUID())
                .recordDate(LocalDate.of(2025, 1, 10))
                .status(AttendanceStatus.PRESENT)
                .isOverridden(overridden)
                .overrideReason(overridden ? "Correcting pipeline error" : "Override was incorrect")
                .originalStatus(overridden ? AttendanceStatus.ABSENT : null)
                .revertReasons(overridden ? null : "2026-04-28T10:15:00 | actor=" + superAdminId + " | reason=Override was incorrect")
                .overriddenBy(overridden ? superAdminId : null)
                .overriddenAt(overridden ? OffsetDateTime.now() : null)
                .build();
    }
}
