package com.aoms.aomsbackend.attendance.controller;

import com.aoms.aomsbackend.attendance.dto.LocationConfigResponse;
import com.aoms.aomsbackend.attendance.dto.LocationConfigUpdateRequest;
import com.aoms.aomsbackend.attendance.entity.SeatVisibilityMode;
import com.aoms.aomsbackend.attendance.service.LocationConfigService;
import com.aoms.aomsbackend.auth.constant.SessionAttribute;
import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.auth.service.UserRoleAccessService;
import com.aoms.aomsbackend.common.exception.GlobalExceptionHandler;
import com.aoms.aomsbackend.config.interceptor.LocationRoleInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LocationConfigControllerTest {

    @Mock
    private LocationConfigService service;

    @Mock
    private UserRoleAccessService userRoleAccessService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private final UUID hrUserId = UUID.randomUUID();
    private final UUID managerId = UUID.randomUUID();
    private final UUID locationId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        LocationConfigController controller = new LocationConfigController(service);
        LocationRoleInterceptor interceptor = new LocationRoleInterceptor(userRoleAccessService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(interceptor)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getConfig_asManager_returns200() throws Exception {
        when(userRoleAccessService.hasAccess(managerId, locationId, UserRoleType.EMPLOYEE)).thenReturn(true);
        when(service.getByBuildingId(locationId)).thenReturn(sampleConfig());

        mockMvc.perform(get("/api/v1/locations/{locationId}/config", locationId)
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), managerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getConfig_asEmployee_returns200() throws Exception {
        UUID employeeId = UUID.randomUUID();
        when(userRoleAccessService.hasAccess(employeeId, locationId, UserRoleType.EMPLOYEE)).thenReturn(true);
        when(service.getByBuildingId(locationId)).thenReturn(sampleConfig());

        mockMvc.perform(get("/api/v1/locations/{locationId}/config", locationId)
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), employeeId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getConfig_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/locations/{locationId}/config", locationId))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateConfig_asHr_returns200() throws Exception {
        LocationConfigUpdateRequest request = new LocationConfigUpdateRequest();
        request.setMinPresenceDurationMinutes(360);

        when(userRoleAccessService.hasAccess(hrUserId, locationId, UserRoleType.HR)).thenReturn(true);
        when(service.updateByBuildingId(any(UUID.class), any(LocationConfigUpdateRequest.class))).thenReturn(sampleConfig());

        mockMvc.perform(patch("/api/v1/locations/{locationId}/config", locationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), hrUserId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.minPresenceDurationMinutes").value(360));
    }

    @Test
    void updateConfig_asManager_returns403() throws Exception {
        when(userRoleAccessService.hasAccess(managerId, locationId, UserRoleType.HR)).thenReturn(false);

        LocationConfigUpdateRequest request = new LocationConfigUpdateRequest();
        request.setMinPresenceDurationMinutes(240);

        mockMvc.perform(patch("/api/v1/locations/{locationId}/config", locationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), managerId.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateConfig_unauthenticated_returns403() throws Exception {
        LocationConfigUpdateRequest request = new LocationConfigUpdateRequest();
        request.setMinPresenceDurationMinutes(240);

        mockMvc.perform(patch("/api/v1/locations/{locationId}/config", locationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateConfig_zeroMinutes_returns400() throws Exception {
        when(userRoleAccessService.hasAccess(hrUserId, locationId, UserRoleType.HR)).thenReturn(true);

        LocationConfigUpdateRequest request = new LocationConfigUpdateRequest();
        request.setMinPresenceDurationMinutes(0);

        mockMvc.perform(patch("/api/v1/locations/{locationId}/config", locationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), hrUserId.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateConfig_negativeMinutes_returns400() throws Exception {
        when(userRoleAccessService.hasAccess(hrUserId, locationId, UserRoleType.HR)).thenReturn(true);

        LocationConfigUpdateRequest request = new LocationConfigUpdateRequest();
        request.setMinPresenceDurationMinutes(-30);

        mockMvc.perform(patch("/api/v1/locations/{locationId}/config", locationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), hrUserId.toString()))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @CsvSource({
            "{}, empty JSON request body",
            "{'latenessThresholdMinutes': 0}, zero lateness threshold",
            "{'sessionGapThresholdHours': -1}, negative session gap threshold"
    })
    void updateConfig_invalidFields_returns400(String jsonContent) throws Exception {
        when(userRoleAccessService.hasAccess(hrUserId, locationId, UserRoleType.HR)).thenReturn(true);

        mockMvc.perform(patch("/api/v1/locations/{locationId}/config", locationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent)
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), hrUserId.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateConfig_workStartTime_returns200() throws Exception {
        when(userRoleAccessService.hasAccess(hrUserId, locationId, UserRoleType.HR)).thenReturn(true);
        when(service.updateByBuildingId(any(UUID.class), any(LocationConfigUpdateRequest.class))).thenReturn(sampleConfig());

        mockMvc.perform(patch("/api/v1/locations/{locationId}/config", locationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workStartTime\": \"08:30:00\"}")
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), hrUserId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private LocationConfigResponse sampleConfig() {
        return LocationConfigResponse.builder()
                .id(UUID.randomUUID())
                .buildingId(locationId)
                .workStartTime(LocalTime.of(9, 0))
                .latenessThresholdMinutes(15)
                .minPresenceDurationMinutes(360)
                .hotDeskBookingWindowDays(7)
                .bookingCancellationCutoffHours(2)
                .seatVisibilityMode(SeatVisibilityMode.FULL)
                .sessionGapThresholdHours(4)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }
}
