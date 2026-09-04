package com.aoms.aomsbackend.attendance.controller;

import com.aoms.aomsbackend.attendance.dto.LocationConfigResponse;
import com.aoms.aomsbackend.attendance.entity.SeatVisibilityMode;
import com.aoms.aomsbackend.attendance.service.LocationConfigService;
import com.aoms.aomsbackend.auth.constant.SessionAttribute;
import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.auth.service.UserRoleAccessService;
import com.aoms.aomsbackend.common.exception.GlobalExceptionHandler;
import com.aoms.aomsbackend.config.interceptor.LocationRoleInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LocationConfigSeatVisibilityControllerTest {

    @Mock
    private LocationConfigService service;

    @Mock
    private UserRoleAccessService userRoleAccessService;

    private MockMvc mockMvc;

    private final UUID facilityAdminId = UUID.randomUUID();
    private final UUID locationId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        LocationConfigController controller = new LocationConfigController(service);
        LocationRoleInterceptor interceptor = new LocationRoleInterceptor(userRoleAccessService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(interceptor)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void updateSeatVisibility_asFacilitiesAdmin_returns200() throws Exception {
        when(userRoleAccessService.hasAccess(eq(facilityAdminId), eq(locationId), eq(UserRoleType.FACILITIES_ADMIN))).thenReturn(true);
        when(service.updateSeatVisibility(eq(locationId), any(UpdateSeatVisibilityRequest.class), any(UUID.class)))
                .thenReturn(sampleConfig(SeatVisibilityMode.TEAM_ONLY));

        mockMvc.perform(patch("/api/v1/locations/{locationId}/config/seat-visibility", locationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seatVisibilityMode\": \"TEAM_ONLY\"}")
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), facilityAdminId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Seat visibility mode updated"));
    }

    @Test
    void updateSeatVisibility_unauthenticated_returns403() throws Exception {
        mockMvc.perform(patch("/api/v1/locations/{locationId}/config/seat-visibility", locationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seatVisibilityMode\": \"FULL\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateSeatVisibility_wrongRole_returns403() throws Exception {
        UUID employeeId = UUID.randomUUID();
        when(userRoleAccessService.hasAccess(eq(employeeId), eq(locationId), eq(UserRoleType.FACILITIES_ADMIN))).thenReturn(false);

        mockMvc.perform(patch("/api/v1/locations/{locationId}/config/seat-visibility", locationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seatVisibilityMode\": \"FULL\"}")
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), employeeId.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateSeatVisibility_nullMode_returns400() throws Exception {
        when(userRoleAccessService.hasAccess(eq(facilityAdminId), eq(locationId), eq(UserRoleType.FACILITIES_ADMIN))).thenReturn(true);

        mockMvc.perform(patch("/api/v1/locations/{locationId}/config/seat-visibility", locationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), facilityAdminId.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateSeatVisibility_invalidModeValue_returns400() throws Exception {
        when(userRoleAccessService.hasAccess(eq(facilityAdminId), eq(locationId), eq(UserRoleType.FACILITIES_ADMIN))).thenReturn(true);

        mockMvc.perform(patch("/api/v1/locations/{locationId}/config/seat-visibility", locationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seatVisibilityMode\": \"NOT_A_VALID_MODE\"}")
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), facilityAdminId.toString()))
                .andExpect(status().isBadRequest());
    }

    private LocationConfigResponse sampleConfig(SeatVisibilityMode mode) {
        return LocationConfigResponse.builder()
                .id(UUID.randomUUID())
                .buildingId(locationId)
                .workStartTime(LocalTime.of(9, 0))
                .latenessThresholdMinutes(15)
                .minPresenceDurationMinutes(360)
                .hotDeskBookingWindowDays(7)
                .bookingCancellationCutoffHours(2)
                .seatVisibilityMode(mode)
                .sessionGapThresholdHours(4)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }
}
